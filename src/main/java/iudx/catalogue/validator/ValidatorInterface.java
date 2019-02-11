package iudx.catalogue.validator;

import io.vertx.core.json.JsonObject;

public interface ValidatorInterface {

  public void validate_item(JsonObject item, JsonObject schema) throws Exception;
}
