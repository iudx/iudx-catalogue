package iudx.catalogue.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.json.JSONArray;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;

public class MongoDB extends AbstractVerticle implements DatabaseInterface {

  private MongoClient mongo;

  private final String TAG_COLLECTION = "tags";
  private String COLLECTION="";
  private static final Logger logger = Logger.getLogger(MongoDB.class.getName());
  private static boolean isCacheEmpty = true;
  private static Map<String, JsonArray> resourceCache = new HashMap<>();
  
  /**
   * Constructor for MongoDB
   *
   * @param //item_database Name of the Item Collection
   * @param //schema_database Name of the Schema Collection
   */
  //Future<Void> init_fut;

  public Future<Void> initDB(Vertx vertx, JsonObject mongoconfig) {

    Future<Void> init_fut = Future.future();
    mongo = MongoClient.createShared(vertx, mongoconfig);

    mongo.createIndex(
        "catalogue",
        new JsonObject().put("geoJsonLocation", "2dsphere"),
        ar -> {
          if (ar.succeeded()) {
            //init_fut.complete();
              System.out.println("index created");
             Future<Void> populateFuture = populateHashMap();
              populateFuture.setHandler(res->{
                  if(res.succeeded()){
                      resourceCache.entrySet().forEach(entry ->{
                          System.out.println("===="+entry.getKey()+": "+entry.getValue());
                      });
                      isCacheEmpty = false;
                      init_fut.complete();
                  }
                  else{
                      logger.info("Failed");
                      init_fut.fail(res.cause());
                  }
              });
          } else {
              logger.info("Failed");
              init_fut.fail(ar.cause());
          }
        });
    return init_fut;
  }

    private Future<Void> populateHashMap() {
      Future<Void> hashFut = Future.future();
      resourceCache.put("resourceServerIds",new JsonArray());
      resourceCache.put("resourceGroupIds",new JsonArray());
      resourceCache.put("resourceItemIds",new JsonArray());
      resourceCache.put("providerIds",new JsonArray());

      if(isCacheEmpty) {
            //populate HashMap
          JsonObject field = new JsonObject();
          field.put("id",1);
          FindOptions options = new FindOptions().setFields(field);
          JsonObject query = new JsonObject();
          query.put("itemType.value", "resourceServer");

          mongo.findWithOptions("catalogue", query, options, res->{
              if(res.succeeded()){
                  for(JsonObject resSObj:res.result())
                      resourceCache.get("resourceServerIds").add(resSObj.getString("id"));
                  query.put("itemType.value", "resourceServerGroup");
                  mongo.findWithOptions("catalogue", query, options, res1->{
                      if(res1.succeeded()) {
                          for (JsonObject resGObj : res1.result())
                              resourceCache.get("resourceGroupIds").add(resGObj.getString("id"));
                          query.put("itemType.value", "resourceItem");
                          mongo.findWithOptions("catalogue", query, options,res2->{
                              if(res2.succeeded()) {
                                  for (JsonObject resObj : res2.result())
                                      resourceCache.get("resourceItemIds").add(resObj.getString("id"));
                                  mongo.distinct("catalogue","provider.value",String.class.getName(),res3->{
                                      if(res3.succeeded()){
                                        resourceCache.put("providerIds",res3.result());      
                                          hashFut.complete();
                                      }else {
                                          if(!res3.succeeded())
                                            hashFut.fail(res3.cause());
                                      }
                                  });
                              } else {
                                  if(!res2.succeeded())
                                    hashFut.fail(res2.cause());
                              }
                          });
                      }else{
                          if(!res1.succeeded())
                            hashFut.fail(res1.cause());
                      }
                  });
              }else{
                  if(!res.succeeded())
                      hashFut.fail(res.cause());
              }
          });
        }else{
          hashFut.complete();
      }
    return hashFut;
    }

    /**
   * Searches the Mongo DB
   *
   * @param //collection Name of the collection
   * @param //query Query to the MongoDB
   * @param //options Options specify the fields that will (not) be displayed
   * @param //message The message to which the result will be replied to
   */
  private JsonObject addFieldsWithDOll(JsonObject res) {
    JsonObject temp = new JsonObject();
    Set<String> keysWithDoll = new HashSet<String>();
    for (String key : res.fieldNames()) {
      Object value = res.getValue(key);
      if (key.length() >= 3 && key.substring(0, 3).equals("_$_")) {
        keysWithDoll.add(key);
        key = "$" + key.substring(3);
        temp.put(key, value);
      }
      if (value.getClass() == JsonObject.class) {
        JsonObject newVal = addFieldsWithDOll((JsonObject) value);
        keysWithDoll.add(key);
        temp.put(key, newVal);
      }
    }
    for (String key : keysWithDoll) {
      res.remove(key);
    }
    for (String key : temp.fieldNames()) {
      res.put(key, temp.getValue(key));
    }
    return res;
  }


  /**
   *Helper function to validate relation for specified geometry.
   *Currently only for bbox.
   */

