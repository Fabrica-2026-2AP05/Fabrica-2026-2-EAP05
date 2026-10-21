# Verificación de las pruebas automáticas de API (HU01, HU03, HU04, HU05)

| | |
|---|---|
| **Rama verificada** | `qa/pruebas-api-hu01-hu05` (parte de `origin/calidad` `9d944da`) |
| **Insumos** | `SPEC_Gemini_Pruebas_API_HU01_HU03_HU04_HU05.md` 1.1 y `VERIFICACION_ClaudeCode_Pruebas_API.md` 1.1 |
| **Verificó** | Claude Code, 21/10/2026 |

## 1. Veredicto

**APROBADA CON CORRECCIONES.**

La suite de Gemini prueba los 20 CP tal como los define el SPEC: los códigos y mensajes esperados son los del oráculo, no hay aserciones debilitadas y están todas las variantes. Las correcciones fueron de evidencia y reportes, no de oráculo:

| Commit | Corrección |
|---|---|
| `33e299d` | Suite de Gemini tal como la entregó (sin commits previos en la rama; se excluyó `.vscode/settings.json`). |
| `8598afb` | `test(api): decodificar cuerpos según el charset de la respuesta`. Además, el calentamiento y el login admin usan `QA_TIMEOUT_MS`, y se quitó un `catch` vacío. |
| `e3b15c8` | `test(api): enmascarar JWT sueltos y registrar el HTTP del paso principal`. Las fallas de CP-01-C publicaban el JWT completo. Los mensajes de `errores.contrasena` estaban ocultos como `***`. El teléfono de prueba anterior contenía como subcadena la clave admin y el enmascarado lo dejaba parcialmente oculto en la evidencia. "HTTP obtenido" mostraba el último paso (p. ej., el 201 de la verificación en CP-03-C). |
| `2036fd1` | `test(api): generar reportes al cerrar JUnit y corregir bugs sugeridos`. El *shutdown hook* truncaba la tabla de consola; ahora se genera una vez al cerrar el store raíz de JUnit. Orden por CP. Títulos de bug según §10. Fragmento real de `GlobalExceptionHandler` en el hallazgo de `Sprint-1`. |
| `610cdd0` | `test(api): restaurar BOM del pom.xml` (el diff del `pom` queda solo en los cambios de pruebas). |
| `6314fec` | `test(api): precisar causa probable del bug de CP-01-F`. |

### Comprobaciones de alcance y CI

- `git diff origin/calidad...HEAD -- src/main`: vacío. No se tocaron pruebas existentes.
- `pom.xml`: dependencia `rest-assured` 5.5.0 (`test`), `excludedGroups=api` y perfil `api-tests`. El perfil también fija `groovy.version=4.0.22`: Spring Boot 4.1.1 gestiona Groovy 5.0.8 y ya no gestiona rest-assured, que necesita Groovy 4. Es un cambio justificado y solo aplica con el perfil.
- `.gitignore`: agrega `api-tests.properties` y quita el BOM. Con el BOM, la primera regla era `﻿.env` y no ignoraba `.env`.
- Sin secretos en código, docs ni historial de la rama. `api-tests.properties` está ignorado. Las credenciales se pasaron solo como variables de entorno de la sesión.
- `./mvnw clean verify` sin perfil: **137 pruebas, 0 fallos**. Ninguna clase `api` en `target/surefire-reports`. JaCoCo: 912/1173 líneas+ramas = **77,7 %** (igual a la referencia).
- Pruebas de humo:
  1. API caída (`http://localhost:9`): 20 `NO EJECUTADO - BLOQUEADO` por "API no disponible", ninguno `PASA`, los 4 archivos se generan.
  2. Sin `QA_ADMIN_*`: CP-05-A/B/C/E/F y CP-03-C bloqueados; CP-05-D se ejecuta (PASA).
  3. `-Dtest=HU01LoginApiTest#cp01B*`: solo CP-01-B en consola y archivos.

## 2. Contexto de la ejecución

