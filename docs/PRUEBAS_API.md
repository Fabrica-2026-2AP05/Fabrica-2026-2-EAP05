# Pruebas Automáticas de API — HU01, HU03, HU04 y HU05

Suite automatizada con **JUnit 5 + RestAssured** para la ejecución de los 20 casos de prueba (CP) funcionales y de seguridad de la API del Caso 14 (Plataforma de Reservas de Servicios - Fábrica Escuela EAP05).

---

## 1. Alcance y cobertura

La suite cubre 20 casos de prueba especificados en el libro `Casos-prueba-EAP05.xlsx` v1.3 (hoja *Mod1 - Autenticacion-Registro*):

- **HU01 — Iniciar sesión:**
  - `CP-01-A`: Inicio exitoso (200, JWT válido de 3 partes, rol cliente, acceso a sonda `/api/reservas/reservar`).
  - `CP-01-B`: Credenciales incorrectas (401, JSON con timestamp y mensaje exacto).
  - `CP-01-C`: Bloqueo por intentos fallidos (5 intentos fallidos y 6.º intento responde 429).
  - `CP-01-D`: Campos ausentes (400 para omisión de contraseña y de email con sus mensajes respectivos).
  - `CP-01-E`: Correo no registrado (401 idéntico a clave errada para evitar enumeración).
  - `CP-01-F`: Usuario inactivo (401 con mensaje de usuario inactivo).
- **HU03 — Cerrar sesión:**
  - `CP-03-A`: Cierre exitoso (200 y sonda posterior responde 401).
  - `CP-03-B`: Token inválido o expirado (401 para token malformado, sin header y sesión cerrada).
  - `CP-03-C`: Token revocado (401 al intentar registrar proveedor con token cerrado y cuenta no creada).
- **HU04 — Registrar cliente:**
  - `CP-04-A`: Registro exitoso (201, UUID, campos devueltos, sin contraseña en respuesta, login 200).
  - `CP-04-B`: Información incompleta (6 variantes con 400 y mensajes de validación específicos; no queda registrado).
  - `CP-04-C`: Correo existente (409 ante correo duplicado).
  - `CP-04-D`: Longitud mínima de contraseña (400 para 5 caracteres, 201 para 6 caracteres).
  - `CP-04-E`: Rol enviado en el cuerpo (201 ignorando rol enviado y asignando 'cliente').
- **HU05 — Registrar proveedor (con token administrador):**
  - `CP-05-A`: Registro exitoso con Bearer admin (201 con rol proveedor y login 200).
  - `CP-05-B`: Información incompleta (5 variantes con 400 y mensajes específicos; no queda registrado).
  - `CP-05-C`: Proveedor duplicado (409 ante proveedor ya existente).
  - `CP-05-D`: Sin token de autorización (401 sin credenciales).
  - `CP-05-E`: Rol no autorizado (403 con token de cliente o token de otro proveedor).
  - `CP-05-F`: Contraseña < 6 caracteres (400 para 5 caracteres, 201 para 6 caracteres).

---

## 2. Configuración del entorno

Las pruebas pueden configurarse mediante variables de entorno o en el archivo `src/test/resources/api-tests.properties` (ignorado por Git). Se dispone de la plantilla `src/test/resources/api-tests.properties.example`.

> **ADVERTENCIA DE SEGURIDAD:**
> Nunca almacene contraseñas reales ni tokens en archivos versionados ni en commits de Git.

### Variables admitidas

| Variable | Obligatoria | Descripción / Ejemplo |
|---|---|---|
| `QA_BASE_URL` | Sí | URL base de la API (`https://fabrica-2026-2-eap05-1.onrender.com` o `http://localhost:8080`) |
| `QA_ADMIN_EMAIL` | Sí (para HU05 y CP-03-C) | Correo de la cuenta administradora |
| `QA_ADMIN_PASSWORD` | Sí (para HU05 y CP-03-C) | Contraseña de la cuenta administradora |
| `QA_EJECUTOR` | Sí | Nombre del responsable de la ejecución (`Garzón Mejía Juan José`) |
| `QA_RAMA_DESPLEGADA` | Sí | Rama o commit probado (`calidad@9d944da`, `Sprint-1@d92fe72`, o `desconocida`) |
| `QA_TIMEOUT_MS` | No | Timeout de conexión y lectura en ms (por defecto `90000` para Render gratuito) |
| `QA_DB_URL` / `QA_DB_USER` / `QA_DB_PASSWORD` | No | Conexión JDBC a base de datos para verificaciones directas en BD |
| `QA_INACTIVE_EMAIL` / `QA_INACTIVE_PASSWORD` | No | Usuario inactivo preexistente como alternativa a BD para CP-01-F |

---

## 3. Comandos de ejecución