  private boolean validateRelation(String geometry, String relation){
	
	if(geometry.equalsIgnoreCase("bbox") && (relation.equalsIgnoreCase("equals")
							||relation.equalsIgnoreCase("disjoint")
							|| relation.equalsIgnoreCase("touches")
							|| relation.equalsIgnoreCase("overlaps")
							|| relation.equalsIgnoreCase("crosses")
							|| relation.equalsIgnoreCase("intersects") 
							|| relation.equalsIgnoreCase("within") )){

		return true;
	}

    else if(geometry.equalsIgnoreCase("linestring") && (relation.equalsIgnoreCase("equals")
							||relation.equalsIgnoreCase("disjoint")
							|| relation.equalsIgnoreCase("touches")
							|| relation.equalsIgnoreCase("overlaps")
							|| relation.equalsIgnoreCase("crosses")
							|| relation.equalsIgnoreCase("intersects") )){

		return true;
	}
	else if(geometry.equalsIgnoreCase("polygon") && (relation.equalsIgnoreCase("equals")
							||relation.equalsIgnoreCase("disjoint")
							|| relation.equalsIgnoreCase("touches")
							|| relation.equalsIgnoreCase("overlaps")
							|| relation.equalsIgnoreCase("crosses")
							|| relation.equalsIgnoreCase("intersects")
							|| relation.equalsIgnoreCase("within") )){

		return true;
	}
    
    else
	   return false;

  }

  /**
   * Helper function to convert string values to Double
   */
  private Double getDoubleFromS(String s){
    Double d = Double.parseDouble(s);
    return d;
  }

  private JsonObject buildQuery(String geometry, JsonArray coordinates, String relation){
	
	JsonObject query = new JsonObject();

	switch(relation){
	
		case "equals": query = new JsonObject()
						.put("geoJsonLocation.coordinates",coordinates );
				break;

		case "disjoint": break;

		case "touches": query = searchGeoIntersects(geometry,coordinates);
				break;

		case "overlaps": query = searchGeoIntersects(geometry,coordinates); 
				 break;

		case "crosses": query = searchGeoIntersects(geometry,coordinates);
				break;

		case "contains": break;

		case "intersects": query = searchGeoIntersects(geometry,coordinates);
				            break;

		case "within": query = searchGeoWithin(geometry,coordinates);
				        break;

        default: break;
	}
	
    return query;
  }
  
  /**
   * Performs Mongo-GeoIntersects operation
   * */

  private JsonObject searchGeoIntersects(String geometry, JsonArray coordinates){

	JsonObject query = new JsonObject();
	
	query.put("geoJsonLocation", new JsonObject()
				.put("$geoIntersects", new JsonObject()
					.put("$geometry",new JsonObject()
						.put("type",geometry)
						.put("coordinates",coordinates))));
	System.out.println("GeoIntersects: "+query.toString());
    return query;
  }

  /**
   * Performs Mongo-GeoWithin operation
   * */

  private JsonObject searchGeoWithin(String geometry, JsonArray coordinates){
  
    JsonObject query = new JsonObject();
    query.put("geoJsonLocation", new JsonObject()
            .put("$geoWithin", new JsonObject()
                .put("$geometry",new JsonObject()
                    .put("type",geometry)
                    .put("coordinates",coordinates))));  
    System.out.println("GeoWithin: " + query.toString());
    return query;
  }

  private void mongoFind(String collection, JsonObject query, JsonObject attributeFilter, Message<Object> message) {

    attributeFilter.put("_id", 0);
    // query.put("Status", "Live");

    String[] hiddenFields = {"_tags","__uuid", "geoJsonLocation", "item-type", "__createdBy", "__instance-id"};

    FindOptions options = new FindOptions();
    options.setFields(attributeFilter);
    mongo.findWithOptions(
        collection,
        query,
        options,
        res -> {
          if (res.succeeded()) {
            // Send back the response
            JsonArray rep = new JsonArray();
            for (JsonObject j : res.result()) {
              for (String hidden : hiddenFields) {
                if (j.containsKey(hidden) && collection != "city_config") {
                  j.remove(hidden);
                }
              }
              j = addFieldsWithDOll(j);
              rep.add(j);
            }
            message.reply(rep);
          } else {
            System.out.println(res.cause());
            message.fail(0, "failure");
          }
        });
  }

  private Future<Void> mongoInsert(String collection, JsonObject insertDocument, Message<Object> message){

      Future<Void> insert_fut = Future.future();
      mongo.insert(collection, insertDocument, resp -> {
          if (resp.succeeded()) {
              if (insertDocument.containsKey("_tags")) {
                  writeTags(insertDocument.getJsonArray("_tags"));
              }
              message.reply(insertDocument.getString("id"));
              insert_fut.complete();
              logger.info("Item Created " + insertDocument.getString("id"));
          } else {
              logger.info("INSERT FAILED!!!"+resp.cause());
              message.fail(0, "failure");
              insert_fut.fail(resp.cause());
          }

      });
    return insert_fut;
  }