| Dato | Valor |
|---|---|
| runId | `20261021150106` |
| Fecha | 21/10/2026 15:02 |
| Base URL | `https://fabrica-2026-2-eap05-1.onrender.com` |
| `/v3/api-docs` | Gestor de Reservas API v1.0.0 |
| Rama probada | **`calidad` o `Sprint-2` (probable)**: CP-01-F responde 500, que según §10 del SPEC corresponde a esas ramas; `Sprint-1@d92fe72` respondería 401. Es una inferencia, no un hecho. |
| Ejecutor | Garzón Mejía Juan José [Calidad] |
| Herramienta | JUnit 5 + RestAssured, `./mvnw -Papi-tests test` |
| Repeticiones | Ninguna: no hubo timeouts de arranque en frío. |

## 3. Resultados de los 20 CP

| CP | HU | Estado | HTTP obtenido | Esperado | Defecto |
|---|---|---|---|---|---|
| CP-01-A | HU01 | PASA | 200 / 400 | 200 con token; sonda ≠ 401/403 | No aplica |
| CP-01-B | HU01 | NO PASA | 400 | 401 `Credenciales inválidas` | Bug 1 |
| CP-01-C | HU01 | NO PASA | 200 | 6.º intento → 429 | Bug 2 |
| CP-01-D | HU01 | PASA | 400 / 400 | 400 / 400 | No aplica |
| CP-01-E | HU01 | NO PASA | 400 | 401, igual a CP-01-B | Bug 1 |
| CP-01-F | HU01 | NO PASA | 500 | 401 `El usuario se encuentra inactivo` | Bug 3 |
| CP-03-A | HU03 | PASA | 200 / 400 / 401 | logout 200; sonda antes ≠ 401; sonda después 401 | No aplica |
| CP-03-B | HU03 | NO PASA | 401 / 401 / 401 | 401 / 401 / 401 con texto exacto | Bug 4 |
| CP-03-C | HU03 | NO PASA | 401 | 401 `Sesión expirada…`; proveedor no registrado | Bug 4 |
| CP-04-A | HU04 | PASA | 201 / 200 | 201 / 200 | No aplica |
| CP-04-B | HU04 | PASA | 400 ×6 | 400 ×6 | No aplica |
| CP-04-C | HU04 | PASA | 409 | 409 | No aplica |
| CP-04-D | HU04 | PASA | 400 / 201 | 400 / 201 | No aplica |
| CP-04-E | HU04 | PASA | 201 / 200 / 403 | 201 / 200 / 403 | No aplica |
| CP-05-A | HU05 | PASA | 201 / 200 | 201 / 200 | No aplica |
| CP-05-B | HU05 | PASA | 400 ×5 | 400 ×5 | No aplica |
| CP-05-C | HU05 | PASA | 409 | 409 | No aplica |
| CP-05-D | HU05 | PASA | 401 | 401 | No aplica |
| CP-05-E | HU05 | PASA | 403 / 403 | 403 / 403 | No aplica |
| CP-05-F | HU05 | PASA | 400 / 201 | 400 / 201 | No aplica |

**Totales: PASA 14 / NO PASA 6 / BLOQUEADO 0.**

Frente a la corrida de Gemini (`20261021114814`: 14 / 5 / 1), CP-01-F pasa de BLOQUEADO a NO PASA porque ya hay cuenta inactiva (`QA_INACTIVE_*`).

En CP-01-A y CP-03-A el 400 es la sonda `POST /api/reservas/reservar {}` con sesión válida: falla la validación del body, que es lo esperado ("concede acceso de su rol").

## 4. Fidelidad al oráculo (Paso 3)

✓ en todas las columnas para los 20 CP: un único `@Test` con `@CasoPrueba` correcto, código y mensaje exactos del SPEC, variantes completas, correos `qa.auto.<runId>.<cp>.<n>@example.com` y sin dependencias entre CP.

