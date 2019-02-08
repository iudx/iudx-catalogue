package iudx.catalogue;

import java.util.logging.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
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

    String database = "mongo";
    vertx.deployVerticle(new DatabaseVerticle(vertx, database));

    ValidatorInterface v = new Validator();
    vertx.deployVerticle(new ValidatorVerticle(v));

    vertx.deployVerticle(
        APIServerVerticle.class.getName(),
        new DeploymentOptions().setWorker(true).setInstances(procs * 2),
        event -> {
          if (event.succeeded()) {
            logger.info("IUDX Catalogue Vert.x API Server is started!");
          } else {
            logger.info("Unable to start IUDX Catalogue Vert.x API Server " + event.cause());
          }
        });
  }
}
