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
              validateRequest(message);
            });
  }

  private void validateRequest(Message<Object> message) {

    logger.info("Validator Verticle received message.body() = " + message.headers());

    action = message.headers().get("action");

    switch (action) {
      case "validate-item":
        {
          validate_item(message);
          break;
        }
    }
  }

  private void validate_item(Message<Object> message) {

    boolean skip_validation = false;
    if (message.headers().contains("skip_validation")) {
      skip_validation = Boolean.parseBoolean(message.headers().get("skip_validation"));
      logger.info("Skip validation found with value " + Boolean.toString(skip_validation));
    }

    if (skip_validation) {
      message.reply("success");
      return;
    }

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
                try {
                  isValid.validate_item(item, schema);
                } catch (Exception e) {
                  message.fail(0, "fail");
                }
                message.reply("success");
              } else if (database_reply.failed()) {
                message.fail(0, database_reply.cause().toString());
              } else {
                message.fail(0, "fail");
              }
            });
  }
}
