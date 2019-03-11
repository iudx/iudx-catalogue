package iudx.catalogue.apiserver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
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

public class APIServerVerticle extends AbstractVerticle {

  private HttpServerResponse response;
  private JsonObject request_body;
  private String path;
  private String itemID;
  private String schemaID;
  private static final Logger logger = Logger.getLogger(APIServerVerticle.class.getName());

  @Override
  public void start(Future<Void> startFuture) {

    HttpServer server;
    int port = config().getInteger("http.port", 8443);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router
        .route("/")
        .handler(
            routingContext -> {
              response = routingContext.response();
              response.sendFile("ui/landing/landing.html");
            });

    router.get("/cat/search").handler(this::getAll);
    router.get("/cat/search/attribute").handler(this::searchAttribute);
    router.get("/cat/items/id/:itemID").handler(this::getItems);
    router.get("/cat/schemas/id/:schemaID").handler(this::getSchemas);

    router.post("/cat/items").handler(this::createItems);
    router.post("/cat/schemas").handler(this::createSchema);

    router.delete("/cat/items/id/:itemID").handler(this::deleteItems);

    logger.info("IUDX Catalogue Routes Defined !");

    server =
        vertx.createHttpServer(
            new HttpServerOptions()
                .setSsl(true)
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
  private boolean authenticateRequest(
      HttpServerRequest request, HttpServerResponse response, String file_path) {

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
          response.setStatusCode(401).end("Use Basic HTTP authorization");
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
                  response.setStatusCode(400).end("Your password is invalid");
                }

                if (allowed && !user.getBoolean("write_permission")) {
                  allowed = false;
                  response.setStatusCode(401).end("You do not have write access to the server");
                }
              } else {
                response.setStatusCode(400).end("User " + userId + " is not registered");
              }

            } catch (Exception e) {
              response.setStatusCode(500).end();
              logger.info(e.toString());
            }

          } else {
            response
                .setStatusCode(400)
                .end("Add userId and password in the header of your request");
          }
        }
      } catch (Exception e) {
        response.setStatusCode(401).end("Use Basic HTTP authorization");
      }

    } else {
      response.setStatusCode(401).end("Use Basic HTTP authorization");
    }

    logger.info("Authentication ended with flag : " + allowed);
    return allowed;
  }
  /**
   * Sends a request to the database to display all items
   *
   * @param event The server request
   */
  private void getAll(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    request_body = new JsonObject();
    DeliveryOptions database_action = new DeliveryOptions();
    database_action.addHeader("action", "search-attribute");
    databaseHandler(database_action, response, request_body);
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
    HttpServerResponse response = routingContext.response();
    path = request.path();
    String skip_validation = request.getHeader("skip_validation").toLowerCase();

    if (authenticateRequest(request, response, "user.list")) {
      logger.info(path);

      try {
        request_body = routingContext.getBodyAsJson();
        DeliveryOptions validator_action = new DeliveryOptions();
        validator_action.addHeader("action", "validate-item");
        if (skip_validation != null) {
          if (!("true".equals(skip_validation)) && !("false".equals(skip_validation))) {
            logger.info("skip_validation not a boolean");
            response.setStatusCode(400).end("Invalid value: skip_validation is not a boolean");
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

                    databaseHandler(database_action, response, request_body);

                  } else if (validator_reply.failed()) {
                    logger.info("Validator Failed");
                    response.setStatusCode(500).end();
                    return;
                  } else {
                    logger.info("No reply");
                    response.setStatusCode(500).end();
                    return;
                  }
                });

      } catch (Exception e) {
        response.setStatusCode(400).end("Invalid item: Not a Json Object");
        return;
      }
    } else {
      logger.info("Unauthorised");
      response.setStatusCode(401).end();
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
    HttpServerResponse response = routingContext.response();
    path = request.path();

    if (authenticateRequest(request, response, "user.list")) {
      logger.info(path);

      request.bodyHandler(
          body -> {
            try {
              request_body = body.toJsonObject();
              DeliveryOptions database_action = new DeliveryOptions();
              database_action.addHeader("action", "write-schema");

              databaseHandler(database_action, response, request_body);

            } catch (Exception e) {
              response.setStatusCode(400).end("Invalid schema: Not a Json Object");
              return;
            }
          });
    } else {
      logger.info("Unauthorised");
      response.setStatusCode(401).end();
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
    HttpServerResponse response = routingContext.response();

    // Example Query : curl -ik -XGET
    // 'https://localhost:8443/cat/search/attribute?owner=rbccps&tags=(etoilet,sanitation)&attributeFilter=(id,latitude,longitude)'

    String query = "";
    try {
      query = URLDecoder.decode(request.query().toString(), "UTF-8");
    } catch (UnsupportedEncodingException e) { // TODO Auto-generated catch block
      response.setStatusCode(400).end("Bad Query");
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

    databaseHandler(database_action, response, request_body);
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

      databaseHandler(database_action, response, request_body);

    } else {
      logger.info("Invalid Parameters");
      response.setStatusCode(400).end("Invalid request parameters");
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

      databaseHandler(database_action, response, request_body);

    } else {
      logger.info("Invalid Parameters");
      response.setStatusCode(400).end("Invalid request parameters");
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

    if (authenticateRequest(request, response, "user.list")) {

      logger.info(path);

      Future<String> decode_request = decodeRequest(request, response);

      if (decode_request.result().equalsIgnoreCase("valid")) {

        DeliveryOptions database_action = new DeliveryOptions();
        database_action.addHeader("action", "delete-item");
        request_body = new JsonObject();
        request_body.put("id", itemID);

        databaseHandler(database_action, response, request_body);
      } else {
        logger.info("Invalid Parameters");
        response.setStatusCode(400).end("Invalid request parameters");
        return;
      }

    } else {
      logger.info("Unauthorised");
      response.setStatusCode(401).end();
      return;
    }
  }

  private void databaseHandler(
      DeliveryOptions database_action, HttpServerResponse response, JsonObject request_body) {

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
                  response
                      .setStatusCode(200)
                      .end(((JsonArray) database_reply.result().body()).encodePrettily());
                  return;
                } else if ("delete-item".equals(action)
                    && "Success".equals(database_reply.result().body().toString())) {
                  response.setStatusCode(204).end();
                  return;
                } else if ("write-item".equals(action) || "write-schema".equals(action)) {
                  String id = database_reply.result().body().toString();
                  response.setStatusCode(201).end(id);
                  return;
                }
              } else if (database_reply.failed()) {
                logger.info("Failed in database handler");
                response.setStatusCode(500).end();
                return;
              } else {
                response.setStatusCode(500).end();
                return;
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
}
