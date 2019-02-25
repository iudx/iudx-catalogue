package iudx.catalogue.database;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public interface DatabaseInterface {

  /**
   * Initialization of the database
   *
   * @param vertx Reference to the Vert.x instance that deployed the verticles.
   * @param config Configuration to initialize the database
   */
  public void init_db(Vertx vertx, JsonObject config);

  /**
   * Searches the database based on the query and returns only those attributes of the documents
   * that are mentioned in the attribute filter.
   *
   * @param message The message from APIServerVerticle which contains the query and the
   *     attributeFilter. The result is replied to this message.
   */
  public void search_attribute(Message<Object> message);

  /**
   * Searches the database and returns the item whose UUID matches the query.
   *
   * @param message The message from APIServerVerticle which contains the UUID of the item. The item
   *     is replied to this message.
   */
  public void read_item(Message<Object> message);

  /**
   * Searches the database and returns the schema whose UUID matches the query.
   *
   * @param message The message from APIServerVerticle which contains the UUID of the schema. The
   *     schema is replied to this message.
   */
  public void read_schema(Message<Object> message);

  /**
   * Inserts the item into the database.
   *
   * @param message The message from APIServerVerticle which contains the item. The UUID of the item
   *     is replied to this message.
   */
  public void write_item(Message<Object> message);

  /**
   * Inserts the schema into the database.
   *
   * @param message The message from APIServerVerticle which contains the schema. The UUID of the
   *     item is replied to this message.
   */
  public void write_schema(Message<Object> message);

  /**
   * Updates the item in the database
   *
   * @param message The message from APIServerVerticle which contains the id of the item and the new
   *     item.
   */
  public void update_item(Message<Object> message);

  /**
   * Updates the schema in the database
   *
   * @param message The message from APIServerVerticle which contains the id of the schema and the
   *     new schema.
   */
  public void update_schema(Message<Object> message);

  /**
   * Deletes the item from the database
   *
   * @param message The message from APIServerVerticle which contains the id of the item.
   */
  public void delete_item(Message<Object> message);

  /**
   * Deletes the schema from the database
   *
   * @param message The message from APIServerVerticle which contains the id of the schema.
   */
  public void delete_schema(Message<Object> message);
}