| CP | Efecto verificado | Observación |
|---|---|---|
| CP-01-A | sonda con el token | — |
| CP-01-B | sin `token` | — |
| CP-01-C | 6 intentos en la evidencia | Cuenta nueva propia; nunca usa la cuenta admin. |
| CP-01-D | sin `token` | Usa los mensajes de `LoginDTO` (`El correo es obligatorio`). |
| CP-01-E | mismo código y `mensaje` que la clave errada | — |
| CP-01-F | sin `token` | Bloqueo solo si no hay BD ni `QA_INACTIVE_*`. |
| CP-03-A | sonda antes (≠401) y después (401) | — |
| CP-03-B | ninguna 5xx | 3 variantes. |
| CP-03-C | §5.3 con token admin nuevo | — |
| CP-04-A | login 200 `cliente`; sin `contrasena`/`password` | — |
| CP-04-B | §5.3 en variantes 1, 3, 4 y 5 | Omitida en "sin email" y "email inválido", como permite el SPEC. Usa los mensajes de `UsuarioRegistroRequest` (`El email…`). |
| CP-04-C | — | Acepta también `… en Supabase Auth`; se obtuvo `El email ya está registrado`. |
| CP-04-D | §5.3 en (a) | 2 límites. |
| CP-04-E | 403 al registrar proveedor con ese token | — |
| CP-05-A | login 200 `proveedor` | — |
| CP-05-B | §5.3 en variantes 1, 3, 4 y 5 | — |
| CP-05-C | — | Igual que CP-04-C. |
| CP-05-D | §5.3 con token admin | Si no hay admin, la verificación se omite y el CP se evalúa solo por el 401. |
| CP-05-E | §5.3 en (a) y (b) | Correos `cp05e_cli`, `cp05e_ta`… (sufijo extra, siguen siendo `qa.auto.%`). |
| CP-05-F | §5.3 en (a) | 2 límites. |

Calidad del código (Paso 4): sin `@Disabled`, sin `catch` vacíos (tras `8598afb`), `Thread.sleep` solo en el calentamiento (3 s), cuerpos JSON como `String`, `DbVerifier` solo hace `SELECT` y el `UPDATE … activo=false` restringido a `qa.auto.%`, `excel-fill` en UTF-8 con BOM.

## 5. Bugs a registrar en Azure DevOps

