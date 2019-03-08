package iudx.catalogue.testcase;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.apiserver.APIServerVerticle;
import iudx.catalogue.database.DatabaseVerticle;
import iudx.catalogue.validator.Validator;
import iudx.catalogue.validator.ValidatorInterface;
import iudx.catalogue.validator.ValidatorVerticle;
import java.util.logging.LogManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(VertxExtension.class)
class AuthorizationTest {

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
  @DisplayName("Testing in-valid auth for item (JSON) POST to catalogue.")
  public void invalidAuth(VertxTestContext testContext) {
    testContext.completeNow();
  }
//
//  @Test
//  @DisplayName("Testing in-valid (NULL) user ID for item (JSON) POST to catalogue.")
//  public void nullUser(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing in-valid (NULL) password for item (JSON) POST to catalogue.")
//  public void nullPassword(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing in-valid (NOT_NULL) user ID for item (JSON) POST to catalogue.")
//  public void invalidUser(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing in-valid (NOT_NULL) password for item (JSON) POST to catalogue.")
//  public void invalidPassword(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing writing with permission for item (JSON) POST to catalogue.")
//  public void withWritePermission(VertxTestContext testContext) {}
//
//  @Test
//  @DisplayName("Testing writing without permission for item (JSON) POST to catalogue.")
//  public void withoutWritePermission(VertxTestContext testContext) {}

}
