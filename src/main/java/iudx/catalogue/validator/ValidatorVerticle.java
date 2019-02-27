package iudx.catalogue.validator;

import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * A verticle which will handle the request to validate the item before adding to the database.
 * Whenever we get an API request to add an item in the catalogue, we will validate the item against
 * a schema. This Schema will be specified in the item. This verticle will be listing to the
 * eventbus for validation request.
 */
public class ValidatorVerticle extends AbstractVerticle {

  private static final Logger logger = Logger.getLogger(ValidatorVerticle.class.getName());
  private String action;
  private ValidatorInterface isValid;

  /** @param validator - Implementation of ValidatorInterface we want to use to validate */
  public ValidatorVerticle(ValidatorInterface validator) {
    isValid = validator;
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

  /**
   * Handler to handle any request directed to this verticle
   *
   * @param message - request that came on the eventbus
   */
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
  /**
   * Validates the item received in the message body. The schema that is used to validate this item
   * is strored in the refCatalogueSchema field of the item. Uses the instance of ValidatorInterface
   * assigned on the construction to validate the item.
   *
   * @param message - request that came on the eventbus
   */
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
