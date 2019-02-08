package iudx.catalogue.database;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;

public interface DatabaseInterface {

  public void init_db();

  public void search_attribute(Future<Void> messageHandler, Message<Object> message);

  public void read_item(Future<Void> messageHandler, Message<Object> message);

  public void read_schema(Future<Void> messageHandler, Message<Object> message);

  public void write_item(Future<Void> messageHandler, Message<Object> message);

  public void write_schema(Future<Void> messageHandler, Message<Object> message);

  public void update_item(Future<Void> messageHandler, Message<Object> message);

  public void update_schema(Future<Void> messageHandler, Message<Object> message);

  public void delete_item(Future<Void> messageHandler, Message<Object> message);

  public void delete_schema(Future<Void> messageHandler, Message<Object> message);
}
