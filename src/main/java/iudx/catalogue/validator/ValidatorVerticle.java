package iudx.catalogue.validator;

import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class ValidatorVerticle extends AbstractVerticle {

  private static final Logger logger = Logger.getLogger(ValidatorVerticle.class.getName());
  private String action;
  private ValidatorInterface isValid;

  public ValidatorVerticle(ValidatorInterface v) {
    isValid = v;
  }

  @Override
  public void start(Future<Void> startFuture) {
    logger.info("Validator Verticle started!");

    vertx
        .eventBus()
        .consumer(
            "validator",
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
    logger.info("Validator Verticle received message.body() = " + message.headers());

    action = message.headers().get("action");

    Future<Void> messageHandler = Future.future();

    switch (action) {
      case "validate-item":
        {
          validate_item(messageHandler, message);
          break;
        }
    }

    return messageHandler;
  }

  private void validate_item(Future<Void> messageHandler, Message<Object> message) {
    // TODO Auto-generated method stub
    // Implement the validation schema block

    JsonObject item = (JsonObject) message.body();

    // Get schema from database and validate
    String schemaID = item.getString("refCatalogueSchema");
    DeliveryOptions database_action = new DeliveryOptions();
    database_action.addHeader("action", "read-schema");
    JsonObject request_body = new JsonObject();
    request_body.put("id", schemaID);
    vertx
        .eventBus()
        .send(
            "database",
            request_body,
            database_action,
            database_reply -> {
              if (database_reply.succeeded()) {
                logger.info(database_reply.result().body().toString());
                JsonObject schema = (JsonObject) database_reply.result().body();
                isValid.validate_item(messageHandler, item, schema);

              } else if (database_reply.failed()) {
                messageHandler.fail(database_reply.cause());
              } else {
                messageHandler.failed();
              }
            });
  }
}