Por diseño de integración continua (CI), las pruebas de API están aisladas bajo la etiqueta `@Tag("api")` y excluidas del ciclo de compilación habitual (`./mvnw clean verify`). Para ejecutarlas se requiere activar explícitamente el perfil `api-tests`.

### Windows PowerShell

```powershell
# Configurar variables de sesión
$env:QA_BASE_URL="https://fabrica-2026-2-eap05-1.onrender.com"
$env:QA_ADMIN_EMAIL="<correo-admin>"
$env:QA_ADMIN_PASSWORD="<clave-admin>"
$env:QA_EJECUTOR="Garzón Mejía Juan José"
$env:QA_RAMA_DESPLEGADA="desconocida"

# Ejecutar todos los 20 CP
.\mvnw.cmd -Papi-tests test

# Ejecutar una sola clase
.\mvnw.cmd -Papi-tests test -Dtest=HU01LoginApiTest

# Ejecutar un único caso de prueba
.\mvnw.cmd -Papi-tests test -Dtest=HU01LoginApiTest#cp01B_credencialesIncorrectas_responde401
```

### Linux / macOS / Git Bash

```bash
export QA_BASE_URL=https://fabrica-2026-2-eap05-1.onrender.com
export QA_ADMIN_EMAIL="<correo-admin>"
export QA_ADMIN_PASSWORD="<clave-admin>"
export QA_EJECUTOR="Garzón Mejía Juan José"
export QA_RAMA_DESPLEGADA="desconocida"

./mvnw -Papi-tests test
```

---

## 4. Estructura de salidas y reportes

Al culminar la ejecución, todos los artefactos se generan en el directorio `target/cp-results/`:

1. **Consola:**
   - Tabla comparativa con columnas: `CP | HU | Estado | HTTP obtenido | Esperado | Defecto sugerido`.
   - Conteo consolidado: `PASA x / NO PASA y / BLOQUEADO z`.
   - Información de la rama y URL probadas.
2. `resultados-<runId>.json`:
   - Registro completo estructurado con metadatos del run, cada caso, peticiones HTTP, cabeceras enmascaradas, códigos de respuesta, tiempos en milisegundos y fragmentos de cuerpo.
3. `resultados-<runId>.csv`:
   - Resumen tabular plano: `cp,hu,estado,http,esperado,fallas,defecto`.
4. `excel-fill-<runId>.csv`:
   - Archivo en UTF-8 con BOM preparado con las coordenadas exactas de celdas (`C(r+34)`, `C(r+35)`, `C(r+36)`, `B(r+42)`) para importar o pegar directamente en la hoja *Mod1* del Excel de pruebas.
5. `bugs-sugeridos-<runId>.md`:
   - Fichas de defectos en formato Azure DevOps para cada caso `NO PASA`, incluyendo pasos reproducibles, severidad recomendada según SQAP y componentes sospechosos. Incluye además el reporte de incidentes del repositorio.

---

## 5. Limpieza de datos de prueba

Cada ejecución genera identificadores de correo únicos bajo el patrón:
`qa.auto.<runId>.<cp>.<n>@example.com`

El equipo de Base de Datos puede depurar estos registros en el ambiente de pruebas ejecutando:

```sql
DELETE FROM public.usuario WHERE email LIKE 'qa.auto.%';
```

---

## 6. Estado de ramas y consideraciones técnicas (al 21/10/2026)

- **Rama base de la suite:** `origin/calidad` (commit `9d944da`).
- **Incidente en `Sprint-1`:** En el commit `263a0aa` (merge de `calidad` en `Sprint-1`), el archivo `GlobalExceptionHandler.java` contiene líneas de código huérfanas fuera de método (líneas 117-120), lo cual impide su compilación. Por política de calidad, esta suite **no modifica `src/main`** y reporta dicho hallazgo en el informe de bugs sugeridos.
- **Diferencias funcionales entre ramas:**
  - En `calidad`: "Usuario no encontrado" lanza `IllegalArgumentException` (que traduce a HTTP 400 en lugar de 401).
  - En `Sprint-1` sano (`d92fe72`): Cuenta con `CredencialesInvalidasException` traducido a HTTP 401.
  - La suite mantiene estrictamente el oráculo formal definido en el SPEC; los comportamientos discordantes de la API son registrados como fallos objetivos.

---

## 7. Dudas sobre el oráculo

- En `CP-04-C` y `CP-05-C`, el oráculo contempla tanto `El email ya está registrado` como la variante `El email ya está registrado en Supabase Auth`. La suite acepta ambas alternativas e informa cuál fue emitida por el backend.
- En `CP-04-E`, el registro responde 201 forzando el rol a `cliente` a pesar del valor enviado en el cuerpo JSON, garantizando la seguridad del control de acceso.
