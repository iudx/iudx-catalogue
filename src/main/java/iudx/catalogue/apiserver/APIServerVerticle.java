package iudx.catalogue.apiserver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.logging.Logger;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Properties;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_1;

public class APIServerVerticle extends AbstractVerticle {

  private static final Logger logger = Logger.getLogger(APIServerVerticle.class.getName());

  static final int HTTP_STATUS_OK = 200;
  static final int HTTP_STATUS_CREATED = 201;
  static final int HTTP_STATUS_DELETED = 204;
  static final int HTTP_STATUS_BAD_REQUEST = 400;
  static final int HTTP_STATUS_NOT_FOUND = 404;
  static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;
  static final int HTTP_STATUS_UNAUTHORIZED = 401;
  static final int HTTP_STATUS_CONFLICT = 409;
  static String emailID_SHA_1, onboardedBy, onboarder;
  private ArrayList<String> itemTypes;

  @Override
  public void start(Future<Void> startFuture) {

    populateItemTypes();

    Router router = defineApiRouting();

    setSystemProps();

    HttpServer server = createServer();

    int port = config().getInteger("http.port", 18443);

    server.requestHandler(router::accept).listen(port);

    logger.info("API Server Verticle started!");

    startFuture.complete();
  }

  private void populateItemTypes() {
    itemTypes = new ArrayList<String>();
    itemTypes.add("resourceItem");
    itemTypes.add("data-model");
    itemTypes.add("access-object");
    itemTypes.add("resourceServer");
    itemTypes.add("resourceServerGroup");
    itemTypes.add("provider");
    itemTypes.add("base-schema");
    itemTypes.add("catalogue-item");
  }

