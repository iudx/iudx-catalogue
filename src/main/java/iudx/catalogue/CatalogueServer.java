package iudx.catalogue;

import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import iudx.catalogue.apiserver.APIServerVerticle;
import iudx.catalogue.database.DatabaseVerticle;
import iudx.catalogue.validator.Validator;
import iudx.catalogue.validator.ValidatorInterface;
import iudx.catalogue.validator.ValidatorVerticle;

public class CatalogueServer extends AbstractVerticle {

  private static final Logger logger = Logger.getLogger(CatalogueServer.class.getName());
  
	public static void main(String[] args) {
		Launcher.executeCommand("run", CatalogueServer.class.getName());
	}

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    DeploymentOptions options = new DeploymentOptions().setConfig(config());

    ValidatorInterface v = new Validator();

    vertx.deployVerticle(
        new ValidatorVerticle(v),
        options,
        event -> {
          if (event.succeeded()) {
            String database = "mongo";

            vertx.deployVerticle(
                new DatabaseVerticle(database),
                options,
                event2 -> {
                  if (event2.succeeded()) {
                    int procs = Runtime.getRuntime().availableProcessors();
                    vertx.deployVerticle(
                        APIServerVerticle.class.getName(),
                        options.setWorker(true).setInstances(procs * 2),
                        event3 -> {
                          if (event3.succeeded()) {
                            logger.info("IUDX Catalogue Vert.x API Server is started!");
                            startFuture.complete();
                          } else {
                            logger.info("Unable to start API Verticle " + event3.cause());
                            startFuture.fail(event3.cause());
                          }
                        });
                  } else {
                    logger.info("Unable to start Database Verticle " + event2.cause());
                    startFuture.fail(event2.cause());
                  }
                });

          } else {
            logger.info("Unable to start Validator Verticle " + event.cause());
            startFuture.fail(event.cause());
          }
        });
  }
}
