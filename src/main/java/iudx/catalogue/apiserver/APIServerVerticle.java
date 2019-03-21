package iudx.catalogue.apiserver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.logging.Logger;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.json.JSONObject;
import org.json.JSONTokener;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
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

import java.util.Base64;
import java.util.Properties;

public class APIServerVerticle extends AbstractVerticle {

  private JsonObject request_body;
  private String path;
  private String itemID;
  private String schemaID;
  private static final Logger logger = Logger.getLogger(APIServerVerticle.class.getName());

  static final int HTTP_STATUS_OK = 200;
  static final int HTTP_STATUS_CREATED = 201;
  static final int HTTP_STATUS_DELETED = 204;
  static final int HTTP_STATUS_BAD_REQUEST = 400;
  static final int HTTP_STATUS_NOT_FOUND = 404;
  static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;
  static final int HTTP_STATUS_UNAUTHORIZED = 401;

  @Override
  public void start(Future<Void> startFuture) {

    HttpServer server;
    int port = config().getInteger("http.port", 8443);
    
    ClientAuth clientAuth;
    Properties systemProps;

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router
        .route("/")
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("ui/landing/landing.html");
            });

    router.get("/cat/search").handler(this::getAll);
    router.get("/cat/search/attribute").handler(this::searchAttribute);
    router.get("/cat/items/id/:itemID").handler(this::getItems);
    router.get("/cat/schemas/id/:schemaID").handler(this::getSchemas);

    router.post("/cat/items").handler(this::createItems);
    router.post("/cat/schemas").handler(this::createSchema);

    router.delete("/cat/items/id/:itemID").handler(this::deleteItems);

    router.put("/cat/items").handler(this::updateItems);

    logger.info("IUDX Catalogue Routes Defined !");

    clientAuth = ClientAuth.REQUEST;
    
    systemProps = System.getProperties();
    
    systemProps.put("javax.net.ssl.keyStore","my-keystore.jks");
    systemProps.put("javax.net.ssl.keyStorePassword","password");
    
    systemProps.put("javax.net.ssl.trustStore", "my-keystore.jks");
    systemProps.put("javax.net.ssl.trustStorePassword","password");
    
    System.setProperties(systemProps);
    
    logger.info("IUDX TLS Property Defined !");
    
    server =
        vertx.createHttpServer(
            new HttpServerOptions()
                .setSsl(true)
                .setClientAuth(clientAuth)
                .setKeyStoreOptions(
                    new JksOptions().setPath("my-keystore.jks").setPassword("password")));

    server.requestHandler(router::accept).listen(port);

    logger.info("API Server Verticle started!");
    startFuture.complete();
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
			String providerCN[] = cn.getName().split("=");
			String provider = providerCN[1];
			
			logger.info("Provider Name as per Certificate is " + provider);
			status = true;
		} catch (SSLPeerUnverifiedException e) { // TODO
			status = false;
			handle400(routingContext, "Certificate 'authenticaton' error");
		}

		return status;
	}

  /**
   * Sends a request to the database to display all items
   *
   * @param event The server request
   */
  private void getAll(RoutingContext routingContext) {
    request_body = new JsonObject();
    DeliveryOptions database_action = new DeliveryOptions();
    database_action.addHeader("action", "search-attribute");
    databaseHandler(database_action, routingContext, request_body);
  }
  /**
   * Sends a request to ValidatorVerticle to validate the item and DatabaseVerticle to insert it in
   * the database. Displays the id of the inserted item.
   *
   * @param event The server request which contains the item to be inserted in the database and the
   *     skip_validation header.
   */
  private void createItems(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    path = request.path();
    String skip_validation = request.getHeader("skip_validation").toLowerCase();

    if (authenticateRequest(routingContext, "user.list")) {
      logger.info(path);

      try {
        request_body = routingContext.getBodyAsJson();
        DeliveryOptions validator_action = new DeliveryOptions();
        validator_action.addHeader("action", "validate-item");
        if (skip_validation != null) {
          if (!("true".equals(skip_validation)) && !("false".equals(skip_validation))) {
            logger.info("skip_validation not a boolean");

            handle400(routingContext, "Invalid value: skip_validation is not a boolean");
            return;
          } else {
            validator_action.addHeader("skip_validation", skip_validation);
          }
        }

        vertx
            .eventBus()
            .send(
                "validator",
                request_body,
                validator_action,
                validator_reply -> {
                  if (validator_reply.succeeded()) {

                    DeliveryOptions database_action = new DeliveryOptions();
                    database_action.addHeader("action", "write-item");

                    databaseHandler(database_action, routingContext, request_body);

                  } else if (validator_reply.failed()) {
                    logger.info("Validator Failed");
                    handle500(routingContext);
                    return;
                  } else {
                    logger.info("No reply");
                    handle500(routingContext);

                    return;
                  }
                });

      } catch (Exception e) {
        handle400(routingContext, "Invalid item: Not a Json Object");
        return;
      }
    } else {
      logger.info("Unauthorised");
      handle401(routingContext, "Unauthorised");
      return;
    }
  }
  /**
   * Inserts the given schema into the database.
   *
   * @param event The server request which contains the schema to be inserted.
   */
  private void createSchema(RoutingContext routingContext) {

    HttpServerRequest request = routingContext.request();
    path = request.path();

    if (authenticateRequest(routingContext, "user.list")) {
      logger.info(path);

      request.bodyHandler(
          body -> {
            try {
              request_body = body.toJsonObject();
              DeliveryOptions database_action = new DeliveryOptions();
              database_action.addHeader("action", "write-schema");

              databaseHandler(database_action, routingContext, request_body);

            } catch (Exception e) {
              handle400(routingContext, "Invalid schema: Not a Json Object");
              return;
            }
          });
    } else {
      logger.info("Unauthorised");
      handle401(routingContext, "Unauthorised");
      return;
    }
  }
  /**
   * Converts the lost of values in string to JsoArray
   *
   * @param s List of values in string form
   * @return The JsonArray which contains values.
   */
  private JsonArray changeToArray(String s) {

    JsonArray values = new JsonArray();
    String[] arr = s.split(",");
    for (String a : arr) {
      if (a.charAt(0) == '(' && a.charAt(a.length() - 1) == ')') {
        if (a.length() > 2) {
          values.add(a.substring(1, a.length() - 1));
        }
      } else if (a.charAt(0) == '(') {
        if (a.length() > 1) {
          values.add(a.substring(1));
        }
      } else if (a.charAt(a.length() - 1) == ')') {
        if (a.length() > 1) {
          values.add(a.substring(0, a.length() - 1));
        }
      } else {
        if (a.length() > 0) {
          values.add(a);
        }
      }
    }

    return values;
  }
  /**
   * Searches the database based on the given query and displays only those fields present in
   * attributeFilter.
   *
   * @param event The server request which contains the query and attributeFilter
   */
  private void searchAttribute(RoutingContext routingContext) {

    HttpServerRequest request = routingContext.request();

    // Example Query : curl -ik -XGET
    // 'https://localhost:8443/cat/search/attribute?owner=rbccps&tags=(etoilet,sanitation)&attributeFilter=(id,latitude,longitude)'

    String query = "";
    try {
      query = URLDecoder.decode(request.query().toString(), "UTF-8");
    } catch (UnsupportedEncodingException e) { // TODO Auto-generated catch block
      handle400(routingContext, "Bad Query");
      return;
    }
    logger.info(query);
    request_body = new JsonObject();

    if (!query.equals("") && query != null) {
      String[] query_parameters = query.split("\\&");
      int query_parameter_length = query_parameters.length;
      logger.info(Integer.toString(query_parameter_length));

      for (int i = 0; i < query_parameter_length; i++) {
        request_body.put(
            query_parameters[i].split("\\=")[0],
            changeToArray(query_parameters[i].split("\\=")[1]));
        logger.info(query_parameters[i]);
      }
      logger.info(request_body.toString());
    }

    DeliveryOptions database_action = new DeliveryOptions();
    logger.info(query);

    database_action.addHeader("action", "search-attribute");

    databaseHandler(database_action, routingContext, request_body);
  }

  /**
   * Retrieves an item from the database
   *
   * @param event The server request which contains the id of the item
   */
  private void getItems(RoutingContext routingContext) {

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    Future<String> decode_request = decodeRequest(request, response);

    if (decode_request.result().equalsIgnoreCase("valid")) {

      DeliveryOptions database_action = new DeliveryOptions();
      database_action.addHeader("action", "read-item");
      request_body = new JsonObject();
      request_body.put("id", itemID);

      databaseHandler(database_action, routingContext, request_body);

    } else {
      logger.info("Invalid Parameters");
      handle400(routingContext, "Invalid request parameters");
      return;
    }
  }

  /**
   * Retrieves a schema from the database
   *
   * @param event The server request which contains the id of the schema.
   */
  private void getSchemas(RoutingContext routingContext) {

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    Future<String> decode_request = decodeRequest(request, response);

    if (decode_request.result().equalsIgnoreCase("valid")) {

      DeliveryOptions database_action = new DeliveryOptions();
      database_action.addHeader("action", "read-schema");
      request_body = new JsonObject();
      request_body.put("id", schemaID);

      databaseHandler(database_action, routingContext, request_body);

    } else {
      logger.info("Invalid Parameters");
      handle400(routingContext, "Invalid request parameters");
      return;
    }
  }
  /**
   * Deletes the item from the database
   *
   * @param event The server request which contains the id of the item.
   */
  private void deleteItems(RoutingContext routingContext) {

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    path = request.path();

    if (authenticateRequest(routingContext, "user.list")) {

      logger.info(path);

      Future<String> decode_request = decodeRequest(request, response);

      if (decode_request.result().equalsIgnoreCase("valid")) {

        DeliveryOptions database_action = new DeliveryOptions();
        database_action.addHeader("action", "delete-item");
        request_body = new JsonObject();
        request_body.put("id", itemID);

        databaseHandler(database_action, routingContext, request_body);
      } else {
        logger.info("Invalid Parameters");
        handle400(routingContext, "Invalid request parameters");
        return;
      }

    } else {
      logger.info("Unauthorised");
      handle401(routingContext, "Unauthorised");
      return;
    }
  }
  /**
   * Updates the item from the database
   *
   * @param routingContext Contains the updated item
   */
  private void updateItems(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    path = request.path();

    if (authenticateRequest(routingContext, "user.list")) {
      logger.info(path);

      try {
        request_body = routingContext.getBodyAsJson();
        DeliveryOptions database_action = new DeliveryOptions();
        database_action.addHeader("action", "update-item");

        databaseHandler(database_action, routingContext, request_body);

      } catch (Exception e) {
        handle400(routingContext, "Invalid item: Not a Json Object");
        return;
      }
    } else {
      logger.info("Unauthorised");
      handle401(routingContext, "Unauthorised");
      return;
    }
  }

  private void databaseHandler(
      DeliveryOptions database_action, RoutingContext routingContext, JsonObject request_body) {

    MultiMap headers = database_action.getHeaders();
    String action = headers.get("action");

    vertx
        .eventBus()
        .send(
            "database",
            request_body,
            database_action,
            database_reply -> {
              if (database_reply.succeeded()) {
                logger.info(database_reply.result().body().toString());

                if ("read-item".equals(action)
                    || "read-schema".equals(action)
                    || "search-attribute".equals(action)) {
                  handle200(routingContext, database_reply.result());
                } else if ("delete-item".equals(action)) {
                  if ("Success".equals(database_reply.result().body().toString())) {
                    handle204(routingContext);
                  } else {
                    logger.info("Item not found");
                    handle400(routingContext, database_reply.result().body().toString());
                  }

                } else if ("write-item".equals(action) || "write-schema".equals(action)) {
                  String id = database_reply.result().body().toString();
                  handle201(routingContext, id);
                } else if ("update-item".equals(action)) {
                  if (database_reply.result().body().toString().contains("Error")) {
                    System.out.println("adp");
                    handle400(routingContext, database_reply.result().body().toString());
                  } else {
                    String id = database_reply.result().body().toString();
                    handle201(routingContext, id);
                  }
                }
              } else {
                handle500(routingContext);
              }
            });
  }

  private Future<String> decodeRequest(HttpServerRequest request, HttpServerResponse response) {

    Future<String> decode_request = Future.future();

    try {

      if (request.absoluteURI().contains("items")) {
        itemID = request.getParam("itemID");
        logger.info(itemID);

        if ((itemID == null)) {
          response.setStatusCode(400).end("Invalid Request");
          decode_request.complete("invalid-request");
        } else {
          decode_request.complete("valid");
        }
      } else if (request.absoluteURI().contains("schemas")) {
        schemaID = request.getParam("schemaID");
        logger.info(schemaID);

        if ((schemaID == null)) {
          response.setStatusCode(400).end("Invalid Request");
          decode_request.complete("invalid-request");
        } else {
          decode_request.complete("valid");
        }
      }

    } catch (Exception e) {
      response.setStatusCode(400).end("Invalid Request");
      decode_request.complete("invalid-request");
    }

    return decode_request;
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

  private void handle200(RoutingContext routingContext, Message<Object> databaseReply) {
    HttpServerResponse response = routingContext.response();

    response.setStatusCode(HTTP_STATUS_OK).end(((JsonArray) databaseReply.body()).encodePrettily());
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
}