  @Override
  public void list(Message<Object> message) {
      JsonObject request = (JsonObject) message.body();
      System.out.println(request.containsKey("item-type"));
      System.out.println(request.containsKey("item-type-ui"));
      String host = request.getString("__instance-id");
      System.out.println(host);
      boolean ui=false;

		String key;

		if (request.containsKey("item-type")) {
			key = request.getString("item-type");
		}else if (request.containsKey("item-type-ui")){
            key=request.getString("item-type-ui");
            ui=true;
        } else {
			key = "undefined";
		}
		System.out.println("In list block : " + key);
      
      if(!ui && key.equalsIgnoreCase("resourceItem"))
      { 
    	System.out.println("Searching resourceItem");
	    JsonObject query = new JsonObject();
	    query.put("itemType", new JsonObject().put("type", "Property").put("value", "resourceItem"));
	    query.put("itemStatus", new JsonObject().put("type", "Property").put("value", "active"));
	    query.put("__instance-id", host);
    	mongoFind("catalogue",query, new JsonObject(), message);
	    
      } else if(!ui && key.equalsIgnoreCase("resourceServer"))
      { 
    	System.out.println("Searching resourceServer");
	    JsonObject query = new JsonObject();
	    query.put("itemType", new JsonObject().put("type", "Property").put("value", "resourceServer"));
	    query.put("itemStatus", new JsonObject().put("type", "Property").put("value", "active"));
    	mongoFind("catalogue",query, new JsonObject(), message);
	    
      } else if(!ui && key.equalsIgnoreCase("resourceServerGroup"))
      { 
    	System.out.println("Searching resourceServerGroup");
	    JsonObject query = new JsonObject();
	    query.put("itemType", new JsonObject().put("type", "Property").put("value", "resourceServerGroup"));
	    query.put("itemStatus", new JsonObject().put("type", "Property").put("value", "active"));
    	mongoFind("catalogue",query, new JsonObject(), message);
	    
      } else if(ui && (key.equalsIgnoreCase("tags") || key.equalsIgnoreCase("provider") || key.equalsIgnoreCase("resourceservergroup"))){
            System.out.println("In list for UI block : "+ key);
            mongo.distinctWithQuery("catalogue",key+".value", String.class.getName(),new JsonObject().put("__instance-id",host),res->{
               if(res.succeeded()){
                   System.out.println("Response Distinct: "+res.result().toString());
                   message.reply(res.result());
               }else
               {
                   message.fail(0, "Failure");
               }
            });        	
	  } else {
        message.fail(0, "Failure");
    }
  }

  
  public void getItem(Message<Object> message) {
    JsonObject request_body = (JsonObject) message.body();
    mongoFind("catalogue",request_body, new JsonObject(), message);
  }

  public void getConfig(Message<Object> message) {
    JsonObject request_body = (JsonObject) message.body();
    mongoFind("city_config",request_body, new JsonObject(), message);
  }