  private HttpServer createServer() {
    ClientAuth clientAuth = ClientAuth.REQUEST;
    String keystore = config().getString("keystore");
    String keystorePassword = config().getString("keystorePassword");

    HttpServer server =
        vertx.createHttpServer(
            new HttpServerOptions()
                .setSsl(true)
                .setClientAuth(clientAuth)
                .setTrustStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword))
                .setKeyStoreOptions(
                    new JksOptions().setPath(keystore).setPassword(keystorePassword)));
    return server;
  }

  private Router defineApiRouting() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // IUDX v1 APIs
    
    // Create an item
    router.post("/catalogue/v1/items").handler(this::create);
    
    // Get item with ID
    router.get("/catalogue/v1/items/:domain/:provider/:resourceServer/:resourceCatogery/:resourceId").handler(this::getItem);
    router.get("/catalogue/v1/items/:resourceServer/:resourceCatogery").handler(this::getItem);
    router.get("/catalogue/v1/items/:resourceServer").handler(this::getItem);
    
    
    router.put("/catalogue/v1/items/:provider/:resourceServer/:resourceCatogery/:resourceId").handler(this::update);
    
    // Delete item with ID
    router.delete("/catalogue/v1/items/:domain/:provider/:resourceServer/:resourceCatogery/:resourceId").handler(this::delete);
    router.delete("/catalogue/v1/items/:resourceServer/:resourceCatogery").handler(this::delete);
    router.delete("/catalogue/v1/items/:resourceServer").handler(this::delete);
    
    // Search items in Catalogue
    router.get("/catalogue/v1/search").handler(this::searchAttribute);
    
    // Count items in Catalogue
    router.get("/catalogue/v1/count").handler(this::count);
    
    // NEW APIs
    router.get("/catalogue/internal_apis/list/:itemtype").handler(this::list);
    router.get("/search/catalogue/attribute").handler(this::searchAttribute);
    router.post("/create/catalogue/:itemtype").handler(this::create);
    router.put("/update/catalogue/:itemtype/:id").handler(this::update);
    router.delete("/remove/catalogue/:itemtype/:id").handler(this::delete);

    router.post("/create/catalogue/resource-item/bulk/:bulkId").handler(this::bulkCreate);
    router.patch("/update/catalogue/resource-item/bulk/:bulkId").handler(this::bulkUpdate);
    router.delete("/remove/catalogue/resource-item/bulk/:bulkId").handler(this::bulkDelete);

    router
    .route("/")
    .handler(
        routingContext -> {
          HttpServerResponse response = routingContext.response();
          response.sendFile("ui/pages/list/index.html");
        });

    router
        .route("/map")
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("ui/pages/map/index.html");
            });

    //router.route("/*").handler(StaticHandler.create("ui/pages"));
    router.route("/assets/*").handler(StaticHandler.create("ui/assets"));
    return router;
  }

  private void setSystemProps() {
    String keystore = config().getString("keystore");
    String keystorePassword = config().getString("keystorePassword");

    String truststore = config().getString("truststore");
    String truststorePassword = config().getString("truststorePassword");

    Properties systemProps = System.getProperties();

    systemProps.put("javax.net.ssl.keyStore", keystore);
    systemProps.put("javax.net.ssl.keyStorePassword", keystorePassword);

    systemProps.put("javax.net.ssl.trustStore", truststore);
    systemProps.put("javax.net.ssl.trustStorePassword", truststorePassword);

    System.setProperties(systemProps);
    logger.info("IUDX TLS Property Defined !");
  }

  /**
   * Checks if the user has necessary permission to write or delete from the database
   *
   * @param event The server request
   * @param path The URL to which the request is sent
   * @param file_path The path of the file which contains the list of users and their permissions
   * @return
   */
  private boolean authenticateRequest(RoutingContext routingContext, String file_path) {

    HttpServerRequest request = routingContext.request();
    boolean allowed = false;
    String authorization = request.getHeader("authorization");

    if (authorization != null) {
      final String userId;
      final String password;
      final String scheme;

      try {
        String[] parts = authorization.split(" ");
        scheme = parts[0];
        String[] credentials = new String(Base64.getDecoder().decode(parts[1])).split(":");
        userId = credentials[0];
        // when the header is: "user:"
        password = credentials.length > 1 ? credentials[1] : null;

        if (!"Basic".equals(scheme)) {

          handle401(routingContext, "Use Basic HTTP authorization");
        } else {
          if (userId != null && password != null) {
            try (InputStream inputStream = new FileInputStream(file_path)) {
              JSONObject users = new JSONObject(new JSONTokener(inputStream));
              JSONObject user;
              if (users.has(userId)) {
                user = users.getJSONObject(userId);
                if (password.equals(user.getString("password"))) {
                  allowed = true;
                } else {
                  handle400(routingContext, "Your password is invalid");
                }

                if (allowed && !user.getBoolean("write_permission")) {
                  allowed = false;
                  handle401(routingContext, "You do not have write access to the server");
                }
              } else {
                handle400(routingContext, "User " + userId + "is not registered");
              }

            } catch (Exception e) {
              handle500(routingContext);
            }

          } else {
            handle400(routingContext, "Add 'authenticaton' in the header of your request");
          }
        }
      } catch (Exception e) {
        handle401(routingContext, "Use Basic HTTP authentication");
      }

    } else {
      handle401(routingContext, "Use Basic HTTP authorization");
    }

    logger.info("Authentication ended with flag : " + allowed);
    return allowed;
  }

	private boolean decodeCertificate(RoutingContext routingContext) {

		boolean status = false;

		try {
			Principal cn = routingContext.request().connection().sslSession().getPeerPrincipal();

			String certificate_class[] = cn.toString().split(",");
			System.out.println(cn.toString());
			String class_level = certificate_class[0];
			String email_id = certificate_class[8];
			onboarder = certificate_class[1];
			String designation = onboarder.split("=")[1];
			System.out.println(designation);

			String[] oid_class = class_level.split("=");
			String level = oid_class[1];

			String[] email = email_id.split("=");
			String[] emailID = email[1].split("@");
			String userName = emailID[0];
			String domain = emailID[1];

			emailID_SHA_1  = new DigestUtils(SHA_1).digestAsHex(email[1]);
			logger.info("email in SHA-1 is " + emailID_SHA_1);
			onboarder = designation + " at " + domain;
			onboardedBy = domain + "/" + emailID_SHA_1;
			logger.info("emailID in SHA-1 is " + emailID_SHA_1);
			logger.info("onBoarder is " + onboarder);
			logger.info("onBoardedBy is " + onboardedBy);

			if ((level.contains("class:3") || level.contains("class:4")
					|| level.contains("class:5")) && onboardedBy.equalsIgnoreCase("rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531") ) {
				status = true;
				logger.info("Valid Certificate");
			} else {
				status = false;
				logger.info("Invalid Certificate");
			}

		} catch (SSLPeerUnverifiedException e) {
			status = false;
		}

		return status;
	}

	private void getItem(RoutingContext routingContext) { // "/catalogue/v1/items/:domain/:provider/:resourceServer/:resourceCatogery/:resourceId

		System.out.println("HIT : In Get Item");
		String id = null;
		int state = routingContext.pathParams().size();

		if (state == 5) {
			id = routingContext.pathParam("domain") + "/" + routingContext.pathParam("provider") + "/"
					+ routingContext.pathParam("resourceServer") + "/" + routingContext.pathParam("resourceCatogery")
					+ "/" + routingContext.pathParam("resourceId");
			System.out.println(id);
		} else if (state == 2) {
			id = routingContext.pathParam("resourceServer") + "/" + routingContext.pathParam("resourceCatogery");
		}

		else if (state == 1) {
			id = routingContext.pathParam("resourceServer");
		}
		System.out.println(id);
		JsonObject request_body = new JsonObject();
		request_body.put("id", id);
		databaseHandler("getItem", routingContext, request_body);
	}

  private void list(RoutingContext routingContext) {
    String currentType = routingContext.request().getParam("itemtype");

    /**
     * New code-
     * Added as a part to provide List of Tags, Provider, ResourceServerGroup APIs for UI
     **/
    JsonObject requestBody = new JsonObject();
    if(currentType.equalsIgnoreCase("tags")){
        requestBody.put("item-type-ui","tags");
        databaseHandler("list",routingContext,requestBody);
    } else if (currentType.equalsIgnoreCase("provider")){
        requestBody.put("item-type-ui","provider");
        databaseHandler("list",routingContext,requestBody);
    }else if (currentType.equalsIgnoreCase("resourceservergroup")){
          requestBody.put("item-type-ui","resourceServerGroup");
          databaseHandler("list",routingContext,requestBody);
    }
    /**-----------------------------ends here-----------------------------------**/

    else if(currentType.equals("item-types")) {
      JsonArray allTypes = new JsonArray(itemTypes);
      JsonObject reply = new JsonObject().put("item-types", allTypes);
      handle200(routingContext, reply);
    } else if (currentType.equals("tags")) {
      JsonObject request_body = new JsonObject();
      databaseHandler("get-tags", routingContext, request_body);
    } else if (itemTypes.contains(currentType)) {
      JsonObject request_body = new JsonObject();
      request_body.put("item-type", currentType);
      databaseHandler("list", routingContext, request_body);
    } else {
      handle400(routingContext, currentType + " does not exist in the catalogue. ");
    }
  }

  /**
   * Sends a request to ValidatorVerticle to validate the item and DatabaseVerticle to insert it in
   * the database. Displays the id of the inserted item.
   *
   * @param event The server request which contains the item to be inserted in the database and the
   *     skip_validation header.
   */
  private void create(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    String query_params = request.query();
    String itemType = query_params.split("=")[1];

			String skip_validation = "false";
			if (request.headers().contains("skip_validation")) {
				skip_validation = request.getHeader("skip_validation").toLowerCase();
			}

			if (decodeCertificate(routingContext)) {
				if (authenticateRequest(routingContext, "user.list")) {
					try {
						JsonObject request_body = routingContext.getBodyAsJson();
						request_body.put("onboardedBy", onboarder);
						request_body.put("role", onboardedBy);
						request_body.put("item-type", itemType);
						request_body.put("__createdBy",onboardedBy);
						DeliveryOptions validator_action = new DeliveryOptions();
						validator_action.addHeader("action", "validate-item");

						if (skip_validation != null) {
							if (!("true".equals(skip_validation)) && !("false".equals(skip_validation))) {
								handle400(routingContext, "Invalid value: skip_validation is not a boolean");
								return;
							} else {
								validator_action.addHeader("skip_validation", skip_validation);
							}
						}
						vertx.eventBus().send("validator", request_body, validator_action, validator_reply -> {
							if (validator_reply.succeeded()) {
								if (itemTypes.contains(itemType)) {
									databaseHandler("create", routingContext, request_body);
								} else {
									handle400(routingContext, "No such item-type exists");
								}
							} else {
								handle500(routingContext);
							}
						});

					} catch (Exception e) {
						handle400(routingContext, "Invalid item: Not a Json Object");
					}
				} else {
					handle401(routingContext, "Unauthorised");
				}
			} else {
				handle400(routingContext, "Certificate 'authenticaton' error");
			}

	 }

  private void bulkCreate(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();

    if (decodeCertificate(routingContext)) {
      if (authenticateRequest(routingContext, "user.list")) {
        try {
          String bulkId = request.getParam("bulkId");
          JsonArray request_body = routingContext.getBodyAsJsonArray();
          JsonObject request_json_object = new JsonObject();
          request_json_object.put("items", request_body);
          request_json_object.put("bulk-id", bulkId);

          databaseHandler("bulkcreate", routingContext, request_json_object);

        } catch (Exception e) {
          handle400(routingContext, "Invalid item: Not a Json Object");
        }
      } else {
        handle401(routingContext, "Unauthorised");
      }
    } else {
      handle400(routingContext, "Certificate 'authenticaton' error");
    }
  }

  private JsonObject prepareQuery(String query) {
    JsonObject request_body = new JsonObject();
    if (!query.equals("") && query != null) {
      String[] queryParams = query.split("\\&");
      int queryLen = queryParams.length;

      for (int i = 0; i < queryLen; i++) {
        String key = queryParams[i].split("\\=")[0];
        String val = queryParams[i].split("\\=")[1];
        request_body.put(key, val);
      }
    }
    return request_body;
  }

  /**
   * Searches the database based on the given query and displays only those fields present in
   * attributeFilter.
   *
   * @param event The server request which contains the query and attributeFilter
   */
  private void searchAttribute(RoutingContext routingContext) {

    HttpServerRequest request = routingContext.request();
    String query = null;

    //System.out.println("APIVERTICLE searchAttribute(): "+routingContext.request().absoluteURI().contains("?"));
    
    if(routingContext.request().absoluteURI().contains("?")) 
    {
    
    try {
      query = URLDecoder.decode(request.query().toString(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      handle400(routingContext, "Bad Query");
      return;
    }
    logger.info("APIVERTICLE searchAttribute(): "+ query);

			JsonObject request_body = prepareQuery(query);
			String item_type = request_body.getString("item-type");
			//System.out.println(item_type);
			if(item_type == null) {
				item_type = "resourceItem";
				request_body.put("item-type", item_type);
			}
			if (item_type.equalsIgnoreCase("resourceServer") || item_type.equalsIgnoreCase("resourceServerGroup") || item_type.equalsIgnoreCase("resourceItem")) {
				databaseHandler("search-attribute", routingContext, request_body);
			}
			else {
				handle400(routingContext, "Bad Request. Should be one of [resourceServer, resourceServerGroup, resourceItem]");
			}
		} else {
			JsonObject request_body = new JsonObject();
			request_body.put("item-type", "resourceItem");
			databaseHandler("list", routingContext, request_body);
		}
	  }

  private void count(RoutingContext routingContext) {

	    HttpServerRequest request = routingContext.request();
	    String query = null;

	    System.out.println(routingContext.request().absoluteURI().contains("?"));
	    
	    if(routingContext.request().absoluteURI().contains("?")) 
	    {
	    
	    try {
	      query = URLDecoder.decode(request.query().toString(), "UTF-8");
	    } catch (UnsupportedEncodingException e) {
	      handle400(routingContext, "Bad Query");
	      return;
	    }
	    logger.info(query);

				JsonObject request_body = prepareQuery(query);
				String item_type = request_body.getString("item-type");
				System.out.println(item_type);
				if(item_type == null) {
					item_type = "resourceItem";
					request_body.put("item-type", item_type);
				}
				if (item_type.equalsIgnoreCase("resourceServer") || item_type.equalsIgnoreCase("resourceServerGroup") || item_type.equalsIgnoreCase("resourceItem")) {
					databaseHandler("count", routingContext, request_body);
				}
				else {
					handle400(routingContext, "Bad Request. Should be one of [resourceServer, resourceServerGroup, resourceItem]");
				}
	    } else {
			JsonObject request_body = new JsonObject();
			request_body.put("item-type", "resourceItem");
		    databaseHandler("count", routingContext, request_body);
	    }
  }

  /**
   * Deletes the item from the database
   *
   * @param event The server request which contains the id of the item.
   */
  private void delete(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();

		System.out.println("HIT : In Delete");
		
		if (decodeCertificate(routingContext)) {
			if (authenticateRequest(routingContext, "user.list")) {
				String id = null;
				String item_type = null;
				int state = routingContext.pathParams().size();
				
				if (state == 5)
				{
				id = routingContext.pathParam("domain") + "/" + routingContext.pathParam("provider") + "/"
						+ routingContext.pathParam("resourceServer") + "/"
						+ routingContext.pathParam("resourceCatogery") + "/" + routingContext.pathParam("resourceId");
				System.out.println(id);
				item_type = "resourceItem"; //   resourceGroupIds   resourceServerIds
				System.out.println(item_type);
				}
				else if(state == 2)
				{
					id = routingContext.pathParam("resourceServer") + "/"
							+ routingContext.pathParam("resourceCatogery");
					System.out.println(id);
					item_type = "resourceGroup"; //   resourceGroupIds   resourceServerIds
					System.out.println(item_type);
				}
				
				else if(state == 1)
				{
					id = routingContext.pathParam("resourceServer");
					System.out.println(id);
					item_type = "resourceServer"; //   resourceGroupIds   resourceServerIds
					System.out.println(item_type);
				}

				
				//if (id.contains(onboardedBy)) {
				if (true) {

					JsonObject request_body = new JsonObject();
					request_body.put("id", id);
					request_body.put("item_type", item_type);
					databaseHandler("delete", routingContext, request_body);
				} else {
					handle401(routingContext, "Unauthorised");
				}
			} else {
				handle401(routingContext, "Unauthorised");
			}
		} else {
			handle400(routingContext, "Certificate 'authenticaton' error");
		}
	}

  private void bulkDelete(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();

    if (decodeCertificate(routingContext)) {
      if (authenticateRequest(routingContext, "user.list")) {
        JsonObject request_body = new JsonObject();

        String bulkId = request.getParam("bulkId");
        request_body.put("bulk-id", bulkId);

        databaseHandler("bulkdelete", routingContext, request_body);
      } else {
        handle401(routingContext, "Unauthorised");
      }
    } else {
      handle400(routingContext, "Certificate 'authenticaton' error");
    }
  }
  /**
   * Updates the item from the database
   *
   * @param routingContext Contains the updated item
   */
  private void update(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    if (decodeCertificate(routingContext)) {
      if (authenticateRequest(routingContext, "user.list")) {
        try {
          JsonObject request_body = routingContext.getBodyAsJson();
          String query;
          try {
            query = URLDecoder.decode(request.query().toString(), "UTF-8");
          } catch (UnsupportedEncodingException e) {
            handle400(routingContext, "Bad Query");
            return;
          }
          logger.info(query);

          String id = routingContext.pathParam("domain") + "/" + routingContext.pathParam("provider") + "/"
				+ routingContext.pathParam("resourceServer") + "/" + routingContext.pathParam("resourceCatogery") + "/"
				+ routingContext.pathParam("resourceId");
          System.out.println(id);

          
          System.out.println(request_body.getJsonObject("id").getString("value"));
          if (id.equals(request_body.getString("id"))) {

            String itemType = query.split("=")[1];
            System.out.println(itemType);
            request_body.put("item-type", itemType);
            databaseHandler("update", routingContext, request_body);
          } else {
            handle400(routingContext, "Ids provided in the URI and object does not match");
          }
        } catch (Exception e) {
          handle400(routingContext, "Invalid item: Not a Json Object");
        }
      } else {
        handle401(routingContext, "Unauthorised");
      }
    } else {
      handle400(routingContext, "Certificate 'authenticaton' error");
    }
  }

  private void bulkUpdate(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    if (decodeCertificate(routingContext)) {
      if (authenticateRequest(routingContext, "user.list")) {
        try {
          JsonObject request_body = routingContext.getBodyAsJson();
          String bulkId = request.getParam("bulkId");
          request_body.put("bulk-id", bulkId);
          databaseHandler("bulkupdate", routingContext, request_body);

        } catch (Exception e) {
          handle400(routingContext, "Invalid item: Not a Json Object");
        }
      } else {
        handle401(routingContext, "Unauthorised");
      }
    } else {
      handle400(routingContext, "Certificate 'authenticaton' error");
    }
  }

  private void databaseHandler(
      String action, RoutingContext routingContext, JsonObject request_body) {

    DeliveryOptions database_action = new DeliveryOptions();
    database_action.addHeader("action", action);

    vertx
        .eventBus()
        .send(
            "database",
            request_body,
            database_action,
            database_reply -> {
              if (database_reply.succeeded()) {
                switch (action) {
                  case "getItem": break;
                  case "list":
                    handle200(routingContext, (JsonArray) database_reply.result().body());
                    break;
                  case "get-tags":
                  case "search-attribute":
                    handle200(routingContext, (JsonArray) database_reply.result().body());
                    break;
                  case "count":
                    handle200(routingContext, (JsonObject) database_reply.result().body());
                    break;
                  case "delete":
                    handle204(routingContext);
                    break;
                  case "create":
                    String resp = database_reply.result().body().toString();
                    if(resp.equals("conflict")) {
                    	handle409(routingContext);
                    } else {
                    	handle201(routingContext, resp);
                    	}
                    break;
                  case "update":
                    String status = database_reply.result().body().toString();
                    JsonObject s = new JsonObject().put("status", status);
                    handle200(routingContext, s);
                    break;
                  case "bulkcreate":
                    JsonObject reply = (JsonObject) database_reply.result().body();
                    handle200(routingContext, reply);
                    break;
                  case "bulkdelete":
                    handle204(routingContext);
                    break;
                  case "bulkupdate":
                    JsonObject rep = (JsonObject) database_reply.result().body();
                    handle200(routingContext, rep);
                    break;
                }
              } else {
                if (database_reply.cause().getMessage().equalsIgnoreCase("Failure")) {
                  handle500(routingContext);
                } else {
                  handle400(routingContext, database_reply.cause().getMessage());
                }
              }
            });
  }

  private String getStatusInJson(String status) {
    return (new JsonObject().put("Status", status)).encodePrettily();
  }

  private void handle400(RoutingContext routingContext, String status) {
    HttpServerResponse response = routingContext.response();
    String jsonStatus = getStatusInJson(status);
    response
        .setStatusCode(HTTP_STATUS_BAD_REQUEST)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(jsonStatus);
  }

  private void handle401(RoutingContext routingContext, String status) {
    HttpServerResponse response = routingContext.response();
    String jsonStatus = getStatusInJson(status);
    response
        .setStatusCode(HTTP_STATUS_UNAUTHORIZED)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(jsonStatus);
  }

  private void handle500(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();

    response.setStatusCode(HTTP_STATUS_INTERNAL_SERVER_ERROR).end();
  }

  private void handle200(RoutingContext routingContext, JsonArray reply) {
    HttpServerResponse response = routingContext.response();

    response.setStatusCode(HTTP_STATUS_OK).end(reply.encodePrettily());
  }

  private void handle200(RoutingContext routingContext, JsonObject reply) {
    HttpServerResponse response = routingContext.response();

    response.setStatusCode(HTTP_STATUS_OK).end(reply.encodePrettily());
  }

  private void handle204(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();

    response.setStatusCode(HTTP_STATUS_DELETED).end();
  }

  private void handle201(RoutingContext routingContext, String id) {
    HttpServerResponse response = routingContext.response();

    String JsonId = (new JsonObject().put("id", id)).encodePrettily();

    response.setStatusCode(HTTP_STATUS_CREATED).end(JsonId);
  }
  
  private void handle409(RoutingContext routingContext) {
	    HttpServerResponse response = routingContext.response();

	    response.setStatusCode(HTTP_STATUS_CONFLICT).end();
	  }
}
