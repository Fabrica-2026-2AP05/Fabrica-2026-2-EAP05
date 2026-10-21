package Fabrica_EAP05.Reservas.api;

import org.junit.jupiter.api.extension.*;
import org.opentest4j.TestAbortedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class ResultadosExtension implements BeforeEachCallback, TestWatcher {

    // TreeMap: los reportes salen ordenados por CP (CP-01-A ... CP-05-F), no por orden de ejecución
    private static final Map<String, ResultadoCP> resultados = new TreeMap<>();
    private static final Map<String, Integer> FILAS_EXCEL = new HashMap<>();

    static {
        // HU01
        FILAS_EXCEL.put("CP-01-A", 2);
        FILAS_EXCEL.put("CP-01-B", 45);
        FILAS_EXCEL.put("CP-01-C", 88);
        FILAS_EXCEL.put("CP-01-D", 131);
        FILAS_EXCEL.put("CP-01-E", 174);
        FILAS_EXCEL.put("CP-01-F", 217);

        // HU03
        FILAS_EXCEL.put("CP-03-A", 389);
        FILAS_EXCEL.put("CP-03-B", 432);
        FILAS_EXCEL.put("CP-03-C", 475);

        // HU04
        FILAS_EXCEL.put("CP-04-A", 518);
        FILAS_EXCEL.put("CP-04-B", 561);
        FILAS_EXCEL.put("CP-04-C", 604);
        FILAS_EXCEL.put("CP-04-D", 647);
        FILAS_EXCEL.put("CP-04-E", 690);

        // HU05
        FILAS_EXCEL.put("CP-05-A", 733);
        FILAS_EXCEL.put("CP-05-B", 776);
        FILAS_EXCEL.put("CP-05-C", 819);
        FILAS_EXCEL.put("CP-05-D", 862);
        FILAS_EXCEL.put("CP-05-E", 905);
        FILAS_EXCEL.put("CP-05-F", 948);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        // JUnit cierra los AutoCloseable del store raíz una sola vez, al terminar todas las clases y antes de que
        // Surefire cierre el JVM (un shutdown hook dejaba la consola truncada y los archivos en riesgo).
        context.getRoot().getStore(ExtensionContext.Namespace.create(ResultadosExtension.class))
                .computeIfAbsent("reportes", k -> (AutoCloseable) ResultadosExtension::generarReportes, AutoCloseable.class);
        Method method = context.getRequiredTestMethod();
        CasoPrueba cpAnno = method.getAnnotation(CasoPrueba.class);
        if (cpAnno != null) {
            ResultadoCP cp = new ResultadoCP(cpAnno.id(), cpAnno.hu(), cpAnno.titulo());
            synchronized (resultados) {
                resultados.put(cpAnno.id(), cp);
            }
            EvidenciaRecorder.setActiveCP(cp);
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEstado("PASA");
            completarInformacion(cp, null);
        }
        EvidenciaRecorder.clear();
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEstado("NO PASA");
            if (cause != null) {
                cp.addFalla(EvidenciaRecorder.enmascarar(cause.getMessage() != null ? cause.getMessage() : cause.toString()));
                if (!(cause instanceof AssertionError)) {
                    cp.setErrorDePrueba(true);
                }
            }
            completarInformacion(cp, cause);
        }
        EvidenciaRecorder.clear();
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEstado("NO EJECUTADO - BLOQUEADO");
            String msg = (cause instanceof TestAbortedException && cause.getMessage() != null)
                    ? cause.getMessage().replace("Assumption failed: ", "")
                    : (cause != null ? cause.getMessage() : "Prerrequisito no cumplido");
            cp.setCausaBloqueo(msg);
            completarInformacion(cp, cause);
        }
        EvidenciaRecorder.clear();
    }

    private static void completarInformacion(ResultadoCP cp, Throwable cause) {
        String fecha = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        int filaInicio = FILAS_EXCEL.getOrDefault(cp.getCp(), 2);

        // Armar valor_resultado
        StringBuilder resumenEvidencia = new StringBuilder();
        resumenEvidencia.append("Automatizada (JUnit 5 + RestAssured), ")
                .append(fecha).append(", ")
                .append(ApiTestConfig.getBaseUrl()).append(" (")
                .append(ApiTestConfig.getRamaDesplegada()).append("), run ")
                .append(ApiTestConfig.getRunId()).append(": ");

        if (cp.getEvidencia().isEmpty()) {
            if ("NO EJECUTADO - BLOQUEADO".equals(cp.getEstado())) {
                resumenEvidencia.append("Bloqueado: ").append(cp.getCausaBloqueo());
            } else {
                resumenEvidencia.append("Sin peticiones ejecutadas");
            }
        } else {
            boolean first = true;
            for (ResultadoCP.PasoEvidencia p : cp.getEvidencia()) {
                if (!first) resumenEvidencia.append(" · ");
                resumenEvidencia.append(p.getMetodo()).append(" ").append(p.getRuta())
                        .append(" → HTTP ").append(p.getStatus());
                String resumenCuerpo = extraerResumenMensaje(p.getResponseBody());
                if (resumenCuerpo != null && !resumenCuerpo.isEmpty()) {
                    resumenEvidencia.append(" ").append(resumenCuerpo);
                }
                first = false;
            }
        }

        if ("NO PASA".equals(cp.getEstado()) && !cp.getFallas().isEmpty()) {
            resumenEvidencia.append(" · Esperado: ").append(cp.getEsperado());
        }

        String valorResultado = resumenEvidencia.toString();
        if (valorResultado.length() > 1500) {
            valorResultado = valorResultado.substring(0, 1497) + "...";
        }

        // Armar defecto y BugSugerido
        String valorDefecto = "No aplica.";
        if ("PASA".equals(cp.getEstado())) {
            valorDefecto = "No aplica.";
        } else if ("NO EJECUTADO - BLOQUEADO".equals(cp.getEstado())) {
            valorDefecto = "No aplica; bloqueado: " + cp.getCausaBloqueo();
        } else {
            // NO PASA
            ResultadoCP.BugInfo bug = generarBugSugerido(cp, cause);
            cp.setBugSugerido(bug);
            if (bug != null) {
                if (bug.getTitulo().startsWith("BUG06") || bug.getTitulo().startsWith("BUG07")) {
                    valorDefecto = bug.getTitulo();
                } else {
                    valorDefecto = "Pendiente ID Azure: " + bug.getTitulo();
                }
            } else {
                valorDefecto = "Pendiente ID Azure: [" + cp.getHu() + "] " + cp.getTitulo() + " no pasa";
            }
        }

        String valorEjecutor = "Quién ejecutó la prueba y fecha: " + ApiTestConfig.getEjecutor() + " [Calidad] - " + fecha + " (automatizada)";

        ResultadoCP.ExcelInfo excel = new ResultadoCP.ExcelInfo(filaInicio, cp.getEstado(), valorResultado, valorDefecto, valorEjecutor);
        cp.setExcel(excel);
    }

    /** true si todas las aserciones fallidas son textos que solo difieren por caracteres mal decodificados (U+FFFD). */
    private static boolean soloFallaPorCodificacion(ResultadoCP cp) {
        boolean alguna = false;
        for (String falla : cp.getFallas()) {
            for (String linea : falla.split("\n")) {
                if (linea.contains("AssertionFailedError")) {
                    if (!linea.contains("�")) {
                        return false;
                    }
                    alguna = true;
                }
            }
        }
        return alguna;
    }

    private static String extraerResumenMensaje(String body) {
        if (body == null || body.trim().isEmpty()) return null;
        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            if (trimmed.contains("\"mensaje\":")) {
                int start = trimmed.indexOf("\"mensaje\":");
                int quoteStart = trimmed.indexOf("\"", start + 10);
                if (quoteStart != -1) {
                    int quoteEnd = trimmed.indexOf("\"", quoteStart + 1);
                    if (quoteEnd != -1) {
                        return trimmed.substring(quoteStart + 1, quoteEnd);
                    }
                }
            } else if (trimmed.contains("\"errores\":")) {
                int start = trimmed.indexOf("\"errores\":");
                int end = trimmed.indexOf("}", start);
                if (end != -1) {
                    return trimmed.substring(start, end + 1).replace("\"", "").replace("\n", " ");
                }
            }
        }
        if (trimmed.length() > 50) {
            return trimmed.substring(0, 47) + "...";
        }
        return trimmed;
    }

    private static ResultadoCP.BugInfo generarBugSugerido(ResultadoCP cp, Throwable cause) {
        String titulo;
        String severidad = "Media";
        String claseSospechosa = "A confirmar por Backend";

        // Casos conocidos de bugs existentes en Azure DevOps
        if ("CP-04-A".equals(cp.getCp()) && cp.getHttpObtenido().equals("409")) {
            titulo = "BUG06 (#119) - reabrir";
            severidad = "Alta";
            claseSospechosa = "UsuarioServiceImpl / SupabaseAuthService (a confirmar por Backend)";
        } else if ("CP-05-D".equals(cp.getCp()) && cp.getHttpObtenido().equals("403")) {
            titulo = "BUG07 (#120) - reabrir";
            severidad = "Media";
            claseSospechosa = "SecurityConfig / JwtFilter (a confirmar por Backend)";
        } else if ("CP-01-B".equals(cp.getCp()) || "CP-01-E".equals(cp.getCp())) {
            titulo = "[HU01] Login con credenciales inválidas responde 400 en lugar de 401";
            severidad = "Media";
            claseSospechosa = "AuthService.validarCredencialesSupabase / GlobalExceptionHandler (a confirmar por Backend)";
        } else if ("CP-01-C".equals(cp.getCp())) {
            titulo = "[HU01] El bloqueo tras 5 intentos fallidos no se activa";
            severidad = "Alta";
            claseSospechosa = "AuthService.login / LoginAttemptService (a confirmar por Backend)";
        } else if ("CP-01-F".equals(cp.getCp())) {
            titulo = "[HU01] Login de usuario inactivo responde " + cp.getHttpObtenido() + " en lugar de 401";
            severidad = "Media";
            claseSospechosa = "Causa probable: las ramas calidad y Sprint-2 no tienen @ExceptionHandler(CredencialesInvalidasException) en GlobalExceptionHandler (a confirmar por Backend)";
        } else if (("CP-03-B".equals(cp.getCp()) || "CP-03-C".equals(cp.getCp())) && soloFallaPorCodificacion(cp)) {
            titulo = "[HU03] Las respuestas 401 de JwtFilter no declaran charset (las tildes se ven mal en clientes UTF-8)";
            severidad = "Baja";
            claseSospechosa = "JwtFilter: escribe con getWriter() sin setContentType/setCharacterEncoding; Tomcat codifica en ISO-8859-1 "
                    + "y Render entrega 'text/plain; charset=utf-8' (a confirmar por Backend)";
        } else {
            titulo = "[" + cp.getHu() + "] " + cp.getTitulo() + " responde HTTP " + cp.getHttpObtenido() + " en lugar del esperado";
        }

        StringBuilder repro = new StringBuilder();
        for (ResultadoCP.PasoEvidencia p : cp.getEvidencia()) {
            repro.append("- **Paso: ").append(p.getPaso()).append("**\n")
                    .append("  - Petición: `").append(p.getMetodo()).append(" ").append(p.getRuta()).append("`\n");
            if (p.getRequestBody() != null && !p.getRequestBody().isEmpty()) {
                repro.append("  - Body enviado:\n    ```json\n    ").append(p.getRequestBody().replace("\n", "\n    ")).append("\n    ```\n");
            }
            repro.append("  - Respuesta obtenida: HTTP ").append(p.getStatus()).append("\n")
                    .append("  - Body recibido:\n    ```json\n    ").append(p.getResponseBody() != null ? p.getResponseBody().replace("\n", "\n    ") : "").append("\n    ```\n");
        }

        String actual = "HTTP " + cp.getHttpObtenido() + (cause != null ? ": " + EvidenciaRecorder.enmascarar(cause.getMessage()) : "");
        String esperado = cp.getEsperado();

        return new ResultadoCP.BugInfo(titulo, cp.getHu(), cp.getCp(), repro.toString(), actual, esperado, severidad, claseSospechosa);
    }

    public static synchronized void generarReportes() {
        if (resultados.isEmpty()) {
            return;
        }

        File outDir = new File("target/cp-results");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        String runId = ApiTestConfig.getRunId();
        String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());

        // 1. Reporte en consola
        imprimirConsola();

        // 2. resultados-<runId>.json
        escribirJson(new File(outDir, "resultados-" + runId + ".json"), runId, fechaHora);

        // 3. resultados-<runId>.csv
        escribirCsv(new File(outDir, "resultados-" + runId + ".csv"));

        // 4. excel-fill-<runId>.csv (UTF-8 con BOM)
        escribirExcelFillCsv(new File(outDir, "excel-fill-" + runId + ".csv"));

        // 5. bugs-sugeridos-<runId>.md
        escribirBugsMarkdown(new File(outDir, "bugs-sugeridos-" + runId + ".md"), runId);

        System.out.println("\n[ResultadosExtension] Reportes generados exitosamente en: " + outDir.getAbsolutePath());
    }

    private static void imprimirConsola() {
        System.out.println("\n=========================================================================================");
        System.out.println("                         RESULTADOS PRUEBAS DE API (HU01, HU03, HU04, HU05)              ");
        System.out.println("=========================================================================================");
        System.out.printf("%-10s | %-6s | %-24s | %-14s | %-30s | %s%n", "CP", "HU", "Estado", "HTTP Obtenido", "Esperado", "Defecto sugerido");
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------");

        int pasa = 0, noPasa = 0, bloqueado = 0;
        for (ResultadoCP cp : resultados.values()) {
            if ("PASA".equals(cp.getEstado())) pasa++;
            else if ("NO PASA".equals(cp.getEstado())) noPasa++;
            else bloqueado++;

            String defecto = cp.getExcel() != null ? cp.getExcel().getDefecto() : "-";
            String esperadoTrunc = cp.getEsperado();
            if (esperadoTrunc.length() > 30) esperadoTrunc = esperadoTrunc.substring(0, 27) + "...";

            System.out.printf("%-10s | %-6s | %-24s | %-14s | %-30s | %s%n",
                    cp.getCp(), cp.getHu(), cp.getEstado(), cp.getHttpObtenido(), esperadoTrunc, defecto);
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("TOTALES: PASA %d / NO PASA %d / BLOQUEADO %d%n", pasa, noPasa, bloqueado);
        System.out.println("RAMA PROBADA: " + ApiTestConfig.getRamaDesplegada());
        System.out.println("BASE URL:     " + ApiTestConfig.getBaseUrl());
        System.out.println("RUN ID:       " + ApiTestConfig.getRunId());
        System.out.println("=========================================================================================\n");
    }

    private static void escribirJson(File file, String runId, String fechaHora) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("{\n");
            writer.write("  \"runId\": \"" + escapeJson(runId) + "\",\n");
            writer.write("  \"fecha\": \"" + escapeJson(fechaHora) + "\",\n");
            writer.write("  \"baseUrl\": \"" + escapeJson(ApiTestConfig.getBaseUrl()) + "\",\n");
            writer.write("  \"ramaDesplegada\": \"" + escapeJson(ApiTestConfig.getRamaDesplegada()) + "\",\n");
            writer.write("  \"apiDocsInfo\": \"" + escapeJson(ApiTestConfig.getApiDocsInfo()) + "\",\n");
            writer.write("  \"ejecutor\": \"" + escapeJson(ApiTestConfig.getEjecutor()) + "\",\n");
            writer.write("  \"herramienta\": \"JUnit 5 + RestAssured\",\n");
            writer.write("  \"casos\": [\n");

            int cIdx = 0;
            for (ResultadoCP cp : resultados.values()) {
                if (cIdx > 0) writer.write(",\n");
                writer.write("    {\n");
                writer.write("      \"cp\": \"" + escapeJson(cp.getCp()) + "\",\n");
                writer.write("      \"hu\": \"" + escapeJson(cp.getHu()) + "\",\n");
                writer.write("      \"titulo\": \"" + escapeJson(cp.getTitulo()) + "\",\n");
                writer.write("      \"estado\": \"" + escapeJson(cp.getEstado()) + "\",\n");
                writer.write("      \"causaBloqueo\": " + (cp.getCausaBloqueo() != null ? "\"" + escapeJson(cp.getCausaBloqueo()) + "\"" : "null") + ",\n");
                writer.write("      \"fallas\": [");
                for (int f = 0; f < cp.getFallas().size(); f++) {
                    if (f > 0) writer.write(", ");
                    writer.write("\"" + escapeJson(cp.getFallas().get(f)) + "\"");
                }
                writer.write("],\n");
                writer.write("      \"errorDePrueba\": " + cp.isErrorDePrueba() + ",\n");
                writer.write("      \"evidencia\": [\n");

                for (int pIdx = 0; pIdx < cp.getEvidencia().size(); pIdx++) {
                    ResultadoCP.PasoEvidencia p = cp.getEvidencia().get(pIdx);
                    if (pIdx > 0) writer.write(",\n");
                    writer.write("        {\n");
                    writer.write("          \"paso\": \"" + escapeJson(p.getPaso()) + "\",\n");
                    writer.write("          \"metodo\": \"" + escapeJson(p.getMetodo()) + "\",\n");
                    writer.write("          \"ruta\": \"" + escapeJson(p.getRuta()) + "\",\n");
                    writer.write("          \"requestBody\": " + (p.getRequestBody() != null ? "\"" + escapeJson(p.getRequestBody()) + "\"" : "null") + ",\n");
                    writer.write("          \"status\": " + p.getStatus() + ",\n");
                    writer.write("          \"responseBody\": " + (p.getResponseBody() != null ? "\"" + escapeJson(p.getResponseBody()) + "\"" : "null") + ",\n");
                    writer.write("          \"ms\": " + p.getMs() + "\n");
                    writer.write("        }");
                }
                writer.write("\n      ],\n");

                if (cp.getExcel() != null) {
                    writer.write("      \"excel\": {\n");
                    writer.write("        \"filaInicio\": " + cp.getExcel().getFilaInicio() + ",\n");
                    writer.write("        \"pasaNoPasa\": \"" + escapeJson(cp.getExcel().getPasaNoPasa()) + "\",\n");
                    writer.write("        \"resultadoObtenido\": \"" + escapeJson(cp.getExcel().getResultadoObtenido()) + "\",\n");
                    writer.write("        \"defecto\": \"" + escapeJson(cp.getExcel().getDefecto()) + "\",\n");
                    writer.write("        \"ejecutor\": \"" + escapeJson(cp.getExcel().getEjecutor()) + "\"\n");
                    writer.write("      },\n");
                } else {
                    writer.write("      \"excel\": null,\n");
                }

                if (cp.getBugSugerido() != null) {
                    writer.write("      \"bugSugerido\": {\n");
                    writer.write("        \"titulo\": \"" + escapeJson(cp.getBugSugerido().getTitulo()) + "\",\n");
                    writer.write("        \"severidad\": \"" + escapeJson(cp.getBugSugerido().getSeveridad()) + "\"\n");
                    writer.write("      }\n");
                } else {
                    writer.write("      \"bugSugerido\": null\n");
                }

                writer.write("    }");
                cIdx++;
            }
            writer.write("\n  ]\n");
            writer.write("}\n");
        } catch (Exception e) {
            System.err.println("[ResultadosExtension] Error escribiendo JSON: " + e.getMessage());
        }
    }

    private static void escribirCsv(File file) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("cp,hu,estado,http,esperado,fallas,defecto\n");
            for (ResultadoCP cp : resultados.values()) {
                String fallasStr = String.join(" | ", cp.getFallas());
                String defecto = cp.getExcel() != null ? cp.getExcel().getDefecto() : "";
                writer.write(csvCell(cp.getCp()) + ","
                        + csvCell(cp.getHu()) + ","
                        + csvCell(cp.getEstado()) + ","
                        + csvCell(cp.getHttpObtenido()) + ","
                        + csvCell(cp.getEsperado()) + ","
                        + csvCell(fallasStr) + ","
                        + csvCell(defecto) + "\n");
            }
        } catch (Exception e) {
            System.err.println("[ResultadosExtension] Error escribiendo CSV: " + e.getMessage());
        }
    }

    private static void escribirExcelFillCsv(File file) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            // Escribir BOM para UTF-8 (\uFEFF)
            writer.write("\uFEFF");
            writer.write("cp,celda_pasa,valor_pasa,celda_resultado,valor_resultado,celda_defecto,valor_defecto,celda_ejecutor,valor_ejecutor\n");

            for (ResultadoCP cp : resultados.values()) {
                int r = FILAS_EXCEL.getOrDefault(cp.getCp(), 2);
                String celdaPasa = "C" + (r + 34);
                String celdaResultado = "C" + (r + 35);
                String celdaDefecto = "C" + (r + 36);
                String celdaEjecutor = "B" + (r + 42);

                String valorPasa = cp.getEstado();
                String valorResultado = cp.getExcel() != null ? cp.getExcel().getResultadoObtenido() : "";
                String valorDefecto = cp.getExcel() != null ? cp.getExcel().getDefecto() : "No aplica.";
                String valorEjecutor = cp.getExcel() != null ? cp.getExcel().getEjecutor() : "";

                writer.write(csvCell(cp.getCp()) + ","
                        + csvCell(celdaPasa) + ","
                        + csvCell(valorPasa) + ","
                        + csvCell(celdaResultado) + ","
                        + csvCell(valorResultado) + ","
                        + csvCell(celdaDefecto) + ","
                        + csvCell(valorDefecto) + ","
                        + csvCell(celdaEjecutor) + ","
                        + csvCell(valorEjecutor) + "\n");
            }
        } catch (Exception e) {
            System.err.println("[ResultadosExtension] Error escribiendo excel-fill CSV: " + e.getMessage());
        }
    }

    private static void escribirBugsMarkdown(File file, String runId) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("# Reporte de Bugs Sugeridos — Ejecución " + runId + "\n\n");
            writer.write("- **Fecha:** " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()) + "\n");
            writer.write("- **Base URL:** " + ApiTestConfig.getBaseUrl() + "\n");
            writer.write("- **Rama desplegada:** " + ApiTestConfig.getRamaDesplegada() + "\n");
            writer.write("- **Ejecutor:** " + ApiTestConfig.getEjecutor() + "\n\n");

            // Agrupar bugs por título para evitar duplicados en defectos compartidos (ej. CP-01-B y CP-01-E)
            Map<String, List<ResultadoCP>> bugsAgrupados = new LinkedHashMap<>();
            for (ResultadoCP cp : resultados.values()) {
                if ("NO PASA".equals(cp.getEstado()) && cp.getBugSugerido() != null) {
                    bugsAgrupados.computeIfAbsent(cp.getBugSugerido().getTitulo(), k -> new ArrayList<>()).add(cp);
                }
            }

            if (bugsAgrupados.isEmpty()) {
                writer.write("## No se detectaron fallos en los casos de prueba ejecutados.\n\n");
            } else {
                writer.write("## Defectos encontrados en los Casos de Prueba\n\n");
                int bNum = 1;
                for (Map.Entry<String, List<ResultadoCP>> entry : bugsAgrupados.entrySet()) {
                    String bugTitulo = entry.getKey();
                    List<ResultadoCP> listaCP = entry.getValue();
                    ResultadoCP firstCp = listaCP.get(0);
                    ResultadoCP.BugInfo bugInfo = firstCp.getBugSugerido();

                    StringBuilder cpsAfectados = new StringBuilder();
                    for (int i = 0; i < listaCP.size(); i++) {
                        if (i > 0) cpsAfectados.append(", ");
                        cpsAfectados.append(listaCP.get(i).getCp());
                    }

                    writer.write("### Bug " + bNum + ": " + bugTitulo + "\n\n");
                    writer.write("- **HU:** " + bugInfo.getHu() + "\n");
                    writer.write("- **Casos de prueba afectados:** " + cpsAfectados + "\n");
                    writer.write("- **Severidad propuesta:** " + bugInfo.getSeveridad() + "\n");
                    writer.write("- **Componente / Clase sospechosa:** " + bugInfo.getClaseSospechosa() + "\n\n");
                    writer.write("#### Pasos para reproducir (Repro Steps)\n\n");
                    writer.write(bugInfo.getReproSteps() + "\n");
                    writer.write("#### Resultado actual vs. Resultado esperado\n\n");
                    writer.write("- **Resultado actual:** " + bugInfo.getResultadoActual() + "\n");
                    writer.write("- **Resultado esperado:** " + bugInfo.getResultadoEsperado() + "\n\n");
                    writer.write("---\n\n");
                    bNum++;
                }
            }

            // §7.7 Hallazgos del repositorio (no son CP)
            writer.write("## Hallazgos del repositorio (no son CP)\n\n");
            writer.write("### [Build] La rama Sprint-1 no compila tras el merge 263a0aa (GlobalExceptionHandler)\n\n");
            writer.write("- **Severidad:** Alta (impide desplegar `Sprint-1` y que el CI de esa rama pase).\n");
            writer.write("- **Detalle:** En `GlobalExceptionHandler.java`, líneas 117–120, después del método `handleUsuarioInactivo` quedaron dos sentencias sueltas fuera de cualquier método:\n");
            writer.write("  ```java\n");
            writer.write("  body.put(\"mensaje\", \"Error interno del servidor\");\n");
            writer.write("  return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);\n");
            writer.write("  ```\n");
            writer.write("  Son restos sintácticos de la resolución de fusión de ramas.\n");
            writer.write("- **Responsable sugerido:** Backend (autor del merge 263a0aa del 20/10/2026).\n");
            writer.write("- **Comprobación:** Confirmado en `origin/Sprint-1` (263a0aa) con `git show origin/Sprint-1:src/main/java/Fabrica_EAP05/Reservas/Exception/GlobalExceptionHandler.java`. No se modifica ni corrige en esta suite según la regla de §9.\n");
        } catch (Exception e) {
            System.err.println("[ResultadosExtension] Error escribiendo bugs markdown: " + e.getMessage());
        }
    }

    private static String csvCell(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String escapeJson(String s) {
        return DatosPrueba.escapeJson(s);
    }
}
