package iudx.catalogue.database;

import io.vertx.core.Future;
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
  public Future<Void> initDB(Vertx vertx, JsonObject mongoconfig);

  /**
   * Searches the database based on the query and returns only those attributes of the documents
   * that are mentioned in the attribute filter.
   *
   * @param message The message from APIServerVerticle which contains the query and the
   *     attributeFilter. The result is replied to this message.
   */
  public void searchAttribute(Message<Object> message);

  /**
   * Searches the database and returns the item whose UUID matches the query.
   *
   * @param message The message from APIServerVerticle which contains the UUID of the item. The item
   *     is replied to this message.
   */
  public void readItem(Message<Object> message);

  /**
   * Searches the database and returns the schema whose UUID matches the query.
   *
   * @param message The message from APIServerVerticle which contains the UUID of the schema. The
   *     schema is replied to this message.
   */
  public void readSchema(Message<Object> message);

  /**
   * Inserts the item into the database.
   *
   * @param message The message from APIServerVerticle which contains the item. The UUID of the item
   *     is replied to this message.
   */
  public void writeItem(Message<Object> message);

  /**
   * Inserts the schema into the database.
   *
   * @param message The message from APIServerVerticle which contains the schema. The UUID of the
   *     item is replied to this message.
   */
  public void writeSchema(Message<Object> message);

  /**
   * Updates the item in the database
   *
   * @param message The message from APIServerVerticle which contains the id of the item and the new
   *     item.
   */
  public void updateItem(Message<Object> message);

  /**
   * Updates the schema in the database
   *
   * @param message The message from APIServerVerticle which contains the id of the schema and the
   *     new schema.
   */
  public void updateSchema(Message<Object> message);

  /**
   * Deletes the item from the database
   *
   * @param message The message from APIServerVerticle which contains the id of the item.
   */
  public void deleteItem(Message<Object> message);

  /**
   * Deletes the schema from the database
   *
   * @param message The message from APIServerVerticle which contains the id of the schema.
   */
  public void deleteSchema(Message<Object> message);
}