  public void getCities(Message<Object> message) {
	    JsonObject request_body = new JsonObject();
	    JsonObject attribute_filter = (JsonObject) message.body();
	    attribute_filter.put("__instance-id", 1);
	    attribute_filter.put("configurations.map_default_view_lat_lng", 1);
	    attribute_filter.put("configurations.smart_city_name", 1);
	    mongoFind("city_config", request_body, attribute_filter, message);
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

	private JsonObject attribute_search_query(JsonObject requestBody) {
		JsonObject query = new JsonObject();
		JsonArray expressions = new JsonArray();

		JsonArray attributeNames = extractElements(requestBody.getString("attribute-name"));
		JsonArray attributeValues = extractElements(requestBody.getString("attribute-value"));

		if (attributeNames.size() != attributeValues.size()) {
			return null;
		}

		for (int i = 0; i < attributeNames.size(); i++) {
			String key = attributeNames.getJsonArray(i).getString(0);
			JsonArray value = attributeValues.getJsonArray(i);

			if (key.equalsIgnoreCase("tags")) {
				key = "_tags";
				JsonArray tags = attributeValues.getJsonArray(i);
				value = new JsonArray();
				for (Object tag : tags) {
					value.add(((String) tag).toLowerCase());
				}
				updateNoOfHits(value);
				JsonObject q = new JsonObject();

				q.put(key, new JsonObject().put("$in", value));
				expressions.add(q);
			} else if (key.charAt(0) == '$') {
				key = "_$_" + key.substring(1);
				JsonObject q = new JsonObject();

				q.put(key, new JsonObject().put("$in", value));
				expressions.add(q);
			} else {
				JsonObject q = new JsonObject();
				JsonObject v = new JsonObject();
				v.put("type", "Property");
				v.put("value", value.getString(0));

				q.put(key, v);
				expressions.add(q);
			}
		}
		query.put("$and", expressions);

		return query;
	}

	private JsonObject attributeQuerySearch(JsonObject requestBody){

      JsonObject query = new JsonObject();
      JsonObject tagQuery = new JsonObject();
      JsonObject otherThanTagQuery = new JsonObject();
      JsonArray expressions = new JsonArray();
      JsonArray attributeNames = new JsonArray();
      JsonArray attributeValues;

      /**
       * Attribute-names = (tags,resourceGroup,provider)
       * Attribute-values = ((streetlight,flood),(aqm_bosch_climo, changebhai), (pscdcl, itms))
       *
       * CASES-
       * 1. attribute-name: {type: "Property"/"Relationship", value: attribute-value}
       * 2. attribute-name: tags, attribute-value=[.....,.....,.....]**/

        String[] attrNameSA = requestBody.getString("attribute-name").replaceAll("\\(|\\)","").split(",");
        for(String attrN:attrNameSA){
            attributeNames.add(attrN);
        }
        attributeValues = extractElements(requestBody.getString("attribute-value"));
        if(attributeNames.size()!=attributeValues.size())
            return null;
      for(int i = 0; i<attributeNames.size(); i++){
          String key = attributeNames.getString(i);
          JsonArray values = attributeValues.getJsonArray(i);
          if(key.equalsIgnoreCase("resourceServerGroup")
                    || key.equalsIgnoreCase("resourceServer")
                    || key.equalsIgnoreCase("provider")){
              for(Object value : values){
                  String prev_value = (String) value;
                  values.remove(prev_value);
                  String new_value = "urn:iudx-catalogue-pune:"+prev_value;
                  prev_value.replace(prev_value,new_value);
                  values.add(new_value);
              }
          }
          if(key.equalsIgnoreCase("tags")){
              tagQuery.put("_tags", new JsonObject().put("$in",values));
          }
          else {
              otherThanTagQuery.put(key+".value",new JsonObject().put("$in",values));
          }
      }
      
      expressions.add(tagQuery).add(otherThanTagQuery);
      query.put("$and",expressions);
      return query;
    }

    private JsonObject geo_within_search_query(JsonObject location) {
		JsonObject query = new JsonObject();
		double latitude = Double.parseDouble(location.getString("lat"));
		double longitude = Double.parseDouble(location.getString("lon"));
		double rad = (Double.parseDouble(location.getString("radius")) / (6378.1*1000)); //converting the radius (ms) to radians
		//double rad = MetersToDecimalDegrees(Double.parseDouble(location.getString("radius")), latitude);

		System.out.println(latitude + "----" + longitude + "----" + rad);

		query.put("geoJsonLocation", new JsonObject().put("$geoWithin", new JsonObject().put("$centerSphere",
				new JsonArray().add(new JsonArray().add(longitude).add(latitude)).add(rad))));
		return query;
	}

	double MetersToDecimalDegrees(double meters, double latitude) {
		return meters / (111.32 * 1000 * Math.cos(latitude * (Math.PI / 180)));
	}

  private JsonObject geoSearchQuery(JsonObject requestBody){
    JsonObject query = new JsonObject();
    String geometry="", relation="", coordinatesS="";
    String[] coordinatesArr;
    JsonArray coordinates= new JsonArray();
    
     //Polygon or LineString
    if(requestBody.containsKey("geometry")){
        if(requestBody.getString("geometry").toUpperCase().contains("Polygon".toUpperCase()))
            geometry = "Polygon";
        else if(requestBody.getString("geometry").toUpperCase().contains("lineString".toUpperCase()))
            geometry = "LineString";
    }
    //bounding-box
    else if(requestBody.containsKey("bbox")){
        geometry = "bbox";
    }

    //relation defaults to 'Intersects'
    relation = requestBody.containsKey("relation")?requestBody.getString("relation").toLowerCase():"intersects";
    boolean valid = validateRelation(geometry, relation);
	if(valid){
        switch(geometry){

            case "bbox": 
                coordinatesS = requestBody.getString("bbox");
                coordinatesArr = coordinatesS.split(",");
                JsonArray temp = new JsonArray();
                JsonArray y1x1 = new JsonArray().add(getDoubleFromS(coordinatesArr[1])).add(getDoubleFromS(coordinatesArr[0]));
                JsonArray y1x2 = new JsonArray().add(getDoubleFromS(coordinatesArr[1])).add(getDoubleFromS(coordinatesArr[2]));
                JsonArray y2x2 = new JsonArray().add(getDoubleFromS(coordinatesArr[3])).add(getDoubleFromS(coordinatesArr[2]));
                JsonArray y2x1 = new JsonArray().add(getDoubleFromS(coordinatesArr[3])).add(getDoubleFromS(coordinatesArr[0]));
                temp.add(y1x1).add(y1x2).add(y2x2).add(y2x1).add(y1x1);
                coordinates.add(temp);
                query = buildQuery("Polygon",coordinates,relation);
                break;

            case "Polygon": 
                coordinatesS = requestBody.getString("geometry");
                coordinatesS = coordinatesS.replaceAll("[a-zA-Z()]","");
                coordinatesArr = coordinatesS.split(",");
                JsonArray extRing = new JsonArray();
                for (int i = 0 ; i<coordinatesArr.length;i+=2){
                    JsonArray points = new JsonArray();
                    points.add(getDoubleFromS(coordinatesArr[i+1])).add(getDoubleFromS(coordinatesArr[i]));
                    extRing.add(points);
                }
                coordinates.add(extRing);
                System.out.println("QUERY: " + coordinates.toString());
                query = buildQuery(geometry,coordinates,relation);
                break;
            
            case "LineString":  
                coordinatesS = requestBody.getString("geometry");
                coordinatesS = coordinatesS.replaceAll("[a-zA-Z()]","");
                coordinatesArr = coordinatesS.split(",");
                for (int i = 0 ; i<coordinatesArr.length;i+=2){
                    JsonArray points = new JsonArray();
                    points.add(getDoubleFromS(coordinatesArr[i+1])).add(getDoubleFromS(coordinatesArr[i]));
                    coordinates.add(points);
                }
                query = buildQuery(geometry,coordinates,relation);
                break;
            
            default: query=null;     
        }
    } else
        query=null;
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
    JsonObject attribute_query = new JsonObject();
    JsonObject geo_query = new JsonObject();
    JsonObject query = new JsonObject();
    JsonArray expressions = new JsonArray();
    System.out.println(requestBody);

		if (requestBody.containsKey("attribute-name") && requestBody.containsKey("attribute-value")
				&& !requestBody.containsKey("lat") && !requestBody.containsKey("lon")
				&& !requestBody.containsKey("radius")
                && !requestBody.containsKey("bbox")
                && !requestBody.containsKey("geometry")) {
		System.out.println("ATTRIBUTE Query");
        attribute_query = attributeQuerySearch(requestBody);
		query = attribute_query;
		
		} else if (requestBody.containsKey("lat") && requestBody.containsKey("lon") && requestBody.containsKey("radius")
				&& ! requestBody.containsKey("attribute-name") && ! requestBody.containsKey("attribute-value")) { 
		System.out.println("GEO-SPATIAL Query (CENTRE PT)");
		geo_query = geo_within_search_query(requestBody);
		query = geo_query;
		} else if (requestBody.containsKey("bbox")	&& ! requestBody.containsKey("attribute-name") && ! requestBody.containsKey("attribute-value")){
            System.out.println("GEO-SPATIAL Query (BBOX)");
            query=geoSearchQuery(requestBody);

        } else if(requestBody.containsKey("geometry") && ! requestBody.containsKey("attribute-name") && ! requestBody.containsKey("attribute-value")){
            System.out.println("GEO-SPATIAL Query (POLYGON/LINESTRING)");
            query=geoSearchQuery(requestBody);

        } else if (requestBody.containsKey("lat") && requestBody.containsKey("lon") && requestBody.containsKey("radius")
				&& requestBody.containsKey("attribute-name") && requestBody.containsKey("attribute-value")) {
			System.out.println("GEO-SPATIAL with ATTRIBUTE Query");
			attribute_query = attributeQuerySearch(requestBody);
			expressions.add(attribute_query);
			geo_query = geo_within_search_query(requestBody);
			expressions.add(geo_query);
			query.put("$and", expressions);
		} else if(requestBody.containsKey("bbox") || requestBody.containsKey("geometry") && requestBody.containsKey("attribute-name") && requestBody.containsKey("attribute-value")){
            System.out.println("GEO-SPATIAL (BBOX/POLYGON/LINESTRING) WITH ATTRIBUTE QUERY");
			attribute_query = attributeQuerySearch(requestBody);
			expressions.add(attribute_query);
			geo_query = geoSearchQuery(requestBody);
			expressions.add(geo_query);
			query.put("$and", expressions);
		} else if (requestBody.containsKey("attribute-name")
            && !requestBody.containsKey("attribute-value")) {
            query = null;
        }else if (!requestBody.containsKey("attribute-name")
            && requestBody.containsKey("attribute-value")) {
            query = null;
		} else {
      query = new JsonObject();
    }
    System.out.println("Final Query: "+ query.toString());
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
    
    String key = request_body.getString("item-type");
    System.out.println("In search block : "+ key);
    if(key.equalsIgnoreCase("resourceItem"))
    { 
    	System.out.println("Searching resourceItem");
	    query.put("itemType", new JsonObject().put("type", "Property").put("value", "resourceItem"));
	    query.put("itemStatus", new JsonObject().put("type", "Property").put("value", "active"));
  	} 
    
    else if(key.equalsIgnoreCase("resourceServer"))
    { 
    	System.out.println("Searching resourceServer");
	    query.put("itemType", new JsonObject().put("type", "Property").put("value", "resourceServer"));
	    query.put("itemStatus", new JsonObject().put("type", "Property").put("value", "active"));
  	} 
    
    else if(key.equalsIgnoreCase("resourceServerGroup"))
    { 
    	System.out.println("Searching resourceServerGroup");
	    query.put("itemType", new JsonObject().put("type", "Property").put("value", "resourceServerGroup"));
	    query.put("itemStatus", new JsonObject().put("type", "Property").put("value", "active"));
  	}

    query.put("__instance-id", request_body.getString("__instance-id"));
    
    if (query == null) {
      message.fail(0, "Bad query: Number of attributes is not equal to number of number of values");
    } else {
      mongoFind("catalogue",query, fields, message);
    }
  }

  @Override
  public void count(Message<Object> message) { // TODO Auto-generated method stub
    JsonObject request_body = (JsonObject) message.body();
    JsonObject query = decodeQuery(request_body);

    String key = request_body.getString("item-type");
    System.out.println("In count block : "+ key);
    if(key.equalsIgnoreCase("resourceItem"))
    { 
    	System.out.println("Searching resourceItem");
	    query.put("itemType", new JsonObject().put("type", "Property").put("value", "resourceItem"));
	    query.put("itemStatus", new JsonObject().put("type", "Property").put("value", "active"));
  	} 
    
    else if(key.equalsIgnoreCase("resourceServer"))
    { 
    	System.out.println("Searching resourceServer");
	    query.put("itemType", new JsonObject().put("type", "Property").put("value", "resourceServer"));
	    query.put("itemStatus", new JsonObject().put("type", "Property").put("value", "active"));
  	} 
    
    else if(key.equalsIgnoreCase("resourceServerGroup"))
    { 
    	System.out.println("Searching resourceServerGroup");
	    query.put("itemType", new JsonObject().put("type", "Property").put("value", "resourceServerGroup"));
	    query.put("itemStatus", new JsonObject().put("type", "Property").put("value", "active"));
  	}

    query.put("__instance-id", request_body.getString("__instance-id"));
    
    if (query == null) {
      message.fail(0, "Bad query: Number of attributes is not equal to number of number of values");
    } else {
      mongo.count(
          "catalogue",
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
      JsonObject geometry;
      JsonArray geometry_array;
      String id, sha1, sha, resourceServer = "", resourceServerGroup = "", provider, resourceId, geometry_type, longitude, latitude, domain, region = null, role;
      String[] onboardedBy;

      sha1 = updated.getString("role");
      onboardedBy = sha1.split("/");
      sha = onboardedBy[1];
      domain = onboardedBy[0];
      /**
       * This is temporary code, needs better optimization
       * Starts now......
       * */
      if (updated.getString("item-type").equalsIgnoreCase("resourceItem")) {
          resourceServerGroup = updated.getJsonObject("resourceServerGroup").getString("value").split(":")[2];
          resourceId = updated.getJsonObject("resourceId").getString("value");
          id = domain + "/" + sha + "/" + resourceServerGroup + "/" + resourceId;
          updated.put("id", id);
      }

      if (updated.getString("item-type").equalsIgnoreCase("resourceServer")) {
          id = updated.getString("name");
          updated.put("id", id);
      }

      if (updated.getString("item-type").equalsIgnoreCase("resourceServerGroup")) {
          id = updated.getJsonObject("resourceServer").getString("value").split(":")[2] + "/"
                  + updated.getString("name");
          updated.put("id", id);
      }
      provider = updated.containsKey("provider") ? updated.getJsonObject("provider").getString("value"): null;
      if(provider!=null)
          updated.put("providerId",provider);

    if(updated.getString("item-type").equalsIgnoreCase("resourceItem")
        || updated.getString("item-type").equalsIgnoreCase("resourceServer")){
          if (updated.containsKey("location")) {
              region = "location";
          } else if (updated.containsKey("coverageRegion")) {
              region = "coverageRegion";
          }

          geometry = updated.getJsonObject(region).getJsonObject("value").getJsonObject("geometry");
          geometry_type = updated.getJsonObject(region).getJsonObject("value").getJsonObject("geometry")
                  .getString("type");
          geometry_array = updated.getJsonObject(region).getJsonObject("value").getJsonObject("geometry")
                  .getJsonArray("coordinates");

          logger.info(geometry.toString());
          logger.info(geometry_type.toString());
          logger.info(geometry_array.toString());

          if (geometry_type == "Point") {
              latitude = geometry_array.getString(0);
              longitude = geometry_array.getString(1);
          } else if (geometry_type == "Polygon") {
              latitude = geometry_array.getString(0);
              longitude = geometry_array.getString(1);
          }

        updated.put("geoJsonLocation", geometry);
    }
      /**
       * .....ends here
       * */
    updated.remove("sha_1_id");
    updated.remove("role");
	updated.put("createdAt", new JsonObject().put("type", "TimeProperty").put("value", new java.util.Date().toString()));
	updated.put("updatedAt", new JsonObject().put("type", "TimeProperty").put("value", new java.util.Date().toString()));
	
    if (addId) {
      updated.put("__uuid", UUID.randomUUID().toString());
    }
    if (bulkId != null) {
      updated.put("bulk-id", bulkId);
    }

    if (updated.containsKey("tags")) {
      JsonArray tagsInLowerCase = new JsonArray();
      JsonArray tags = updated.getJsonObject("tags").getJsonArray("value");      

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
                mongo.bulkWrite(TAG_COLLECTION, bulk, tagsUpdated -> {
                });
            }
          }
          else{
          }
        });
  }

