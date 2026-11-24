package org.acme.discord.api;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.acme.testresources.PostgreSqlTestResource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
@QuarkusTestResource(PostgreSqlTestResource.class)
class ErrorContractIT {

    @Test
    void shouldRejectMissingRequiredBodiesWithValidationDetails() {
        for (String path : List.of("/api/users", "/api/guilds", "/api/roles", "/api/channels")) {
            Response response = given().contentType("application/json").post(path);

            assertError(response, 400, "VALIDATION_ERROR");
            response.then()
                    .body("message", equalTo("Request validation failed"))
                    .body("violations.field", hasItem("request"));
        }
    }

    @Test
    void shouldPreserveMethodAndMediaTypeProtocolErrors() {
        given().post("/api/users/{id}", UUID.randomUUID())
                .then().statusCode(405);

        given().contentType("text/plain").body("not-json").post("/api/users")
                .then().statusCode(415);
    }

    @Test
    void shouldReturnClientSafeErrorForUnexpectedHttpFailure() {
        Response response = given().get("/api/test/internal-error");

        assertError(response, 500, "INTERNAL_SERVER_ERROR");
        response.then()
                .body("message", equalTo("An unexpected error occurred. Please contact support."))
                .body("violations", hasSize(0));
        String body = response.asString().toLowerCase();
        assertFalse(body.contains("illegalstateexception"));
        assertFalse(body.contains("sql"));
        assertFalse(body.contains("password"));
        assertFalse(body.contains("secret"));
        assertFalse(body.contains("internalerrortestresource"));
    }

    private static void assertError(Response response, int status, String code) {
        response.then()
                .statusCode(status)
                .contentType("application/json")
                .body("code", equalTo(code))
                .body("message", not(nullValue()))
                .body("timestamp", not(nullValue()))
                .body("violations", not(nullValue()));
    }
}
