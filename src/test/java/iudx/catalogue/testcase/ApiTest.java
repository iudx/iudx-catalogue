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
class ApiTest implements CatlogueTesting {

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
  @DisplayName("Testing valid item (JSON and auth) POST to catalogue.")
  public void insertValidItem(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing in-valid item (JSON) POST to catalogue.")
  public void insertInvalidItem(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing valid tag search.")
  public void validTagSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing in-valid tag search.")
  public void invalidTagSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing valid attribute filter search.")
  public void validAttributeFilterSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing in-valid attribute filter search.")
  public void invalidAttributeFilterSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing home page.")
  public void testHomePage(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing item search.")
  public void testItemSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with in-valid key.")
  public void invalidKeySearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with empty key.")
  public void emptyKeySearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with empty attribute filters.")
  public void emptyAttributeFilterSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with white-spaces in attribute filters.")
  public void whiteSpaceAttributeFilterSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with valid SINGLE TAG.")
  public void validSingleTagSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with valid MULTIPLE TAG.")
  public void validMultipleTagSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with valid SINGLE in-valid TAG.")
  public void invalidSingleTagSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with valid MULTIPLE in-valid TAG.")
  public void invalidMultipleTagSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with valid single in-case-sensitive TAG.")
  public void validCaseInsensitiveTagSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with valid ID.")
  public void validIdSearch(VertxTestContext testContext) {}

  @Test
  @DisplayName("Testing catalogue search with in-valid ID.")
  public void invalidIdSearch(VertxTestContext testContext) {}
}
