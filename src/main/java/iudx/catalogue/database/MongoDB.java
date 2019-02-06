package iudx.catalogue.database;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class MongoDB extends AbstractVerticle implements DatabaseInterface {
  private MongoClient mongo;
  private String ITEM_COLLECTION, SCHEMA_COLLECTION;

  public MongoDB(String item_database, String schema_database) {
    mongo = MongoClient.createShared(vertx, config());
    ITEM_COLLECTION = item_database;
    SCHEMA_COLLECTION = schema_database;
  }

  private void mongo_find(
      String collection,
      JsonObject query,
      FindOptions options,
      Future<Void> messageHandler,
      Message<Object> message) {

    mongo.findWithOptions(
        collection,
        query,
        options,
        res -> {
          if (res.succeeded()) {
            // Send back the response
            message.reply(res.result());
            messageHandler.succeeded();
          } else {
            messageHandler.failed();
          }
        });
  }

  @Override
  public void search_attribute(Future<Void> messageHandler, Message<Object> message) {

    JsonObject request_body = (JsonObject) message.body();
    JsonObject query = new JsonObject();
    JsonObject fields = new JsonObject();

    // Populate query
    Iterator<Map.Entry<String, Object>> it = request_body.iterator();
    while (it.hasNext()) {
      String key = it.next().getKey();
      JsonArray values = (JsonArray) it.next().getValue();
      if (!key.equalsIgnoreCase("attributeFilter")) {
        for (int i = 0; i < values.size(); i++) {
          query.put(key, values.getString(i));
        }
      }
    }

    // Populate fields
    JsonArray filter = request_body.getJsonArray("attributeFilter");
    for (int i = 0; i < filter.size(); i++) {
      fields.put(filter.getString(i), 1);
    }

    // Call mongo find
    FindOptions options = new FindOptions().setFields(fields);
    mongo_find(ITEM_COLLECTION, query, options, messageHandler, message);
  }

  @Override
  public void read_item(Future<Void> messageHandler, Message<Object> message) {

    JsonObject query = new JsonObject();
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    query.put("UUID", request_body.getString("itemID"));

    // Call mongo find
    mongo_find(ITEM_COLLECTION, query, new FindOptions(), messageHandler, message);
  }

  @Override
  public void read_schema(Future<Void> messageHandler, Message<Object> message) {
    // TODO Auto-generated method stub

  }

  private JsonObject addNewAttributes(JsonObject doc, String version) {
    JsonObject updated = doc.copy();
    updated.put("Created", new java.util.Date());
    updated.put("Last modified on", new java.util.Date());
    updated.put("Status", "Live");
    updated.put("Version", version);
    updated.put("UUID", UUID.randomUUID().toString());

    return updated;
  }

  @Override
  public void write_item(Future<Void> messageHandler, Message<Object> message) {

    JsonObject request_body = (JsonObject) message.body();
    JsonObject updated_item = addNewAttributes(request_body, "1.0");

    mongo.insert(
        ITEM_COLLECTION,
        updated_item,
        res -> {
          if (res.succeeded()) {
            messageHandler.succeeded();
          } else {
            messageHandler.fail(res.cause());
          }
        });
  }

  @Override
  public void write_schema(Future<Void> messageHandler, Message<Object> message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void update_item(Future<Void> messageHandler, Message<Object> message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void update_schema(Future<Void> messageHandler, Message<Object> message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void delete_item(Future<Void> messageHandler, Message<Object> message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void delete_schema(Future<Void> messageHandler, Message<Object> message) {
    // TODO Auto-generated method stub

  }
}
