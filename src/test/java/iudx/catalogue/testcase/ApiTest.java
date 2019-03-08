package iudx.catalogue.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.logging.LogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.apiserver.APIServerVerticle;
import iudx.catalogue.database.DatabaseVerticle;
import iudx.catalogue.validator.Validator;
import iudx.catalogue.validator.ValidatorInterface;
import iudx.catalogue.validator.ValidatorVerticle;

@ExtendWith(VertxExtension.class)
class ApiTest implements CatlogueTesting {

  static WebClient webClient;

  @BeforeAll
  public static void prepare(Vertx vertx, VertxTestContext testContext) {
    int procs = Runtime.getRuntime().availableProcessors();

    LogManager.getLogManager().reset();
    
    WebClientOptions options =
        new WebClientOptions()
            .setTrustAll(true)
            .setSsl(true)
            .setDefaultHost("localhost")
            .setDefaultPort(8455);
    webClient = WebClient.create(vertx, options);

    Checkpoint val = testContext.checkpoint(3);

    JsonObject databaseConf =
        new JsonObject()
            .put("http.port", 8455)
            .put("mongo_host", "localhost")
            .put("mongo_port", 27017);

    String database = "mongo";

    DeploymentOptions DeploymentOptions = new DeploymentOptions().setConfig(databaseConf);

    vertx.deployVerticle(
        new DatabaseVerticle(database),
        DeploymentOptions,
        testContext.succeeding(id -> val.flag()));

    ValidatorInterface v = new Validator();
    vertx.deployVerticle(
        new ValidatorVerticle(v), DeploymentOptions, testContext.succeeding(id -> val.flag()));

    vertx.deployVerticle(
        APIServerVerticle.class.getName(),
        DeploymentOptions.setWorker(true).setInstances(procs * 2),
        testContext.succeeding(id -> val.flag()));
  }

  @AfterAll
  public static void close_up(Vertx vertx) {
    vertx.close();
  }

  @Test
  @DisplayName("Testing valid item (JSON and auth) POST to catalogue.")
  public void insertValidItem(Vertx vertx, VertxTestContext testContext) {

    JsonObject data = new JsonObject().put("hello", "OH yeah hello").put("how", "youhu");
    webClient
        .postAbs("https://arun:arunrbccps@localhost:8455/cat/items")
        .ssl(true)
        .putHeader("Content-Type", "application/json")
        .putHeader("skip_validation", "true")
        .as(BodyCodec.string())
        .sendJsonObject(
            data,
            testContext.succeeding(
                resp -> {
                  testContext.verify(
                      () -> {
                        assertThat(resp.statusCode()).isEqualTo(200);
                        assertThat(resp.body().isEmpty()).isFalse();
                        System.out.println(resp.body()); 
                        testContext.completeNow();
                      });
                }));
 }

//  @Test
//  @DisplayName("Testing in-valid item (JSON) POST to catalogue.")
//  public void insertInvalidItem(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing valid tag search.")
//  public void validTagSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing in-valid tag search.")
//  public void invalidTagSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing valid attribute filter search.")
//  public void validAttributeFilterSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing in-valid attribute filter search.")
//  public void invalidAttributeFilterSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing home page.")
//  public void testHomePage(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing item search.")
//  public void testItemSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with in-valid key.")
//  public void invalidKeySearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with empty key.")
//  public void emptyKeySearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with empty attribute filters.")
//  public void emptyAttributeFilterSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with white-spaces in attribute filters.")
//  public void whiteSpaceAttributeFilterSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with valid SINGLE TAG.")
//  public void validSingleTagSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with valid MULTIPLE TAG.")
//  public void validMultipleTagSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with valid SINGLE in-valid TAG.")
//  public void invalidSingleTagSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with valid MULTIPLE in-valid TAG.")
//  public void invalidMultipleTagSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with valid single in-case-sensitive TAG.")
//  public void validCaseInsensitiveTagSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with valid ID.")
//  public void validIdSearch(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing catalogue search with in-valid ID.")
//  public void invalidIdSearch(VertxTestContext testContext) {}
}