  private JsonObject removeDollar(JsonObject item) {
    Set<String> keysWithDol = new HashSet<String>();
    JsonObject temp = new JsonObject();

    for (String key : item.fieldNames()) {
      Object value = item.getValue(key);
      if (key.charAt(0) == '$') {
        keysWithDol.add(key);
        key = "_$_" + key.substring(1);
        temp.put(key, value);
      }
      if (value.getClass() == JsonObject.class) {
        JsonObject newVal = removeDollar((JsonObject) value);
        keysWithDol.add(key);
        temp.put(key, newVal);
      }
    }
    for (String key : keysWithDol) {
      item.remove(key);
    }
    for (String key : temp.fieldNames()) {
      item.put(key, temp.getValue(key));
    }
    return item;
  }

  @Override
  public void create(Message<Object> message) {

      JsonObject request_body = (JsonObject) message.body();
      JsonObject itemWithoutDol = removeDollar(request_body);

      String item_type = request_body.getString("item-type");
      String item_id = null;
      String resourceGroup= null, resourceServer = null, provider=null;
      Future<Void> insertHashFuture = Future.future();
      System.out.println("In create : " + request_body);
      if (item_type.equalsIgnoreCase("resourceItem")) {
          request_body = addNewAttributes(itemWithoutDol, 1, true, null);
          item_id = request_body.getString("id");
          resourceGroup = request_body.getJsonObject("resourceServerGroup").getString("value").split(":")[2];
          resourceServer = request_body.getJsonObject("resourceServer").getString("value").split(":")[2];
          logger.info("Item ID : " + item_id);
          // resourceCache.remove("resourceItemIds", item_id);
          if (!resourceCache.get("resourceItemIds").contains(item_id))
              if (resourceCache.get("resourceServerIds").contains(resourceServer) &&
                      resourceCache.get("resourceGroupIds").contains(resourceGroup))
                  insertHashFuture = mongoInsert("catalogue",request_body, message);
              else
                  message.fail(0,"Resource Server and/or Resource Server Group not found!");
          else
              message.reply("conflict");

      } else if (item_type.equalsIgnoreCase("resourceServer")) {
          request_body = addNewAttributes(itemWithoutDol, 1, true, null);
          item_id = request_body.getString("id");

          if (!resourceCache.get("resourceServerIds").contains(item_id)){
              logger.info("CREATE RESOURCE SERVER");
             insertHashFuture= mongoInsert("catalogue", request_body, message);
          }
          else
              message.reply("conflict");
      } else if (item_type.equalsIgnoreCase("resourceServerGroup")) {
          request_body = addNewAttributes(itemWithoutDol, 1, true, null);
          item_id = request_body.getString("id");
          resourceServer = request_body.getJsonObject("resourceServer").getString("value").split(":")[2];
          if (!resourceCache.get("resourceGroupIds").contains(item_id)) {
              if (resourceCache.get("resourceServerIds").contains(resourceServer)) {
                  logger.info("CREATE RESOURCE GROUP");
                  insertHashFuture = mongoInsert("catalogue",request_body, message);
              } else
                  message.fail(0,"Resource Server is not Found!");
          }else
          message.reply("conflict");
      }

      String finalItem_id = item_id;

      JsonObject finalRequest_body = request_body;
      insertHashFuture.setHandler(res->{
          if(res.succeeded()) {
              if (item_type.equalsIgnoreCase("resourceItem"))
                  resourceCache.get("resourceItemIds").add(finalItem_id);
              else if (item_type.equalsIgnoreCase("resourceServer")) {
                  resourceCache.get("resourceServerIds").add(finalItem_id);
              }
              else if (item_type.equalsIgnoreCase("resourceServerGroup")) {
                  resourceCache.get("resourceGroupIds").add(finalItem_id);
              }
              if(finalRequest_body.containsKey("providerId")
                      && !resourceCache.get("providerIds").contains(finalRequest_body.getString("providerId"))){
                  resourceCache.get("providerIds").add(finalRequest_body.getString("providerId"));
              }
              resourceCache.entrySet().forEach(entry ->{
                  System.out.println("Added to HaspMap after insert\n===="+entry.getKey()+": "+entry.getValue());
              });
          }else{
              logger.info("HashMap insert failed. "+res.cause());
          }
      });
  }
//			JsonObject q1 = new JsonObject();
//			JsonObject q2 = new JsonObject();
//            q1.put("id", resourceServer);
//            q2.put("id",resourceGroup);
//            JsonObject fields = new JsonObject().put("id",1);
//            FindOptions options = new FindOptions().setFields(fields).setLimit(1);
//
//            //check in the hash map if item-id is already present
//            //add code here....
//
//            JsonObject finalRequest_body = request_body;
//            mongo.findWithOptions(COLLECTION,q1,options, res->{
//                if(res.succeeded() && !res.result().isEmpty()){
//                    mongo.findWithOptions(COLLECTION,q2,options,res2->{
//                       if(res2.succeeded() && !res2.result().isEmpty()){
//                           //call the insert function
//                            mongoInsert(finalRequest_body,message);
//                       }else {
//                           //throw an error
//                       }
//
//                    });
//                } else{
//                        //throw an error
//                }
//            });

//		    JsonObject q1 = new JsonObject();
//		    q1.put("id",resourceServer);
//		    JsonObject field = new JsonObject().put("id",1);
//		    FindOptions options = new FindOptions().setFields(field).setLimit(1);
//
//		    //check if the item is already present in HashMap
//            //add code here....
//
//            JsonObject finalRequest_body1 = request_body;
//            mongo.findWithOptions(COLLECTION, q1,options, res -> {
//                if(res.succeeded() && !res.result().isEmpty()){
//                    //call the insert method
//                    mongoInsert(finalRequest_body1,message);
//                }else {
//                    //throw an error
//                }
//            });
//
//        }
//		q.put("id", item_id);
//		q.put("item-type", item_type);
//		expressions.add(q);
//		query.put("$and", expressions);

