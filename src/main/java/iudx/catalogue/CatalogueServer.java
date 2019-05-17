package iudx.catalogue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.apiserver.APIServerVerticle;
import iudx.catalogue.database.DatabaseVerticle;
import iudx.catalogue.validator.Validator;
import iudx.catalogue.validator.ValidatorInterface;
import iudx.catalogue.validator.ValidatorVerticle;

public class CatalogueServer {

  private static final Logger logger = Logger.getLogger(CatalogueServer.class.getName());

  public static void main(String[] args) {

    int procs = Runtime.getRuntime().availableProcessors();
    Vertx vertx = Vertx.vertx();

    DeploymentOptions options = getConfigOptions("app_conf.json");
    String database = "mongo";
    vertx.deployVerticle(new DatabaseVerticle(database), options);

    ValidatorInterface v = new Validator();
    vertx.deployVerticle(new ValidatorVerticle(v), options);

    vertx.deployVerticle(
        APIServerVerticle.class.getName(),
        options.setWorker(true).setInstances(procs * 2),
        event -> {
          if (event.succeeded()) {
            logger.info("IUDX Catalogue Vert.x API Server is started!");
          } else {
            logger.info("Unable to start IUDX Catalogue Vert.x API Server " + event.cause());
          }
        });
  }

  private static DeploymentOptions getConfigOptions(String filepath) {
    
    DeploymentOptions options = null;
    
    try (InputStream inputStream = new FileInputStream(filepath)) {
      JSONObject rawConfig = new JSONObject(new JSONTokener(inputStream));
      JsonObject config = new JsonObject(rawConfig.toString());
      options = new DeploymentOptions().setConfig(config);
    } catch (Exception e) {
      System.out.println(e);
    }
    return options;
  }
}
