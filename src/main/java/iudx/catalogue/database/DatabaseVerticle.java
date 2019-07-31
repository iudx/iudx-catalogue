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
      db = new MongoDB();
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
            + config().getString("mongo_host", "catalogue-database-mongodb")
            + ":"
            + config().getInteger("mongo_port", 27017).toString();

    mongoconfig =
        new JsonObject().put("connection_string", database_uri).put("db_name", database_name);

    Future<Void> init_fut = db.initDB(vertx, mongoconfig);
    init_fut.setHandler(startFuture.completer());
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
      case "list":
        {
          db.list(message);
          break;
        }

      case "get-tags":
        {
          db.listTags(message);
          break;
        }

      case "create":
        {
          db.create(message);
          break;
        }

      case "update":
        {
          db.update(message);
          break;
        }

      case "delete":
        {
          db.delete(message);
          break;
        }

      case "search-attribute":
        {
          db.searchAttribute(message);
          break;
        }
      case "count":
      {
    	  db.count(message);
    	  break;
      }
      case "bulkcreate":
      {
    	  db.bulkCreate(message);
    	  break;
      }
      case "bulkupdate":
      {
    	  db.bulkUpdate(message);
    	  break;
      }
      case "bulkdelete":
      {
    	  db.bulkDelete(message);
    	  break;
      }
      default:
        {
          break;
        }
    }
  }
}
