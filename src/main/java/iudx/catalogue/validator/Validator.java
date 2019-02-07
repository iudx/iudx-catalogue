package iudx.catalogue.validator;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class Validator implements ValidatorInterface {

  @Override
  public void validate_item(Future<Void> messageHandler, JsonObject item, JsonObject schema) {
    // TODO Auto-generated method stub
    JSONObject p = new JSONObject(item.getMap());
    JSONObject s = new JSONObject(schema.getMap());

    try {
      Schema sc = SchemaLoader.load(s);
      sc.validate(p); // throws a ValidationException if this object is invalid
    } catch (Exception e) {
      messageHandler.failed();
    }
    messageHandler.complete();
  }
}
