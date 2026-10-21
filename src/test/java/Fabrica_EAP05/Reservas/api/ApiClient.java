package Fabrica_EAP05.Reservas.api;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.params.CoreConnectionPNames;

public class ApiClient {

    private static RequestSpecification baseRequest() {
        int timeout = (int) ApiTestConfig.getTimeoutMs();
        RestAssuredConfig config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout)
                        .setParam(CoreConnectionPNames.SO_TIMEOUT, timeout));

        return RestAssured.given()
                .config(config)
                .baseUri(ApiTestConfig.getBaseUrl())
                .relaxedHTTPSValidation();
    }

    public static Response login(String paso, String email, String contrasena) {
        String body = DatosPrueba.bodyLogin(email, contrasena);
        long start = System.currentTimeMillis();
        Response res = baseRequest()
                .contentType("application/json")
                .body(body)
                .when()
                .post("/api/auth/login");
        long ms = System.currentTimeMillis() - start;

        EvidenciaRecorder.registrarPaso(paso, "POST", "/api/auth/login", body, res.getStatusCode(), res.getBody().asString(), ms);
        return res;
    }

    public static Response registrarCliente(String paso, String bodyJson) {
        long start = System.currentTimeMillis();
        Response res = baseRequest()
                .contentType("application/json")
                .body(bodyJson != null ? bodyJson : "")
                .when()
                .post("/api/usuario/registrar");
        long ms = System.currentTimeMillis() - start;

        EvidenciaRecorder.registrarPaso(paso, "POST", "/api/usuario/registrar", bodyJson, res.getStatusCode(), res.getBody().asString(), ms);
        return res;
    }

    public static Response registrarProveedor(String paso, String token, String bodyJson) {
        RequestSpecification req = baseRequest().contentType("application/json");
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (bodyJson != null) {
            req.body(bodyJson);
        }

        long start = System.currentTimeMillis();
        Response res = req.when().post("/api/usuario/registrar/proveedor");
        long ms = System.currentTimeMillis() - start;

        String loggedReq = (token != null ? "Header Authorization: Bearer " + token + "\n" : "") + (bodyJson != null ? bodyJson : "");
        EvidenciaRecorder.registrarPaso(paso, "POST", "/api/usuario/registrar/proveedor", loggedReq, res.getStatusCode(), res.getBody().asString(), ms);
        return res;
    }

    public static Response logout(String paso, String token) {
        RequestSpecification req = baseRequest();
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }

        long start = System.currentTimeMillis();
        Response res = req.when().post("/api/auth/logout");
        long ms = System.currentTimeMillis() - start;

        String loggedReq = (token != null ? "Header Authorization: Bearer " + token : "Sin Header Authorization");
        EvidenciaRecorder.registrarPaso(paso, "POST", "/api/auth/logout", loggedReq, res.getStatusCode(), res.getBody().asString(), ms);
        return res;
    }

    public static Response sondaSesionCliente(String paso, String token) {
        RequestSpecification req = baseRequest()
                .contentType("application/json")
                .body("{}");
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }

        long start = System.currentTimeMillis();
        Response res = req.when().post("/api/reservas/reservar");
        long ms = System.currentTimeMillis() - start;

        String loggedReq = (token != null ? "Header Authorization: Bearer " + token + "\n" : "") + "{}";
        EvidenciaRecorder.registrarPaso(paso, "POST", "/api/reservas/reservar", loggedReq, res.getStatusCode(), res.getBody().asString(), ms);
        return res;
    }

    public static String obtenerTokenAdmin() {
        if (!ApiTestConfig.isAdminValid()) {
            return null;
        }
        Response res = login("Login prerrequisito admin", ApiTestConfig.getAdminEmail(), ApiTestConfig.getAdminPassword());
        if (res.getStatusCode() == 200) {
            return res.jsonPath().getString("token");
        }
        return null;
    }
}
