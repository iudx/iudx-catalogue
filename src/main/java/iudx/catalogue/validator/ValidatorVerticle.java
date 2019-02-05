package iudx.catalogue.validator;

import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class ValidatorVerticle extends AbstractVerticle {

  private static final Logger logger = Logger.getLogger(ValidatorVerticle.class.getName());
  String action;
  JsonObject request_body;

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
    request_body = (JsonObject) message.body();

    Future<Void> messageHandler = Future.future();

    switch (action) {
      case "validate-item":
        {
          validate_item(messageHandler);
          break;
        }

      case "validate-schema":
        {
          validate_schema(messageHandler);
          break;
        }
    }

    return messageHandler;
  }

  private void validate_schema(Future<Void> messageHandler) {
    // TODO Auto-generated method stub

    // Implement the validation item block

    messageHandler.complete();
  }

  private void validate_item(Future<Void> messageHandler) {
    // TODO Auto-generated method stub

    // Implement the validation schema block
    messageHandler.complete();
  }
}