Las HU01 y HU03 están `Closed`: los bugs se registran contra su HU. El cambio de estado lo decide el PO (Juan José Garzón). BUG06 (#119) y BUG07 (#120) no se reabren: CP-04-A y CP-05-D pasan.

> Nota: los archivos de la corrida de Gemini `20261021114814` no asociaban BUG06/BUG07 a CP-01-B/CP-01-E (ya compartían el título del 400). Sí tenían títulos genéricos para CP-03-B/CP-03-C ("responde HTTP 401/201 en lugar del esperado") y otro título para CP-01-C; quedaron corregidos.

### Bug 1 — `[HU01] Login con credenciales inválidas responde 400 en lugar de 401`
- **Severidad:** Media. **CP:** CP-01-B, CP-01-E.
- **Repro:** `POST /api/auth/login` con un cliente existente y `contrasena`=`Incorrecta99`, o con `noexiste.<runId>@example.com` / `MiPassword123`.
- **Actual:** 400 `{"mensaje":"Credenciales inválidas","timestamp":…}`. **Esperado:** 401 con el mismo cuerpo.
- **Causa probable (a confirmar por Backend):** `AuthService.validarCredencialesSupabase` lanza `IllegalArgumentException`, que `GlobalExceptionHandler` traduce a 400. La respuesta no revela si la cuenta existe; falla solo por el código.

### Bug 2 — `[HU01] El bloqueo tras 5 intentos fallidos no se activa`
- **Severidad:** Alta (control de seguridad). **CP:** CP-01-C.
- **Repro:** cliente nuevo; 5 logins con clave incorrecta (los 5 → 400) y un 6.º con la correcta.
- **Actual:** 6.º intento 200 con `token`. **Esperado:** 429 `Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intenta de nuevo en … minuto(s)`, sin token.
- **Causa probable (a confirmar por Backend):** `AuthService.login` solo cuenta el fallo al capturar `CredencialesInvalidasException`, pero llega `IllegalArgumentException` (misma raíz que el Bug 1).

### Bug 3 — `[HU01] Login de usuario inactivo responde 500 en lugar de 401`
- **Severidad:** Media. **CP:** CP-01-F.
- **Repro:** `POST /api/auth/login` con `qa.auto.manual.cp01f.20261021@example.com` (marcada `activo=false`) y su clave correcta.
- **Actual:** 500 `{"timestamp":…,"status":500,"error":"Internal Server Error","path":"/api/auth/login"}`. **Esperado:** 401 `{"mensaje":"El usuario se encuentra inactivo"}`, sin token.
- **Causa probable (a confirmar por Backend):** las ramas `calidad` y `Sprint-2` no tienen `@ExceptionHandler(CredencialesInvalidasException)`.
- Confirmado también por el usuario con una prueba manual (mismo 500).

### Bug 4 — `[HU03] Las respuestas 401 de JwtFilter no declaran charset (las tildes se ven mal en clientes UTF-8)`
- **Severidad:** Baja (cosmético: el código 401 y el efecto son correctos). **CP:** CP-03-B (variante c) y CP-03-C.
- **Repro:** login de un cliente, logout (200) y, con el mismo token, `POST /api/auth/logout` o `POST /api/usuario/registrar/proveedor`.
- **Actual:** 401 con el texto `Sesi�n expirada. Por favor, inicie sesi�n nuevamente`. **Esperado:** `Sesión expirada. Por favor, inicie sesión nuevamente`.
- **Evidencia (`curl -i` contra Render, 21/10/2026):**
  - Header recibido: `Content-Type: text/plain; charset=utf-8`.
  - Cuerpo: `53 65 73 69 F3 6E …` → `ó` = `0xF3`, que es ISO-8859-1, no UTF-8 (`C3 B3`).
  - Comparación: el logout 200 llega con `text/plain;charset=UTF-8` y `ó` = `C3 B3`, correcto.
  - Tomcat 11.0.24 aislado, con el mismo código que `JwtFilter` (`setStatus(401)` + `getWriter().write(…)`): responde `0xF3` **sin** `Content-Type`.
- **Conclusión:** `JwtFilter.java` (líneas 53–54 y 74–75) escribe sin `setContentType`/charset, así que Tomcat usa ISO-8859-1. El borde de Render/Cloudflare (probable, por el formato en minúsculas del header) completa `charset=utf-8`. Cualquier cliente que respete el header muestra `�`. La suite decodifica según el charset de la respuesta (ISO-8859-1 si no viene); como aquí viene `utf-8`, el texto recibido no coincide y el CP se mantiene **NO PASA**. Probado en local, sin Render, el texto se leería bien.
- **Sugerencia (a confirmar por Backend):** `response.setContentType("text/plain;charset=UTF-8")` antes de `getWriter()`. El mismo patrón aplica a `Sesión expirada por inactividad`.

### Hallazgo del repositorio (no es un CP) — `[Build] La rama Sprint-1 no compila tras el merge 263a0aa (GlobalExceptionHandler)`
- **Severidad:** Alta (impide desplegar `Sprint-1` y que pase su CI).
- **Detalle:** en `origin/Sprint-1` (`263a0aa`, todavía la cabeza el 21/10), después de `handleUsuarioInactivo` quedan fuera de cualquier método:
  ```java
  body.put("mensaje", "Error interno del servidor");
  return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  ```
- **Responsable sugerido:** Backend (autor del merge del 20/10). No se corrige en esta rama.

## 6. Pendientes

- **Limpieza de datos (Bases de Datos):** borrar los usuarios de prueba con `email like 'qa.auto.%'` en `public.usuario` y en `auth.users`, incluida la cuenta manual `qa.auto.manual.cp01f.20261021@example.com` y la de la verificación de charset `qa.auto.20261021120412.charset.1@example.com`.
- **Dudas sobre el oráculo:**
  - CP-03-B(c) y CP-03-C: si el equipo considera que la codificación no forma parte del CP, los dos CP pasarían por código y efecto. Se dejan en NO PASA porque el SPEC exige el texto exacto.
  - CP-05-D sin credenciales de admin no puede verificar "no queda registrado"; con credenciales sí lo verifica (esta corrida).
- **PR hacia `calidad`:** pendiente de autorización. El PR no hace que el CI ejecute las pruebas `api`. Lo integra Juan Ignacio Parra.

## 7. Archivos generados

En `target/cp-results/` (no versionados):

- `excel-fill-20261021150106.csv`: para la hoja Mod1 (celdas `C(r+34)`, `C(r+35)`, `C(r+36)`, `B(r+42)`).
- `resultados-20261021150106.json`
- `resultados-20261021150106.csv`
- `bugs-sugeridos-20261021150106.md`
