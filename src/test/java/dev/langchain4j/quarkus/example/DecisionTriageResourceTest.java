package dev.langchain4j.quarkus.example;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DecisionTriageResourceTest {

    @Test
    void testTriageEndpoint() {
        given()
                .contentType(ContentType.JSON)
                .body(new TriageRequest("I need an enterprise invoice and custom payment contract for 500 users."))
                .when()
                .post("/triage/local")
                .then()
                .statusCode(200)
                .body("modelName", equalTo("devops-thiago/classone-gemma4-e2b"))
                .body("department", notNullValue())
                .body("urgentProbability", greaterThanOrEqualTo(0.0f))
                .body("urgentProbability", lessThanOrEqualTo(1.0f))
                .body("frustrationScore", greaterThanOrEqualTo(0.0f));
    }
}
