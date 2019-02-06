package iudx.catalogue.database;

import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;

public class DatabaseVerticle extends AbstractVerticle {

  private static final Logger logger = Logger.getLogger(DatabaseVerticle.class.getName());
  private String action;
  private DatabaseInterface db;

  public DatabaseVerticle(DatabaseInterface db) {
    this.db = db;
  }

  @Override
  public void start(Future<Void> startFuture) {

    logger.info("Database Verticle started!");
    vertx
        .eventBus()
        .consumer(
            "database",
            message -> {
              Future<Void> requestHandler = validateRequest(message);

              if (requestHandler.succeeded()) {
                message.reply("success");
              } else {
                message.fail(0, "failure");
              }
            });
  }

  private Future<Void> validateRequest(Message<Object> message) {
    // TODO Auto-generated method stub
    logger.info("Database Verticle received message.body() = " + message.body());

    action = (String) message.headers().get("action");

    Future<Void> messageHandler = Future.future();

    switch (action) {
      case "read-item":
        {
          db.read_item(messageHandler, message);
          break;
        }

      case "read-schema":
        {
          db.read_schema(messageHandler, message);
          break;
        }

      case "write-item":
        {
          db.write_item(messageHandler, message);
          break;
        }

      case "write-schema":
        {
          db.write_schema(messageHandler, message);
          break;
        }

      case "update-item":
        {
          db.update_item(messageHandler, message);
          break;
        }

      case "update-schema":
        {
          db.update_schema(messageHandler, message);
          break;
        }

      case "delete-item":
        {
          db.delete_item(messageHandler, message);
          break;
        }

      case "delete-schema":
        {
          db.delete_schema(messageHandler, message);
          break;
        }

      case "search-attribute":
        {
          db.search_attribute(messageHandler, message);
          break;
        }
    }

    return messageHandler;
  }
}
