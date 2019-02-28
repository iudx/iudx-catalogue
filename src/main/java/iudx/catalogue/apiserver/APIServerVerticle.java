package iudx.catalogue.apiserver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import java.util.Base64;

public class APIServerVerticle extends AbstractVerticle implements Handler<HttpServerRequest> {

  HttpServer server;
  HttpServerRequest request;
  private HttpServerResponse resp;
  private JsonObject request_body;
  String path;
  String[] path_parameters;
  String itemID;
  String schemaID;
  private static final Logger logger = Logger.getLogger(APIServerVerticle.class.getName());

  @Override
  public void start(Future<Void> startFuture) {

    int port = config().getInteger("http.port", 8443);
    server =
        vertx.createHttpServer(
            new HttpServerOptions()
                .setSsl(true)
                .setKeyStoreOptions(
                    new JksOptions().setPath("my-keystore.jks").setPassword("password")));
    server.requestHandler(APIServerVerticle.this).listen(port);

    logger.info("API Server Verticle started!");
  }
  /**
   * Authenticates the user and calls the method depending on the URL to which the request is sent to.
   * @param event The server request
   */
  @Override
  public void handle(HttpServerRequest event) {

    request = event;
    resp = request.response();
    path = request.path();

    if (request.path().equals("/")) {
      resp.sendFile("ui/landing/landing.html");
      return;
    }

    if (path.contains("/cat/items/id/")) {
      path_parameters = path.split("\\/");
      itemID = path_parameters[4];
      logger.info(itemID);
      path = "/cat/items/id/";
    } else if (path.contains("/cat/schemas/id/")) {
      path_parameters = path.split("\\/");
      schemaID = path_parameters[4];
      logger.info(schemaID);
      path = "/cat/schemas/id/";
    }

    if (!event.method().toString().equalsIgnoreCase("GET")) {
      if (authenticate_request(event, path, "user.list")) {
        logger.info(path);

        switch (path) {
          case "/cat/items":
            {
              create_items(request);
              break;
            }
          case "/cat/schemas":
            {
              create_schema(request);
              break;
            }
          case "/cat/items/id/":
            {
              delete_items(request);
              break;
            }
          default:
            resp.setStatusCode(404).end();
        }
      }
    } else {
      switch (path) {
        case "/cat/search/attribute":
          {
            search_attribute(request);
            break;
          }
        case "/cat/items/id/":
          {
            get_items(request);
            break;
          }
        case "/cat/schemas/id/":
          {
            get_schemas(request);
            break;
          }
        case "/cat/search":
          {
            get_all(request);
            break;
          }
        default:
          resp.setStatusCode(404).end();
      }
    }
  }
  /**
   * Checks if the user has necessary permission to write or delete from the database
   *
   * @param event The server request
   * @param path The URL to which the request is sent
   * @param file_path The path of the file which contains the list of users and their permissions
   * @return
   */
  private boolean authenticate_request(HttpServerRequest event, String path, String file_path) {

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
          resp.setStatusCode(401).end("Use Basic HTTP authorization");
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
                  resp.setStatusCode(400).end("Your password is invalid");
                }

