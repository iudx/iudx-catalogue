package iudx.catalogue.validator;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface ValidatorInterface {
	
	public void validate_item(Future<Void> messageHandler, JsonObject request_body);
	
	public void validate_schema(Future<Void> messageHandler, JsonObject request_body);

}
