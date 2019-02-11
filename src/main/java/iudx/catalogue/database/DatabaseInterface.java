package iudx.catalogue.database;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public interface DatabaseInterface {

  public void init_db(Vertx vertx, JsonObject config);

  public void search_attribute(Message<Object> message);

  public void read_item(Message<Object> message);

  public void read_schema(Message<Object> message);

  public void write_item(Message<Object> message);

  public void write_schema(Message<Object> message);

  public void update_item(Message<Object> message);

  public void update_schema(Message<Object> message);

  public void delete_item(Message<Object> message);

  public void delete_schema(Message<Object> message);
}
