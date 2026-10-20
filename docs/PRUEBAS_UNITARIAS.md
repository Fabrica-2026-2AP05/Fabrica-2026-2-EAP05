# Plan de pruebas unitarias

Fuente: hoja **Pruebas unitarias** del archivo `Casos de Prueba - Plataforma de Reservas de Servicios.xlsx`.

## Alcance

- Sprint 1 comprometido: HU01, HU03, HU04 y HU05.
- Framework: JUnit 5 y Mockito.
- Estructura: Arrange, Act y Assert dentro de cada prueba.
- Las pruebas unitarias no usan red ni base de datos real.
- Ejecución: `./mvnw.cmd clean verify`.
- Reporte JaCoCo: `target/site/jacoco/index.html`.

## HU01 - Iniciar sesión

| ID | Escenario |
|---|---|
| PU-01-01 | Credenciales válidas de usuario activo |
| PU-01-02 | Supabase rechaza la contraseña |
| PU-01-03 | Cuenta de Supabase sin perfil local |
| PU-01-04 | Usuario inactivo |
| PU-01-05 | Campos vacíos o correo inválido |
| PU-01-06 | JWT conserva correo y rol |
| PU-01-07 | JWT usa cliente cuando el rol es nulo |
| PU-01-08 | Token inválido o alterado |
Los escenarios HTTP de inicio de sesión se documentan como pruebas funcionales del módulo 1 y se ejecutan desde Swagger UI.

## HU03 - Cerrar sesión

| ID | Escenario |
|---|---|
| PU-03-01 | Logout con token revoca la sesión |
| PU-03-02 | Logout sin token no agrega a lista negra |
| PU-03-03 | Token agregado queda revocado |
| PU-03-04 | Usuario sin actividad no tiene sesión |
| PU-03-05 | Cerrar sesión elimina la actividad |
| PU-03-06 | Sesión superior a 30 minutos expira |
| PU-03-09 | Solicitud sin token continúa sin autenticar |
| PU-03-10 | Token revocado responde 401 |
| PU-03-11 | Token válido carga el rol en el contexto |
| PU-03-12 | Inactividad responde 401 |
| PU-03-13 | Usuario inexistente o inactivo no se autentica |
| PU-03-14 | Firma inválida no se autentica |
| PU-03-15 | Limpiar token lo retira de la lista negra |

## HU04 - Registrar cliente

| ID | Escenario |
|---|---|
| PU-04-01 | Registro válido crea cliente y envía correo |
| PU-04-02 | Email duplicado produce conflicto |
| PU-04-03 | Fallo local elimina el usuario externo |
| PU-04-04 | Fallo de correo no revierte el registro |
| PU-04-05 | Validación de campos obligatorios |
| PU-04-06 | Creación exitosa en Supabase |
| PU-04-07 | Supabase rechaza un email existente |
| PU-04-08 | Fallo de limpieza externa no oculta el error original |
| PU-04-09 | Excepciones se traducen a 401, 400 y 409 |
| PU-04-12 | Error externo no relacionado con duplicidad |
| PU-04-13 | Eliminación exitosa en Supabase |
| PU-04-14 | Generación exitosa de token recovery |
| PU-04-15 | Respuesta de recovery inválida |
| PU-04-16 | Correo de establecimiento de contraseña |

## HU05 - Registrar proveedor

| ID | Escenario |
|---|---|
| PU-05-01 | Registro válido fuerza rol proveedor |
Los códigos HTTP y el control de acceso del registro de proveedor se validan como pruebas funcionales del módulo 1 mediante Swagger UI.

## Evidencia esperada

Al terminar, se registran la cantidad ejecutada, aprobada y fallida, la cobertura de instrucciones, líneas y ramas, y la ruta del reporte JaCoCo.

## Resultado de ejecución

- Fecha: 2026-09-28.
- Comando: `./mvnw.cmd clean verify`.
- Pruebas JUnit ejecutadas: 56.
- Aprobadas: 56.
- Fallidas: 0.
- Errores: 0.
- Omitidas: 0.
- Cobertura de instrucciones: 72,91 %.
- Cobertura de líneas: 78,14 %.
- Cobertura de ramas: 69,23 %.
- Resultado del Quality Gate de 65 % por líneas: aprobado.

El documento contiene únicamente pruebas relacionadas con las historias comprometidas HU01, HU03, HU04 y HU05. Las pruebas unitarias usan dependencias simuladas. Las pruebas funcionales de API se ejecutan manualmente desde Swagger UI.
