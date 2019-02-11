package iudx.catalogue.database;

import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class DatabaseVerticle extends AbstractVerticle {

  private static final Logger logger = Logger.getLogger(DatabaseVerticle.class.getName());
  private String action;
  private DatabaseInterface db;
  private static final String database_uri = "mongodb://localhost:27017";
  private static final String database_name = "catalogue";

  public DatabaseVerticle(Vertx vertx, String which_database) {

    if (which_database == "mongo") {

      db = new MongoDB("items", "schemas");

      JsonObject mongoconfig =
          new JsonObject().put("connection_string", database_uri).put("db_name", database_name);

      db.init_db(vertx, mongoconfig);
    }
  }

  @Override
  public void start(Future<Void> startFuture) {

    logger.info("Database Verticle started!");
    vertx
        .eventBus()
        .consumer(
            "database",
            message -> {
              validateRequest(message);
            });
  }

  private void validateRequest(Message<Object> message) {
    // TODO Auto-generated method stub
    logger.info("Database Verticle received message.body() = " + message.body());

    action = (String) message.headers().get("action");

    switch (action) {
      case "read-item":
        {
          db.read_item(message);
          break;
        }

      case "read-schema":
        {
          db.read_schema(message);
          break;
        }

      case "write-item":
        {
          db.write_item(message);
          break;
        }

      case "write-schema":
        {
          db.write_schema(message);
          break;
        }

      case "update-item":
        {
          db.update_item(message);
          break;
        }

      case "update-schema":
        {
          db.update_schema(message);
          break;
        }

      case "delete-item":
        {
          db.delete_item(message);
          break;
        }

      case "delete-schema":
        {
          db.delete_schema(message);
          break;
        }

      case "search-attribute":
        {
          db.search_attribute(message);
          break;
        }
    }
  }
}
