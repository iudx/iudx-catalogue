package iudx.catalogue.validator;

import io.vertx.core.json.JsonObject;
/** Validates an JSON Item against a JSON schema */
public interface ValidatorInterface {

  /**
   * Validate item against schema. Throws an Exception when item is invalid
   *
   * @param item - the item which will be validated
   * @param schema - the schema against which the validation will be done
   * @throws Exception - when validation fails
   */
  public void validateItem(JsonObject item, JsonObject schema) throws Exception;
}
