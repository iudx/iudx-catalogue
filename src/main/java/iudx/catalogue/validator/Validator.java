package iudx.catalogue.validator;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

import io.vertx.core.json.JsonObject;

public class Validator implements ValidatorInterface {

  @Override
  public void validate_item(JsonObject item, JsonObject schema) throws Exception {
    // TODO Auto-generated method stub
    JSONObject p = new JSONObject(item.getMap());
    JSONObject s = new JSONObject(schema.getMap());

    Schema sc = SchemaLoader.load(s);
    sc.validate(p); // throws a ValidationException if this object is invalid
  }
}
