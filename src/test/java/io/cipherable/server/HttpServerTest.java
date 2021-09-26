package io.cipherable.server;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.cipherable.config.ConfigRepository;
import io.cipherable.server.aws.KmsCryptographicOperations;
import io.cipherable.server.crypto.SodiumCryptographyService;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import software.amazon.awssdk.regions.Region;

@ExtendWith(VertxExtension.class)
public class HttpServerTest {
  RequestSpecification requestSpecification;

  public HttpServerTest() {
    requestSpecification = new RequestSpecBuilder()
        .setBaseUri("http://"+Main.DEFAULT_HOST)
        .setPort(Main.DEFAULT_PORT)
        .setContentType(ContentType.JSON)
        .build();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @BeforeAll
  public static void beforeAll(Vertx vertx, VertxTestContext ctx) throws InterruptedException{

    vertx.deployVerticle(
        () ->
            new HttpServer(
                Main.DEFAULT_HOST,
                Main.DEFAULT_PORT,
                Mockito.mock(ConfigRepository.class),
                Mockito.mock(KmsCryptographicOperations.class),
                Mockito.mock(SodiumCryptographyService.class),
                Region.US_WEST_2),
        new DeploymentOptions(),
        ctx.succeedingThenComplete());
    ctx.awaitCompletion(10, TimeUnit.SECONDS);
  }

  @Test
  public void testPing() {
    given(requestSpecification)
        .when().get("/ping")
        .then()
        .assertThat()
        .statusCode(200)
        .body( equalTo("pong"));
  }

}