		// JsonObject updated_item = request_body;

//		mongo.count(COLLECTION, query, res -> {
//			logger.info(res.result().toString());
//
//			if (res.result().intValue() == 0) {
//
//			} else {
//				message.reply("conflict");
//			}
//		});
 
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
    query.put("itemType", new JsonObject().put("type", "Property").put("value", itemType));
    // Get its version and tags
    JsonObject fields = new JsonObject();
    // fields.put("Version", 1);
    fields.put("_tags", 1);
    FindOptions options = new FindOptions().setFields(fields);
    
    mongo.findWithOptions(
        "catalogue",
        query,
        options,
        res -> {
          if (res.succeeded()) {
            int version;
            if (res.result().isEmpty()) {
              System.out.println("Does not exist");
              message.fail(0, "Error: The item with id: " + id + " does not exist.");
            } else {
            	System.out.println("HIT !");
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
                  "catalogue",
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
                          "catalogue",
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
    String item_id = request_body.getString("id");
    String item_type = request_body.getString("item_type");
    // Populate query
    JsonObject query = new JsonObject();
    query.put("id", request_body.getString("id"));
    
    mongo.findOneAndDelete(
        "catalogue",
        query,
        res -> {
          if (res.succeeded() && !(res.result() == null)) {
        	  JsonArray resourceItem_dataArray = resourceCache.get("resourceItemIds"); 
        	  JsonArray resourceServer_dataArray = resourceCache.get("resourceServerIds");
        	  JsonArray resourceServerGroup_dataArray = resourceCache.get("resourceServerGroupIds");
              if (item_type.equalsIgnoreCase("resourceItem"))
                  resourceCache.remove("resourceItemIds", resourceItem_dataArray.remove(item_id)); 
              else if (item_type.equalsIgnoreCase("resourceServer")) {
                  resourceCache.remove("resourceItemIds", resourceServer_dataArray.remove(item_id));              }
              else if (item_type.equalsIgnoreCase("resourceServerGroup")) {
                  resourceCache.remove("resourceServerGroupIds", resourceServerGroup_dataArray.remove(item_id));              }
              System.out.println(resourceCache.toString());
            if (res.result().containsKey("_tags")) {
              deleteTags(res.result().getJsonArray("_tags"));
            }
            System.out.println(resourceCache.toString());
            message.reply("Success");
          } else if (res.result() == null) {
            message.fail(0, "Item not found");
          } else {
            message.fail(0, "Failure");
          }
        });
  }

  @Override
  public void bulkCreate(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject request_body = (JsonObject) message.body();
    JsonArray items = request_body.getJsonArray("items");
    String bulkId = request_body.getString("bulk-id");
    JsonArray itemIds = new JsonArray();
    List<BulkOperation> bulk_create = new ArrayList<BulkOperation>();
    for (int i = 0; i < items.size(); i++) {
      JsonObject item = items.getJsonObject(i);
      item = removeDollar(item);
      JsonObject itemWithAttr = addNewAttributes(item, 1, true, bulkId);
      itemIds.add(itemWithAttr.getString("id"));
      if (itemWithAttr.containsKey("_tags")) {
        writeTags(itemWithAttr.getJsonArray("_tags"));
      }
      bulk_create.add(BulkOperation.createInsert(itemWithAttr));
    }
    if (!bulk_create.isEmpty()) {
      mongo.bulkWrite(
          "catalogue",
          bulk_create,
          bulkWrite -> {
            if (bulkWrite.succeeded()) {
              JsonObject reply = new JsonObject();
              reply.put("bulk-id", bulkId);
              reply.put("items", itemIds);
              message.reply(reply);
            } else {
              message.fail(0, "Failure");
            }
          });
    }
  }

  @Override
  public void bulkDelete(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject request_body = (JsonObject) message.body();
    String bulkId = request_body.getString("bulk-id");
    JsonObject query = new JsonObject();
    query.put("bulk-id", bulkId);
    query.put("item-type", "resource-item");
    mongo.find(
        "catalogue",
        query,
        documents -> {
          if (documents.succeeded()) {
            List<JsonObject> items = documents.result();
            if (items.size() == 0) {
              message.fail(0, "No such bulk-id");
            } else {
              for (JsonObject item : items) {
                if (item.containsKey("_tags")) {
                  JsonArray tags = item.getJsonArray("_tags");
                  deleteTags(tags);
                }
              }
              mongo.removeDocuments(
                  "catalogue",
                  query,
                  deleteItems -> {
                    if (deleteItems.succeeded()) {
                      JsonObject reply = new JsonObject();
                      reply.put("bulk-id", bulkId);
                      reply.put("status", "success");
                      message.reply(reply);
                    } else {
                      message.fail(0, "Failure");
                    }
                  });
            }
          } else {
            message.fail(0, "Failure");
          }
        });
  }

  @Override
  public void bulkUpdate(Message<Object> message) {
    // TODO Auto-generated method stub
    JsonObject requestBody = (JsonObject) message.body();
    String bulkId = requestBody.getString("bulk-id");
    JsonObject query = new JsonObject();
    query.put("item-type", "resource-item");
    query.put("bulk-id", bulkId);
    mongo.find(
        "catalogue",
        query,
        itemsToUpdate -> {
          if (itemsToUpdate.succeeded()) {
            if (itemsToUpdate.result().size() != 0) {

              boolean tagsPresent = false;
              if (requestBody.containsKey("tags")) {
                System.out.println("Yes");
                tagsPresent = true;
              }
              if (tagsPresent) {
                JsonArray tagsInLowerCase = new JsonArray();
                JsonArray tags = requestBody.getJsonArray("tags");

                for (Object i : tags) {
                  tagsInLowerCase.add(((String) i).toLowerCase());
                }
                requestBody.put("_tags", tagsInLowerCase);
                for (JsonObject item : itemsToUpdate.result()) {
                  if (item.containsKey("_tags")) {
                    JsonArray oldTags = item.getJsonArray("_tags");
                    updateTags(oldTags, tagsInLowerCase);
                  } else {
                    writeTags(tagsInLowerCase);
                  }
                }
              }
              requestBody.put("Last modified on", new java.util.Date().toString());
              JsonObject update = new JsonObject().put("$set", requestBody);
              UpdateOptions options = new UpdateOptions();
              options.setMulti(true);
              mongo.updateCollectionWithOptions(
                  "catalogue",
                  query,
                  update,
                  options,
                  updateResult -> {
                    if (updateResult.succeeded()) {
                      System.out.println("Should have worked");
                      JsonObject reply = new JsonObject();
                      reply.put("bulk-id", bulkId);
                      reply.put("status", "Success");
                      message.reply(reply);
                    } else {
                      message.fail(0, "Failure");
                    }
                  });

            } else {
              message.fail(0, "No such bulk-id");
            }
          } else {
            message.fail(0, "Failure");
          }
        });
  }
}
