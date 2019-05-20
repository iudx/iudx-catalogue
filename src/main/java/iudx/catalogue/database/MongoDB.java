package iudx.catalogue.database;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;

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

  private final String TAG_COLLECTION = "tags";
  private final String COLLECTION = "catalogue";

  /**
   * Constructor for MongoDB
   *
   * @param item_database Name of the Item Collection
   * @param schema_database Name of the Schema Collection
   */
  public Future<Void> initDB(Vertx vertx, JsonObject mongoconfig) {

    Future<Void> init_fut = Future.future();
    mongo = MongoClient.createShared(vertx, mongoconfig);

    mongo.createIndex(
        COLLECTION,
        new JsonObject().put("geoJsonLocation", "2dsphere"),
        ar -> {
          if (ar.succeeded()) {
            init_fut.complete();
          } else {
            init_fut.fail(ar.cause());
          }
        });
    return init_fut;
  }
  /**
   * Searches the Mongo DB
   *
   * @param collection Name of the collection
   * @param query Query to the MongoDB
   * @param options Options specify the fields that will (not) be displayed
   * @param message The message to which the result will be replied to
   */
  private void mongoFind(JsonObject query, JsonObject attributeFilter, Message<Object> message) {

    attributeFilter.put("_id", 0);
    query.put("Status", "Live");

    String[] hiddenFields = {"_tags"};

    FindOptions options = new FindOptions();
    options.setFields(attributeFilter);

    mongo.findWithOptions(
        COLLECTION,
        query,
        options,
        res -> {
          if (res.succeeded()) {
            // Send back the response
            JsonArray rep = new JsonArray();
            for (JsonObject j : res.result()) {
              for (String hidden : hiddenFields) {
                if (j.containsKey(hidden)) {
                  j.remove(hidden);
                }
              }
              for (String key : j.fieldNames()) {
                if (key.length() >= 3 && key.substring(0, 3).equals("_$_")) {
                  Object value = j.getValue(key);
                  j.remove(key);
                  key = "$" + key.substring(3);
                  j.put(key, value);
                }
              }

              rep.add(j);
            }
            message.reply(rep);
          } else {
            message.fail(0, "failure");
          }
        });
  }

  public void list(Message<Object> message) {
    JsonObject request_body = (JsonObject) message.body();
    String itemType = request_body.getString("item-type");
    JsonObject query = new JsonObject();
    query.put("item-type", itemType);

    mongoFind(query, new JsonObject(), message);
  }

  public void listTags(Message<Object> message) {
    JsonObject query = new JsonObject();
    JsonObject fields = new JsonObject();
    fields.put("_id", 0);
    fields.put("tag", 1);
    FindOptions options = new FindOptions().setFields(fields);
    mongo.findWithOptions(
        TAG_COLLECTION,
        query,
        options,
        tags -> {
          if (tags.succeeded()) {
            JsonArray tagCollection = new JsonArray();
            for (JsonObject j : tags.result()) {
              tagCollection.add(j);
            }
            message.reply(tagCollection);
          } else {
            message.fail(0, "Failure");
          }
        });
  }

  private void updateNoOfHits(JsonArray tags) {
    JsonObject query = new JsonObject();
    query.put("tag", new JsonObject().put("$in", tags));
    mongo.find(
        TAG_COLLECTION,
        query,
        searchedTags -> {
          if (searchedTags.succeeded()) {
            List<BulkOperation> bulk = new ArrayList<BulkOperation>();
            for (JsonObject j : searchedTags.result()) {
              j.put("noOfHits", (j.getInteger("noOfHits") + 1));
              JsonObject filter = new JsonObject().put("tag", j.getString("tag"));
              bulk.add(BulkOperation.createReplace(filter, j));
            }
            if (!bulk.isEmpty()) {
              mongo.bulkWrite(TAG_COLLECTION, bulk, res2 -> {});
            }
          }
        });
  }

  private JsonObject geo_search_query(JsonObject location) {
    JsonObject query = new JsonObject();

    if (location.getString("bounding-type").equals("circle")) {
      double latitude = location.getDouble("lat");
      double longitude = location.getDouble("long");
      double rad = location.getDouble("radius", 1.0) * 1000.0;

      query.put(
          "$nearSphere",
          new JsonObject()
              .put(
                  "$geometry",
                  new JsonObject()
                      .put("type", "Point")
                      .put("coordinates", new JsonArray("[" + longitude + "," + latitude + "]")))
              .put("$maxDistance", rad));
    }
    return query;
  }

  private String extractString(String s, int b) {
    String ans;
    int i;
    for (i = b; i < s.length() - 1; i++) {
      if (s.charAt(i) == ',' || s.charAt(i) == ')') {
        break;
      }
    }
    ans = s.substring(b, i);
    return ans;
  }

  private JsonArray extractElements(String s) {
    JsonArray elements = new JsonArray();
    for (int i = 1; i < s.length() - 1; i++) {
      if (s.charAt(i) == ',') {
        continue;
      } else {
        JsonArray ele = new JsonArray();
        if (s.charAt(i) == '(') {
          int j;
          for (j = i + 1; j < s.length() - 1; j++) {
            String e = extractString(s, j);
            ele.add(e);
            j = j + e.length();
            if (s.charAt(j) == ')') {
              break;
            }
          }
          i = j;
          elements.add(ele);
        } else {
          String e = extractString(s, i);
          i = i + e.length();
          ele.add(e);
          elements.add(ele);
        }
      }
    }
    return elements;
  }

  private JsonObject decodeQuery(JsonObject requestBody) {
    JsonObject query = new JsonObject();

    if (requestBody.containsKey("attribute-name") && requestBody.containsKey("attribute-value")) {
      JsonArray attributeNames = extractElements(requestBody.getString("attribute-name"));
      JsonArray attributeValues = extractElements(requestBody.getString("attribute-value"));

      if (attributeNames.size() != attributeValues.size()) {
        return null;
      }

      JsonArray expressions = new JsonArray();
      for (int i = 0; i < attributeNames.size(); i++) {
        String key = attributeNames.getJsonArray(i).getString(0);
        JsonArray value = attributeValues.getJsonArray(i);
        //        System.out.println(key);
        //        System.out.println(value.size() );
        //        for (int j = 0; j < value.size(); j++) {
        //          System.out.print(value.getString(j) +" "+ value.getString(j).length());
        //        }
        //        System.out.println();

        if (key.equalsIgnoreCase("tags")) {
          key = "_tags";
          JsonArray tags = attributeValues.getJsonArray(i);
          value = new JsonArray();
          for (int j = 0; j < tags.size(); j++) {
            value.add(tags.getString(i).toLowerCase());
          }
          updateNoOfHits(value);
        } else if (key.equalsIgnoreCase("location")) {
          JsonObject location = new JsonObject();
          location.put("bounding-type", value.getString(0));
          location.put("lat", Double.parseDouble(value.getString(1)));
          location.put("long", Double.parseDouble(value.getString(2)));
          location.put("radius", Double.parseDouble(value.getString(3)));

          key = "geoJsonLocation";
          value = new JsonArray().add(geo_search_query(location));
        } else if (key.charAt(0) == '$') {
          key = "_$_" + key.substring(1);
        }
        JsonObject q = new JsonObject();

        q.put(key, new JsonObject().put("$in", value));
        expressions.add(q);
      }
      query.put("$and", expressions);
    } else if (!requestBody.containsKey("attribute-name")
        && !requestBody.containsKey("attribute-value")) {
      query = null;
    } else if (!requestBody.containsKey("attribute-name")
        && !requestBody.containsKey("attribute-value")) {
      query = null;
    }

    return query;
  }

  private JsonObject decodeFields(JsonObject requestBody) {
    JsonObject fields = new JsonObject();
    if (requestBody.containsKey("attribute-filter")) {
      JsonArray attributeFilter = extractElements(requestBody.getString("attribute-filter"));
      for (int i = 0; i < attributeFilter.size(); i++) {
        String field = attributeFilter.getJsonArray(i).getString(0);
        if (field.charAt(0) == '$') {
          field = "_$_" + field.substring(1);
        }
        fields.put(field, 1);
      }
    }

    return fields;
  }

  @Override
  public void searchAttribute(Message<Object> message) {

    JsonObject request_body = (JsonObject) message.body();
    JsonObject query = decodeQuery(request_body);
    JsonObject fields = decodeFields(request_body);

    if (query == null) {
      message.fail(0, "Bad query");
    } else {
      mongoFind(query, fields, message);
    }
  }

  @Override
  public void count(Message<Object> message) { // TODO Auto-generated method stub
    JsonObject request_body = (JsonObject) message.body();
    JsonObject query = decodeQuery(request_body);

    if (query == null) {
      message.fail(0, "Bad query");
    } else {
      query.put("Status", "Live");
      mongo.count(
          COLLECTION,
          query,
          result -> {
            if (result.succeeded()) {
              JsonObject num = new JsonObject();
              long numItems = result.result();
              num.put("Count", numItems);
              message.reply(num);
            } else {
              message.fail(0, "Failure");
            }
          });
    }
  }

  /**
   * Adds the fields id, Version, Status, Created, Last modified on to the given JsonObject
   *
   * @param doc The document that is being inserted into the database
   * @param version The version of the document
   * @return The JsonObject with additional fields
   */
  private JsonObject addNewAttributes(JsonObject doc, int version, boolean addId, String bulkId) {

    JsonObject updated = doc.copy();
    updated.put("Created", new java.util.Date().toString());
    updated.put("Last modified on", new java.util.Date().toString());
    updated.put("Status", "Live");
    updated.put("Version", version);
    updated.put("Provider", "iudx-provider");
    if (addId) {
      updated.put("id", UUID.randomUUID().toString());
    }
    if (bulkId != null) {
      updated.put("bulk-id", bulkId);
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
        already_present_tags -> {
          if (already_present_tags.succeeded()) {
            List<BulkOperation> bulk = new ArrayList<BulkOperation>();
            Set<String> tags_completed = new HashSet<String>();
            for (JsonObject j : already_present_tags.result()) {
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
            if (!bulk.isEmpty()) {
              mongo.bulkWrite(TAG_COLLECTION, bulk, tagsUpdated -> {});
            }
          }
        });
  }

  private JsonObject removeDollar(JsonObject item) {
    for (String key : item.fieldNames()) {
      if (key.charAt(0) == '$') {
        Object value = item.getValue(key);
        item.remove(key);
        key = "_$_" + key.substring(1);
        item.put(key, value);
      }
    }
    return item;
  }

  @Override
  public void create(Message<Object> message) {

    JsonObject request_body = (JsonObject) message.body();
    JsonObject itemWithoutDol = removeDollar(request_body);
    JsonObject updated_item = addNewAttributes(itemWithoutDol, 1, true, null);

    mongo.insert(
        COLLECTION,
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

  private void updateTags(JsonArray old_tags, JsonArray new_tags) {
    JsonArray toDelete = new JsonArray();
    for (Object t : old_tags) {
      if (new_tags.contains(((String) t))) {
        // Don't change the item count
        new_tags.remove(t);
      } else {
        // Decrease its count
        toDelete.add(t);
      }
    }
    if (!new_tags.isEmpty()) {
      writeTags(new_tags);
    }
    if (!toDelete.isEmpty()) {
      deleteTags(toDelete);
    }
  }

  @Override
  public void update(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject query = new JsonObject();
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    String id = request_body.getString("id");
    String itemType = request_body.getString("item-type");
    query.put("id", id);
    query.put("item-type", itemType);
    // Get its version and tags
    JsonObject fields = new JsonObject();
    fields.put("Version", 1);
    fields.put("_tags", 1);
    FindOptions options = new FindOptions().setFields(fields);
    mongo.findWithOptions(
        COLLECTION,
        query,
        options,
        res -> {
          if (res.succeeded()) {
            int version;
            if (res.result().isEmpty()) {
              System.out.println("Does not exist");
              message.fail(0, "Error: The item with id: " + id + " does not exist.");
            } else {
              JsonObject old_item = res.result().get(0);
              version = old_item.getInteger("Version");
              // Populate update fields
              JsonObject update = new JsonObject(); // Update query

              JsonObject to_update = new JsonObject(); // Update fields
              to_update.put("Status", "Deprecated");
              to_update.put("id", id + "_v" + String.valueOf(version) + ".0");
              to_update.put("Last modified on", new java.util.Date().toString());
              update.put("$set", to_update);

              mongo.updateCollection(
                  COLLECTION,
                  query,
                  update,
                  res2 -> {
                    if (res2.succeeded()) {
                      JsonObject updated_item =
                          addNewAttributes(request_body, version + 1, false, null);
                      updated_item.put("id", id);
                      if (old_item.containsKey("_tags")) {
                        JsonArray old_tags = old_item.getJsonArray("_tags");
                        if (updated_item.containsKey("_tags")) {
                          JsonArray new_tags = updated_item.getJsonArray("_tags");
                          updateTags(old_tags, new_tags);
                        } else {
                          deleteTags(old_tags);
                        }
                      } else {
                        if (updated_item.containsKey("_tags")) {
                          JsonArray new_tags = updated_item.getJsonArray("_tags");
                          writeTags(new_tags);
                        }
                      }

                      mongo.insert(
                          COLLECTION,
                          updated_item,
                          res3 -> {
                            if (res3.succeeded()) {
                              message.reply("Success");
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
  }

  private void deleteTags(JsonArray tags) {
    JsonObject query = new JsonObject();
    query.put("tag", new JsonObject().put("$in", tags));
    mongo.find(
        TAG_COLLECTION,
        query,
        find_tags -> {
          if (find_tags.succeeded()) {
            List<BulkOperation> bulk = new ArrayList<BulkOperation>();
            for (JsonObject j : find_tags.result()) {
              j.put("noOfItems", (j.getInteger("noOfItems") - 1));
              JsonObject filter = new JsonObject().put("tag", j.getString("tag"));
              if (j.getInteger("noOfItems") > 0) {
                bulk.add(BulkOperation.createReplace(filter, j));
              } else {
                bulk.add(BulkOperation.createDelete(filter));
              }
            }
            if (!bulk.isEmpty()) {
              mongo.bulkWrite(TAG_COLLECTION, bulk, res2 -> {});
            }
          }
        });
  }

  @Override
  public void delete(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject request_body = (JsonObject) message.body();

    // Populate query
    JsonObject query = new JsonObject();
    query.put("id", request_body.getString("id"));
    query.put("item-type", request_body.getString("item-type"));

    mongo.findOneAndDelete(
        COLLECTION,
        query,
        res -> {
          if (res.succeeded() && !(res.result() == null)) {
            if (res.result().containsKey("_tags")) {
              deleteTags(res.result().getJsonArray("_tags"));
            }
            message.reply("Success");
          } else if (res.result() == null) {
            message.fail(0, "Item not found");
          } else {
            message.fail(0, "Failure");
          }
        });
  }
}
