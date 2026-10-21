package Fabrica_EAP05.Reservas.api;

import java.util.ArrayList;
import java.util.List;

public class ResultadoCP {
    private String cp;
    private String hu;
    private String titulo;
    private String estado = "NO PASA"; // "PASA", "NO PASA", "NO EJECUTADO - BLOQUEADO"
    private String causaBloqueo;
    private List<String> fallas = new ArrayList<>();
    private boolean errorDePrueba = false;
    private List<PasoEvidencia> evidencia = new ArrayList<>();
    private ExcelInfo excel;
    private BugInfo bugSugerido;
    private String httpObtenido = "-";
    private String esperado = "-";

    public ResultadoCP() {}

    public ResultadoCP(String cp, String hu, String titulo) {
        this.cp = cp;
        this.hu = hu;
        this.titulo = titulo;
    }

    public static class PasoEvidencia {
        private String paso;
        private String metodo;
        private String ruta;
        private String requestBody;
        private int status;
        private String responseBody;
        private long ms;

        public PasoEvidencia() {}

        public PasoEvidencia(String paso, String metodo, String ruta, String requestBody, int status, String responseBody, long ms) {
            this.paso = paso;
            this.metodo = metodo;
            this.ruta = ruta;
            this.requestBody = requestBody;
            this.status = status;
            this.responseBody = responseBody;
            this.ms = ms;
        }

        public String getPaso() { return paso; }
        public void setPaso(String paso) { this.paso = paso; }
        public String getMetodo() { return metodo; }
        public void setMetodo(String metodo) { this.metodo = metodo; }
        public String getRuta() { return ruta; }
        public void setRuta(String ruta) { this.ruta = ruta; }
        public String getRequestBody() { return requestBody; }
        public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getResponseBody() { return responseBody; }
        public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
        public long getMs() { return ms; }
        public void setMs(long ms) { this.ms = ms; }
    }

    public static class ExcelInfo {
        private int filaInicio;
        private String pasaNoPasa;
        private String resultadoObtenido;
        private String defecto;
        private String ejecutor;

        public ExcelInfo() {}

        public ExcelInfo(int filaInicio, String pasaNoPasa, String resultadoObtenido, String defecto, String ejecutor) {
            this.filaInicio = filaInicio;
            this.pasaNoPasa = pasaNoPasa;
            this.resultadoObtenido = resultadoObtenido;
            this.defecto = defecto;
            this.ejecutor = ejecutor;
        }

        public int getFilaInicio() { return filaInicio; }
        public void setFilaInicio(int filaInicio) { this.filaInicio = filaInicio; }
        public String getPasaNoPasa() { return pasaNoPasa; }
        public void setPasaNoPasa(String pasaNoPasa) { this.pasaNoPasa = pasaNoPasa; }
        public String getResultadoObtenido() { return resultadoObtenido; }
        public void setResultadoObtenido(String resultadoObtenido) { this.resultadoObtenido = resultadoObtenido; }
        public String getDefecto() { return defecto; }
        public void setDefecto(String defecto) { this.defecto = defecto; }
        public String getEjecutor() { return ejecutor; }
        public void setEjecutor(String ejecutor) { this.ejecutor = ejecutor; }
    }

    public static class BugInfo {
        private String titulo;
        private String hu;
        private String cp;
        private String reproSteps;
        private String resultadoActual;
        private String resultadoEsperado;
        private String severidad;
        private String claseSospechosa;

        public BugInfo() {}

        public BugInfo(String titulo, String hu, String cp, String reproSteps, String resultadoActual, String resultadoEsperado, String severidad, String claseSospechosa) {
            this.titulo = titulo;
            this.hu = hu;
            this.cp = cp;
            this.reproSteps = reproSteps;
            this.resultadoActual = resultadoActual;
            this.resultadoEsperado = resultadoEsperado;
            this.severidad = severidad;
            this.claseSospechosa = claseSospechosa;
        }

        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getHu() { return hu; }
        public void setHu(String hu) { this.hu = hu; }
        public String getCp() { return cp; }
        public void setCp(String cp) { this.cp = cp; }
        public String getReproSteps() { return reproSteps; }
        public void setReproSteps(String reproSteps) { this.reproSteps = reproSteps; }
        public String getResultadoActual() { return resultadoActual; }
        public void setResultadoActual(String resultadoActual) { this.resultadoActual = resultadoActual; }
        public String getResultadoEsperado() { return resultadoEsperado; }
        public void setResultadoEsperado(String resultadoEsperado) { this.resultadoEsperado = resultadoEsperado; }
        public String getSeveridad() { return severidad; }
        public void setSeveridad(String severidad) { this.severidad = severidad; }
        public String getClaseSospechosa() { return claseSospechosa; }
        public void setClaseSospechosa(String claseSospechosa) { this.claseSospechosa = claseSospechosa; }
    }

    public String getCp() { return cp; }
    public void setCp(String cp) { this.cp = cp; }
    public String getHu() { return hu; }
    public void setHu(String hu) { this.hu = hu; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCausaBloqueo() { return causaBloqueo; }
    public void setCausaBloqueo(String causaBloqueo) { this.causaBloqueo = causaBloqueo; }
    public List<String> getFallas() { return fallas; }
    public void setFallas(List<String> fallas) { this.fallas = fallas; }
    public void addFalla(String falla) { this.fallas.add(falla); }
    public boolean isErrorDePrueba() { return errorDePrueba; }
    public void setErrorDePrueba(boolean errorDePrueba) { this.errorDePrueba = errorDePrueba; }
    public List<PasoEvidencia> getEvidencia() { return evidencia; }
    public void setEvidencia(List<PasoEvidencia> evidencia) { this.evidencia = evidencia; }
    public void addEvidencia(PasoEvidencia paso) { this.evidencia.add(paso); }
    public ExcelInfo getExcel() { return excel; }
    public void setExcel(ExcelInfo excel) { this.excel = excel; }
    public BugInfo getBugSugerido() { return bugSugerido; }
    public void setBugSugerido(BugInfo bugSugerido) { this.bugSugerido = bugSugerido; }
    public String getHttpObtenido() { return httpObtenido; }
    public void setHttpObtenido(String httpObtenido) { this.httpObtenido = httpObtenido; }
    public String getEsperado() { return esperado; }
    public void setEsperado(String esperado) { this.esperado = esperado; }
}
