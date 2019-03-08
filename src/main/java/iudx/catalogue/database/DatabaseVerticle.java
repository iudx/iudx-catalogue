package iudx.catalogue.database;

import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class DatabaseVerticle extends AbstractVerticle {

  private static final Logger logger = Logger.getLogger(DatabaseVerticle.class.getName());
  private DatabaseInterface db;
  private static final String database_name = "catalogue";
  
  /**
   * Constructor for DatabaseVerticle
   *
   * @param which_database The name of the database
   */
  public DatabaseVerticle(String which_database) {

    if ("mongo".equals(which_database)) {
      db = new MongoDB("items", "schemas");
    }
  }

  @Override
  public void start(Future<Void> startFuture) {
	JsonObject mongoconfig;
	String database_uri;

    logger.info("Database Verticle started!");
    vertx
        .eventBus()
        .consumer(
            "database",
            message -> {
              validateRequest(message);
            });

    database_uri =
        "mongodb://"
            + config().getString("mongo_host", "localhost")
            + ":"
            + config().getInteger("mongo_port", 27017).toString();

    mongoconfig =
        new JsonObject().put("connection_string", database_uri).put("db_name", database_name);

    db.initDB(vertx, mongoconfig);
    startFuture.complete();
  }

  /**
   * Calls the database method depending on the action sent by APIServerVerticle
   *
   * @param message Contains the action that has to be performed and the required parameters.
   */
  private void validateRequest(Message<Object> message) {
    // TODO Auto-generated method stub
    logger.info("Database Verticle received message.body() = " + message.body());

    String action = (String) message.headers().get("action");

    switch (action) {
      case "read-item":
        {
          db.readItem(message);
          break;
        }

      case "read-schema":
        {
          db.readSchema(message);
          break;
        }

      case "write-item":
        {
          db.writeItem(message);
          break;
        }

      case "write-schema":
        {
          db.writeSchema(message);
          break;
        }

      case "update-item":
        {
          db.updateItem(message);
          break;
        }

      case "update-schema":
        {
          db.updateSchema(message);
          break;
        }

      case "delete-item":
        {
          db.deleteItem(message);
          break;
        }

      case "delete-schema":
        {
          db.deleteSchema(message);
          break;
        }

      case "search-attribute":
        {
          db.searchAttribute(message);
          break;
        }
      default : 
      {
    	  break;
      }
    }
  }
}
