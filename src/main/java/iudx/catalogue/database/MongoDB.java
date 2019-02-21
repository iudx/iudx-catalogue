package iudx.catalogue.database;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class MongoDB extends AbstractVerticle implements DatabaseInterface {

  private MongoClient mongo;

  private String ITEM_COLLECTION, SCHEMA_COLLECTION;

  public MongoDB(String item_database, String schema_database) {

    ITEM_COLLECTION = item_database;
    SCHEMA_COLLECTION = schema_database;
  }

  public void init_db(Vertx vertx, JsonObject mongoconfig) {

    mongo = MongoClient.createShared(vertx, mongoconfig);
  }

  private void mongo_find(
      String collection, JsonObject query, FindOptions options, Message<Object> message) {

    JsonObject fields = options.getFields();
    fields.put("_id", 0);
    
    options.setFields(fields);

    mongo.findWithOptions(
        collection,
        query,
        options,
        res -> {
          if (res.succeeded()) {
            // Send back the response
            JsonArray rep = new JsonArray();
            for (JsonObject j : res.result()) {
              if (j.containsKey("_tags")) {
                j.remove("_tags");
              }
              rep.add(j);
            }
            message.reply(rep);
          } else {
            message.fail(0, "failure");
          }
        });
  }

  @Override
  public void search_attribute(Message<Object> message) {

    JsonObject request_body = (JsonObject) message.body();
    JsonObject query = new JsonObject();
    JsonObject fields = new JsonObject();

    // Populate query
    Iterator<Map.Entry<String, Object>> it = request_body.iterator();
    while (it.hasNext()) {
      String key = it.next().getKey();
      JsonArray values = request_body.getJsonArray(key);
      if (!key.equalsIgnoreCase("attributeFilter")) {
        if (key.equalsIgnoreCase("tags")) {
          for (int i = 0; i < values.size(); i++) {
            query.put("_tags", values.getString(i).toLowerCase());
          }
        } else {
          for (int i = 0; i < values.size(); i++) {
            query.put(key, values.getString(i));
          }
        }
      }
    }

    // Do not output the _id and _tags field of mongo

    // Populate fields
    if (request_body.containsKey("attributeFilter")) {
      JsonArray filter = request_body.getJsonArray("attributeFilter");
      for (int i = 0; i < filter.size(); i++) {
        fields.put(filter.getString(i), 1);
      }
    }

    // Call mongo find
    FindOptions options = new FindOptions().setFields(fields);
    mongo_find(ITEM_COLLECTION, query, options, message);
  }

  @Override
  public void read_item(Message<Object> message) {

    JsonObject query = new JsonObject();
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    query.put("UUID", request_body.getString("id"));

    // Call mongo find
    mongo_find(ITEM_COLLECTION, query, new FindOptions(), message);
  }

  private JsonObject encode_schema(JsonObject schema) {

    String[] temp = StringUtils.split(schema.encode(), "$");
    String encodedSchema = StringUtils.join(temp, "&");
    return new JsonObject(encodedSchema);
  }

  private JsonObject decode_schema(JsonObject encodedSchema) {

    String[] temp = StringUtils.split(encodedSchema.encode(), "&");
    String schema = StringUtils.join(temp, "$");
    return new JsonObject(schema);
  }

  @Override
  public void read_schema(Message<Object> message) {

    JsonObject m = (JsonObject) message.body();
    JsonObject query = new JsonObject();

    query.put("UUID", m.getString("id"));

    mongo.findOne(
        SCHEMA_COLLECTION,
        query,
        new JsonObject(),
        res -> {
          if (res.succeeded()) {
            message.reply(decode_schema(res.result()));
          } else {
            message.fail(0, "failure");
          }
        });
  }

  private JsonObject addNewAttributes(JsonObject doc, String version) {

    JsonObject updated = doc.copy();
    updated.put("Created", new java.util.Date().toString());
    updated.put("Last modified on", new java.util.Date().toString());
    updated.put("Status", "Live");
    updated.put("Version", version);
    updated.put("UUID", UUID.randomUUID().toString());

    if (updated.containsKey("tags")) {
      JsonArray tagsInLowerCase = new JsonArray();
      JsonArray tags = updated.getJsonArray("tags");

      for (Object i : tags) {
        tagsInLowerCase.add(((String) i).toLowerCase());
      }
      updated.put("_tags", tagsInLowerCase);
    }

    return updated;
  }

  @Override
  public void write_item(Message<Object> message) {

    JsonObject request_body = (JsonObject) message.body();
    JsonObject updated_item = addNewAttributes(request_body, "1.0");

    mongo.insert(
        ITEM_COLLECTION,
        updated_item,
        res -> {
          if (res.succeeded()) {
            message.reply(updated_item.getString("UUID"));
          } else {
            message.fail(0, "failure");
          }
        });
  }

  @Override
  public void write_schema(Message<Object> message) {

    JsonObject request_body = (JsonObject) message.body();

    mongo.insert(
        SCHEMA_COLLECTION,
        encode_schema(request_body),
        res -> {
          if (res.succeeded()) {
            message.reply("success");
          } else {
            message.fail(0, "failure");
          }
        });
  }

  @Override
  public void update_item(Message<Object> message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void update_schema(Message<Object> message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void delete_item(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject query = new JsonObject();
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    query.put("UUID", request_body.getString("id"));

    mongo.removeDocument(
        ITEM_COLLECTION,
        query,
        res -> {
          if (res.succeeded()) {
            message.reply("Success");
          } else {
            message.fail(0, "Failure");
          }
        });
  }

  @Override
  public void delete_schema(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject query = new JsonObject();
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    query.put("UUID", request_body.getString("id"));

    mongo.removeDocument(
        SCHEMA_COLLECTION,
        query,
        res -> {
          if (res.succeeded()) {
            message.reply("Success");
          } else {
            message.fail(0, "Failure");
          }
        });
  }
}
