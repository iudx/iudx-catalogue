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

  public void list(Message<Object> message);

  public void listTags(Message<Object> message);

  /**
   * Inserts the item into the database.
   *
   * @param message The message from APIServerVerticle which contains the item. The UUID of the item
   *     is replied to this message.
   */
  public void create(Message<Object> message);

  /**
   * Updates the item in the database
   *
   * @param message The message from APIServerVerticle which contains the id of the item and the new
   *     item.
   */
  public void update(Message<Object> message);

  /**
   * Deletes the item from the database
   *
   * @param message The message from APIServerVerticle which contains the id of the item.
   */
  public void delete(Message<Object> message);

  public void count(Message<Object> message);

  public void bulkUpdate(Message<Object> message);

  public void bulkDelete(Message<Object> message);

  public void bulkCreate(Message<Object> message);
}