                if (allowed == true) {
                  switch (path) {
                    case "/cat/items":
                    case "/cat/schemas":
                      {
                        if (!user.getBoolean("write_permission")) {
                          allowed = false;
                          resp.setStatusCode(401).end("You do not have write access to the server");
                        }
                        break;
                      }

                    case "/cat/search/attribute":
                    case "/cat/items/id/":
                    case "/cat/schemas/id/":
                    case "/cat/search":
                      {
                        if (!user.getBoolean("read_permission")) {
                          allowed = false;
                          resp.setStatusCode(401).end("You do not have read access to the server");
                        }
                        break;
                      }
                    default:
                      resp.setStatusCode(404).end("Invalid path");
                  }
                }
              } else {
                resp.setStatusCode(400).end("User " + userId + " is not registered");
              }

            } catch (Exception e) {
              resp.setStatusCode(500).end();
              System.out.println(e);
            }

          } else {
            resp.setStatusCode(400).end("Add userId and password in the header of your request");
          }
        }
      } catch (Exception e) {
        resp.setStatusCode(401).end("Use Basic HTTP authorization");
      }

    } else {
      resp.setStatusCode(401).end("Use Basic HTTP authorization");
    }

    logger.info("Authentication ended with flag : " + allowed);
    return allowed;
  }
  /**
   * Sends a request to the database to display all items
   *
   * @param event The server request
   */
  private void get_all(HttpServerRequest event) {

    if (event.method().toString().equalsIgnoreCase("GET")) {
      request_body = new JsonObject();

      DeliveryOptions database_action = new DeliveryOptions();
      database_action.addHeader("action", "search-attribute");

      vertx
          .eventBus()
          .send(
              "database",
              request_body,
              database_action,
              database_reply -> {
                if (database_reply.succeeded()) {
                  logger.info(database_reply.result().body().toString());
                  resp.setStatusCode(200)
                      .end(((JsonArray) database_reply.result().body()).encodePrettily());
                  return;
                } else if (database_reply.failed()) {
                  logger.info("Search Failed");
                  resp.setStatusCode(500).end();
                  return;
                } else {
                  resp.setStatusCode(500).end();
                  return;
                }
              });
    } else {
      logger.info("End-Point Not Found");
      resp.setStatusCode(404).end();
      return;
    }
  }
  /**
   * Sends a request to ValidatorVerticle to validate the item and DatabaseVerticle to insert it in
   * the database. Displays the id of the inserted item.
   *
   * @param event The server request which contains the item to be inserted in the database and the
   *     skip_validation header.
   */
  private void create_items(HttpServerRequest event) {

    if (event.method().toString().equalsIgnoreCase("POST")) {
      request.bodyHandler(
          body -> {
            try {
              request_body = body.toJsonObject();
              String skip_validation = event.getHeader("skip_validation").toLowerCase();

              DeliveryOptions validator_action = new DeliveryOptions();
              validator_action.addHeader("action", "validate-item");
              if (skip_validation != null) {
                if (!skip_validation.equals("true") && !skip_validation.equals("false")) {
                  logger.info("skip_validation not a boolean");
                  resp.setStatusCode(400).end("Invalid value: skip_validation is not a boolean");
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

                          vertx
                              .eventBus()
                              .send(
                                  "database",
                                  request_body,
                                  database_action,
                                  database_reply -> {
                                    if (database_reply.succeeded()) {
                                      String id = database_reply.result().body().toString();
                                      resp.setStatusCode(201).end(id);
                                      return;
                                    } else if (database_reply.failed()) {
                                      logger.info("Database Failed");
                                      resp.setStatusCode(500).end();
                                      return;
                                    } else {
                                      resp.setStatusCode(500).end();
                                      return;
                                    }
                                  });
                        } else if (validator_reply.failed()) {
                          logger.info("Validator Failed");
                          resp.setStatusCode(500).end();
                          return;
                        } else {
                          logger.info("No reply");
                          resp.setStatusCode(500).end();
                          return;
                        }
                      });

            } catch (Exception e) {
              resp.setStatusCode(400).end("Invalid item: Not a Json Object");
              return;
            }
          });
    } else {
      logger.info("End-Point Not Found");
      resp.setStatusCode(404).end();
      return;
    }
  }
  /**
   * Inserts the given schema into the database.
   *
   * @param event The server request which contains the schema to be inserted.
   */
  private void create_schema(HttpServerRequest event) {

    if (event.method().toString().equalsIgnoreCase("POST")) {
      request.bodyHandler(
          body -> {
            try {
              request_body = body.toJsonObject();
              DeliveryOptions database_action = new DeliveryOptions();
              database_action.addHeader("action", "write-schema");

              vertx
                  .eventBus()
                  .send(
                      "database",
                      request_body,
                      database_action,
                      database_reply -> {
                        if (database_reply.succeeded()) {
                          resp.setStatusCode(201).end();
                          return;
                        } else if (database_reply.failed()) {
                          logger.info("Database Failed");
                          resp.setStatusCode(500).end();
                          return;
                        } else {
                          resp.setStatusCode(500).end();
                          return;
                        }
                      });
            } catch (Exception e) {
              resp.setStatusCode(400).end("Invalid schema: Not a Json Object");
              return;
            }
          });

    } else {
      logger.info("End-Point Not Found");
      resp.setStatusCode(404).end();
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
  private void search_attribute(HttpServerRequest event) {

    // Example Query : curl -ik -XGET
    // 'https://localhost:8443/cat/search/attribute?owner=rbccps&tags=(etoilet,sanitation)&attributeFilter=(id,latitude,longitude)'
    if (event.method().toString().equalsIgnoreCase("GET")) {
      String query = request.query();
      logger.info(query);
      request_body = new JsonObject();

      if (!query.equals("") && query != null) {
        query = String.join(" ", query.split("\\%20"));
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

      vertx
          .eventBus()
          .send(
              "database",
              request_body,
              database_action,
              database_reply -> {
                if (database_reply.succeeded()) {
                  logger.info(database_reply.result().body().toString());
                  resp.setStatusCode(200)
                      .end(((JsonArray) database_reply.result().body()).encodePrettily());
                  return;
                } else if (database_reply.failed()) {
                  logger.info("Search Failed");
                  resp.setStatusCode(500).end();
                  return;
                } else {
                  resp.setStatusCode(500).end();
                  return;
                }
              });
    } else {
      logger.info("End-Point Not Found");
      resp.setStatusCode(404).end();
      return;
    }
  }

  /**
   * Retrieves an item from the database
   *
   * @param event The server request which contains the id of the item
   */
  private void get_items(HttpServerRequest event) {

    if (event.method().toString().equalsIgnoreCase("GET")) {

      DeliveryOptions database_action = new DeliveryOptions();
      database_action.addHeader("action", "read-item");
      request_body = new JsonObject();
      request_body.put("id", itemID);
      vertx
          .eventBus()
          .send(
              "database",
              request_body,
              database_action,
              database_reply -> {
                if (database_reply.succeeded()) {
                  logger.info(database_reply.result().body().toString());
                  resp.setStatusCode(200)
                      .end(((JsonArray) database_reply.result().body()).encodePrettily());
                  return;
                } else if (database_reply.failed()) {
                  logger.info("Database Failed");
                  resp.setStatusCode(500).end();
                  return;
                } else {
                  resp.setStatusCode(500).end();
                  return;
                }
              });
    } else {
      logger.info("End-Point Not Found");
      resp.setStatusCode(404).end();
      return;
    }
  }

  /**
   * Retrieves a schema from the database
   *
   * @param event The server request which contains the id of the schema.
   */
  private void get_schemas(HttpServerRequest event) {

    if (event.method().toString().equalsIgnoreCase("GET")) {

      DeliveryOptions database_action = new DeliveryOptions();
      database_action.addHeader("action", "read-schema");
      request_body = new JsonObject();
      request_body.put("id", schemaID);
      vertx
          .eventBus()
          .send(
              "database",
              request_body,
              database_action,
              database_reply -> {
                if (database_reply.succeeded()) {
                  logger.info(database_reply.result().body().toString());
                  resp.setStatusCode(200)
                      .end(((JsonArray) database_reply.result().body()).encodePrettily());
                  return;
                } else if (database_reply.failed()) {
                  logger.info("Validator Failed");
                  resp.setStatusCode(500).end();
                  return;
                } else {
                  resp.setStatusCode(500).end();
                  return;
                }
              });
    } else {
      logger.info("End-Point Not Found");
      resp.setStatusCode(404).end();
      return;
    }
  }
  /**
   * Deletes the item from the database
   *
   * @param event The server request which contains the id of the item.
   */
  private void delete_items(HttpServerRequest event) {

    if (event.method().toString().equalsIgnoreCase("DELETE")) {

      DeliveryOptions database_action = new DeliveryOptions();
      database_action.addHeader("action", "delete-item");
      request_body = new JsonObject();
      request_body.put("id", itemID);
      vertx
          .eventBus()
          .send(
              "database",
              request_body,
              database_action,
              database_reply -> {
                if (database_reply.succeeded()) {
                  logger.info(database_reply.result().body().toString());
                  resp.setStatusCode(204).end();
                  return;
                } else if (database_reply.failed()) {
                  logger.info("Database Failed");
                  resp.setStatusCode(500).end();
                  return;
                } else {
                  resp.setStatusCode(500).end();
                  return;
                }
              });
    } else {
      logger.info("End-Point Not Found");
      resp.setStatusCode(404).end();
      return;
    }
  }
}
