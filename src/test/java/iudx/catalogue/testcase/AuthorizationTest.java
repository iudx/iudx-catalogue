package iudx.catalogue.testcase;

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
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.apiserver.APIServerVerticle;
import iudx.catalogue.database.DatabaseVerticle;
import iudx.catalogue.validator.Validator;
import iudx.catalogue.validator.ValidatorInterface;
import iudx.catalogue.validator.ValidatorVerticle;

@ExtendWith(VertxExtension.class)
class AuthorizationTest implements CatlogueTesting {

  private Vertx vertx;
  private WebClient webClient;

  @BeforeAll
  void prepare(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    int procs = Runtime.getRuntime().availableProcessors();

    JsonObject databaseConf =
        new JsonObject()
            .put("http.port", 84545)
            .put("mongo_host", "localhost")
            .put("mongo_port", 27017);

    String database = "mongo";

    DeploymentOptions options = new DeploymentOptions().setConfig(databaseConf);
    vertx.deployVerticle(new DatabaseVerticle(database), options);

    ValidatorInterface v = new Validator();
    vertx.deployVerticle(
        new ValidatorVerticle(v), options, testContext.succeeding(id -> testContext.completeNow()));

    vertx.deployVerticle(
        APIServerVerticle.class.getName(),
        options.setWorker(true).setInstances(procs * 2),
        testContext.succeeding(id -> testContext.completeNow()));

    webClient =
        WebClient.create(
            vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(84545));
  }

  @AfterAll
  void close_up() {
    vertx.close();
  }
  

  @Test
  @DisplayName("Testing in-valid auth for item (JSON) POST to catalogue.")
  public void invalidAuth(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing in-valid (NULL) user ID for item (JSON) POST to catalogue.")
  public void nullUser(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing in-valid (NULL) password for item (JSON) POST to catalogue.")
  public void nullPassword(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing in-valid (NOT_NULL) user ID for item (JSON) POST to catalogue.")
  public void invalidUser(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing in-valid (NOT_NULL) password for item (JSON) POST to catalogue.")
  public void invalidPassword(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing writing with permission for item (JSON) POST to catalogue.")
  public void withWritePermission(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing writing without permission for item (JSON) POST to catalogue.")
  public void withoutWritePermission(VertxTestContext testContext) {}

}
