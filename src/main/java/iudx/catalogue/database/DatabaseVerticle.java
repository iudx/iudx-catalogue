package iudx.catalogue.database;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class DatabaseVerticle extends AbstractVerticle {

	private static final Logger logger = Logger.getLogger(DatabaseVerticle.class.getName());
	private DatabaseInterface db;
	private MongoClient mongo;
	private JsonObject mongoconfig;
	private String database_host;
	private int database_port;
	private String database_user;
	private String database_password;
	private String database_name;
	private String auth_database;
  
  /**
   * Constructor for DatabaseVerticle
   *
   * @param which_database The name of the database
   */
  public DatabaseVerticle(String which_database) {

    if ("mongo".equals(which_database)) {
      db = new MongoDB();
    }
  }

  @Override
  public void start(Future<Void> startFuture) {
    JsonObject mongoconfig;
    String database_uri;

    logger.info("Database Verticle started!");
    vertx
        .eventBus()
        .consumer(
            "database",
            message -> {
              validateRequest(message);
            });

	Properties prop = new Properties();
    InputStream input = null;

    try 
    {
        input = new FileInputStream("config.properties");
        prop.load(input);
        
        database_user		=	prop.getProperty("database_user");
        database_password	=	prop.getProperty("database_password");
        database_host 		=	prop.getProperty("database_host");
        database_port		=	Integer.parseInt(prop.getProperty("database_port"));
        database_name		=	prop.getProperty("database_name");
        auth_database		=	prop.getProperty("auth_database");
        	        

        logger.info("database_user 	: " + database_user);
        logger.info("database_password	: " + database_password);
        logger.info("database_host 	: " + database_host);
        logger.info("database_port 	: " + database_port);
        logger.info("database_name		: " + database_name);
        logger.info("auth_database		: " + auth_database);
        
        input.close();
        
    } 
    catch (Exception e) 
    {
        e.printStackTrace();
    } 
    
	mongoconfig		= 	new JsonObject()
					.put("username", database_user)
					.put("password", database_password)
					.put("authSource", auth_database)
					.put("host", database_host)
					.put("port", database_port)
					.put("db_name", database_name);

    Future<Void> init_fut = db.initDB(vertx, mongoconfig);
    init_fut.setHandler(startFuture.completer());
  }

  /**
   * Calls the database method depending on the action sent by APIServerVerticle
   *
   * @param message Contains the action that has to be performed and the required parameters.
   */
  private void validateRequest(Message<Object> message) {
    // TODO Auto-generated method stub
    logger.info("Database Verticle received message.body() = " + message.body());

    String action = (String) message.headers().get("action");

    switch (action) {
      case "list":
        {
          db.list(message);
          break;
        }

      case "getItem":
      {
        db.getItem(message);
        break;
      }

      case "get-tags":
        {
          db.listTags(message);
          break;
        }

      case "create":
        {
          db.create(message);
          break;
        }

      case "update":
        {
          db.update(message);
          break;
        }

      case "delete":
        {
          db.delete(message);
          break;
        }

      case "search-attribute":
        {
          db.searchAttribute(message);
          break;
        }
      case "count":
      {
    	  db.count(message);
    	  break;
      }
      case "bulkcreate":
      {
    	  db.bulkCreate(message);
    	  break;
      }
      case "bulkupdate":
      {
    	  db.bulkUpdate(message);
    	  break;
      }
      case "bulkdelete":
      {
    	  db.bulkDelete(message);
    	  break;
      }
      default:
        {
          break;
        }
    }
  }
}
