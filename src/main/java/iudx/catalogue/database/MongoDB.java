package iudx.catalogue.database;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class MongoDB extends AbstractVerticle implements DatabaseInterface {

  private MongoClient mongo;

  private String ITEM_COLLECTION, SCHEMA_COLLECTION, TAG_COLLECTION;
  /**
   * Constructor for MongoDB
   *
   * @param item_database Name of the Item Collection
   * @param schema_database Name of the Schema Collection
   */
  public MongoDB(String item_database, String schema_database, String tag_database) {

    ITEM_COLLECTION = item_database;
    SCHEMA_COLLECTION = schema_database;
    TAG_COLLECTION = tag_database;
  }

  public void initDB(Vertx vertx, JsonObject mongoconfig) {

    mongo = MongoClient.createShared(vertx, mongoconfig);
  }
  /**
   * Searches the Mongo DB
   *
   * @param collection Name of the collection
   * @param query Query to the MongoDB
   * @param options Options specify the fields that will (not) be displayed
   * @param message The message to which the result will be replied to
   */
  private void mongoFind(
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

  private void updateHitsTags(JsonArray tags) {
    JsonObject query = new JsonObject();
    query.put("tag", new JsonObject().put("$in", tags));
    mongo.find(
        TAG_COLLECTION,
        query,
        res -> {
          if (res.succeeded()) {
            List<BulkOperation> bulk = new ArrayList<BulkOperation>();
            for (JsonObject j : res.result()) {
              j.put("noOfHits", (j.getInteger("noOfHits") + 1));
              JsonObject filter = new JsonObject().put("tag", j.getString("tag"));
              bulk.add(BulkOperation.createReplace(filter, j));
            }
            mongo.bulkWrite(TAG_COLLECTION, bulk, res2 -> {});
          }
        });
  }

  @Override
  public void searchAttribute(Message<Object> message) {

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
          if (values.size() == 1) {
            query.put("_tags", values.getString(0).toLowerCase());
            updateHitsTags(new JsonArray().add(query.getString("_tags")));
          } else {
            JsonArray tag_values = new JsonArray();
            for (int i = 0; i < values.size(); i++) {
              tag_values.add(values.getString(i).toLowerCase());
            }
            query.put("_tags", new JsonObject().put("$in", tag_values));
            updateHitsTags(tag_values);
          }
        } else {
          for (int i = 0; i < values.size(); i++) {
            query.put(key, values.getString(i));
          }
        }
      }
    }

    // Populate fields
    if (request_body.containsKey("attributeFilter")) {
      JsonArray filter = request_body.getJsonArray("attributeFilter");
      for (int i = 0; i < filter.size(); i++) {
        fields.put(filter.getString(i), 1);
      }
    }

    // Call mongo find
    FindOptions options = new FindOptions().setFields(fields);
    mongoFind(ITEM_COLLECTION, query, options, message);
  }

  @Override
  public void readItem(Message<Object> message) {

    JsonObject query = new JsonObject();
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    query.put("id", request_body.getString("id"));

    // Call mongo find
    mongoFind(ITEM_COLLECTION, query, new FindOptions(), message);
  }
  /**
   * Replaces the '$' in the fields of schema with '&'
   *
   * @param schema The schema whose fields have to be changed
   * @return The schema whose fields have '&' for '$'
   */
  private JsonObject encodeSchema(JsonObject schema) {

    String[] temp = StringUtils.split(schema.encode(), "$");
    String encodedSchema = StringUtils.join(temp, "&");
    return new JsonObject(encodedSchema);
  }
  /**
   * Replaces the '&' in the fields of encoded schema with '$'
   *
   * @param encodedSchema The encoded schema whose state has to be reverted
   * @return The original schema, obtained by replacing the '&' in the fields of encoded schema by
   *     '$'
   */
  private JsonObject decodeSchema(JsonObject encodedSchema) {

    String[] temp = StringUtils.split(encodedSchema.encode(), "&");
    String schema = StringUtils.join(temp, "$");
    return new JsonObject(schema);
  }

  @Override
  public void readSchema(Message<Object> message) {

    JsonObject m = (JsonObject) message.body();
    JsonObject query = new JsonObject();

    query.put("id", m.getString("id"));

    mongo.findOne(
        SCHEMA_COLLECTION,
        query,
        new JsonObject(),
        res -> {
          if (res.succeeded()) {
            message.reply(decodeSchema(res.result()));
          } else {
            message.fail(0, "failure");
          }
        });
  }
  /**
   * Adds the fields id, Version, Status, Created, Last modified on to the given JsonObject
   *
   * @param doc The document that is being inserted into the database
   * @param version The version of the document
   * @return The JsonObject with additional fields
   */
  private JsonObject addNewAttributes(JsonObject doc, int version, boolean addId) {

    JsonObject updated = doc.copy();
    updated.put("Created", new java.util.Date().toString());
    updated.put("Last modified on", new java.util.Date().toString());
    updated.put("Status", "Live");
    updated.put("Version", version);
    updated.put("Provider", "iudx-provider");
    if (addId) {
      updated.put("id", UUID.randomUUID().toString());
    }

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

  private void writeTags(JsonArray tags) {
    JsonObject query = new JsonObject();
    query.put("tag", new JsonObject().put("$in", tags));
    mongo.find(
        TAG_COLLECTION,
        query,
        res -> {
          if (res.succeeded()) {
            List<BulkOperation> bulk = new ArrayList<BulkOperation>();
            Set<String> tags_completed = new HashSet<String>();
            for (JsonObject j : res.result()) {
              tags_completed.add(j.getString("tag"));
              j.put("noOfItems", (j.getInteger("noOfItems") + 1));
              JsonObject filter = new JsonObject().put("tag", j.getString("tag"));
              bulk.add(BulkOperation.createReplace(filter, j));
            }
            for (Object tag : tags) {
              if (tags_completed.contains((String) tag)) {
                continue;
              }
              JsonObject ins = new JsonObject();
              ins.put("tag", (String) tag);
              ins.put("noOfHits", 0);
              ins.put("noOfItems", 1);
              bulk.add(BulkOperation.createInsert(ins));
            }
            mongo.bulkWrite(TAG_COLLECTION, bulk, res2 -> {});
          }
        });
  }

  @Override
  public void writeItem(Message<Object> message) {

    JsonObject request_body = (JsonObject) message.body();
    JsonObject updated_item = addNewAttributes(request_body, 1, true);

    mongo.insert(
        ITEM_COLLECTION,
        updated_item,
        res -> {
          if (res.succeeded()) {
            if (updated_item.containsKey("_tags")) {
              writeTags(updated_item.getJsonArray("_tags"));
            }
            message.reply(updated_item.getString("id"));
          } else {
            message.fail(0, "failure");
          }
        });
  }

  @Override
  public void writeSchema(Message<Object> message) {

    JsonObject request_body = (JsonObject) message.body();

    mongo.insert(
        SCHEMA_COLLECTION,
        encodeSchema(request_body),
        res -> {
          if (res.succeeded()) {
            message.reply("success");
          } else {
            message.fail(0, "failure");
          }
        });
  }

  private void updateItemTags(JsonArray old_tags, JsonArray new_tags) {
    JsonArray to_dec = new JsonArray();
    for (Object t : old_tags) {
      if (new_tags.contains(((String) t))) {
        // Don't change the item count
        new_tags.remove(t);
      } else {
        // Decrease its count
        to_dec.add(t);
      }
    }
    writeTags(new_tags);
    decItemsTags(to_dec);
  }

  @Override
  public void updateItem(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject query = new JsonObject();
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    if (request_body.containsKey("id")) {
      String id = request_body.getString("id");
      query.put("id", id);
      // Get its version and tags
      JsonObject fields = new JsonObject();
      fields.put("Version", 1);
      fields.put("_tags", 1);
      FindOptions options = new FindOptions().setFields(fields);
      mongo.findWithOptions(
          ITEM_COLLECTION,
          query,
          options,
          res -> {
            if (res.succeeded()) {
              int version;
              if (res.result().isEmpty()) {
                System.out.println("Does not exist");
                message.reply("Error: The item with id: " + id + " does not exist.");
              } else if (res.result().size() > 1) {
                System.out.println("Multiple items");
                message.reply("Error: There are multiple items with id: " + id);
              } else {
                JsonObject old_item = res.result().get(0);
                version = old_item.getInteger("Version");
                // Populate update fields
                JsonObject update = new JsonObject();
                JsonObject to_update = new JsonObject();
                to_update.put("Status", "Deprecated");
                to_update.put("id", id + "_v" + String.valueOf(version) + ".0");
                to_update.put("Last modified on", new java.util.Date().toString());
                update.put("$set", to_update);
                mongo.updateCollection(
                    ITEM_COLLECTION,
                    query,
                    update,
                    res2 -> {
                      if (res2.succeeded()) {
                        JsonObject updated_item =
                            addNewAttributes(request_body, version + 1, false);
                        if (old_item.containsKey("_tags")) {
                          JsonArray old_tags = old_item.getJsonArray("_tags");
                          if (updated_item.containsKey("_tags")) {
                            JsonArray new_tags = updated_item.getJsonArray("_tags");
                            updateItemTags(old_tags, new_tags);
                          }
                        }

                        mongo.insert(
                            ITEM_COLLECTION,
                            updated_item,
                            res3 -> {
                              if (res3.succeeded()) {
                                message.reply(updated_item.getString("id"));
                              } else {
                                message.fail(0, "failure");
                              }
                            });
                      } else {
                        message.fail(0, "failure");
                      }
                    });
              }
            } else {
              message.fail(0, "failure");
            }
          });
    } else {
      message.reply("Specify the id of the item that you want to update");
    }
  }

  @Override
  public void updateSchema(Message<Object> message) {
    // TODO Auto-generated method stub

  }

  private void decItemsTags(JsonArray tags) {
    JsonObject query = new JsonObject();
    query.put("tag", new JsonObject().put("$in", tags));
    mongo.find(
        TAG_COLLECTION,
        query,
        res -> {
          if (res.succeeded()) {
            List<BulkOperation> bulk = new ArrayList<BulkOperation>();
            for (JsonObject j : res.result()) {
              j.put("noOfItems", (j.getInteger("noOfItems") - 1));
              JsonObject filter = new JsonObject().put("tag", j.getString("tag"));
              bulk.add(BulkOperation.createReplace(filter, j));
            }
            mongo.bulkWrite(TAG_COLLECTION, bulk, res2 -> {});
          }
        });
  }

  @Override
  public void deleteItem(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject query = new JsonObject();
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    query.put("id", request_body.getString("id"));

    mongo.findOneAndDelete(
        ITEM_COLLECTION,
        query,
        res -> {
          if (res.succeeded() && !(res.result() == null)) {
            if (res.result().containsKey("_tags")) {
              decItemsTags(res.result().getJsonArray("_tags"));
            }
            message.reply("Success");
          } else if (res.result() == null) {
            message.reply("Item not found");
          } else {
            message.fail(0, "Failure");
          }
        });
  }

  @Override
  public void deleteSchema(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject query = new JsonObject();
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    query.put("id", request_body.getString("id"));

    mongo.findOneAndDelete(
        SCHEMA_COLLECTION,
        query,
        res -> {
          if (res.succeeded() && !(res.result() == null)) {
            message.reply("Success");
          } else if (res.result() == null) {
            message.reply("Schema not found");
          } else {
            message.fail(0, "Failure");
          }
        });
  }
}
