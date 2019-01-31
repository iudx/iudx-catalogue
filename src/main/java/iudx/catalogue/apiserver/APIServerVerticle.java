package iudx.catalogue.apiserver;

import java.util.List;
import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;


public class APIServerVerticle extends AbstractVerticle implements Handler<HttpServerRequest> {

	HttpServer server;
	HttpServerRequest request;
	private HttpServerResponse resp;
	private JsonObject request_body;
	private final static Logger logger = Logger.getLogger(APIServerVerticle.class.getName());

	@Override
	public void start(Future<Void> startFuture) {

		int port = 8443;
		server = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
				.setKeyStoreOptions(new JksOptions().setPath("my-keystore.jks").setPassword("password")));
		server.requestHandler(APIServerVerticle.this).listen(port);

		logger.info("API Server Verticle started!");

	}

	@Override
	public void handle(HttpServerRequest event) {
		
		request = event;
		resp = request.response();

		switch (request.path()) {

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
		case "/cat/search/attribute": 
		{
			search_attribute(request);
			break;
		}
		
		default:
			resp.setStatusCode(400).end();
		}
	}

	private void create_items(HttpServerRequest event) {
		// TODO Auto-generated method stub

		if (event.method().toString().equalsIgnoreCase("POST")) 
		{
		request.bodyHandler(body -> 
		{
			request_body = body.toJsonObject();
		});
		
		DeliveryOptions validator_action = new DeliveryOptions();
		validator_action.addHeader("action", "validate-item");
		
		vertx.eventBus().send("validator", request_body, validator_action, validator_reply -> {
		if (validator_reply.succeeded()) 
		{
			
			DeliveryOptions database_action = new DeliveryOptions();
			database_action.addHeader("action", "write-item");

			vertx.eventBus().send("database", request_body, database_action, database_reply -> {
				
			if (database_reply.succeeded()) 
				{
					resp.setStatusCode(200).end();
					return;
				} else if (database_reply.failed()) 
				{
					logger.info("Database Failed");
					resp.setStatusCode(500).end();
					return;
				} else
				{
					resp.setStatusCode(500).end();
					return;
				}
				});
				} else if (validator_reply.failed()) 
				{
					logger.info("Validator Failed");
					resp.setStatusCode(500).end();
					return;
				} else 
				{
					logger.info("No reply");
					resp.setStatusCode(500).end();
					return;
				}
			});
		  } else 
		  {
			logger.info("End-Point Not Found");
			resp.setStatusCode(404).end();
			return;
		  }
	}
	

	private void create_schema(HttpServerRequest event) {
		// TODO Auto-generated method stub

		if (event.method().toString().equalsIgnoreCase("POST")) 
		{
		request.bodyHandler(body -> 
		{
			request_body = body.toJsonObject();
		});

		DeliveryOptions validator_action = new DeliveryOptions();
		validator_action.addHeader("action", "validate-schema");

		vertx.eventBus().send("validator", request_body, validator_action, validator_reply -> {
		if (validator_reply.succeeded()) 
		{
			DeliveryOptions database_action = new DeliveryOptions();
			database_action.addHeader("action", "write-schema");

			vertx.eventBus().send("database", request_body, database_action, database_reply -> {
				
			if (database_reply.succeeded()) 
				{
					resp.setStatusCode(200).end();
					return;
				} else if (database_reply.failed()) 
				{
					logger.info("Validator Failed");
					resp.setStatusCode(500).end();
					return;
				} else
				{
					resp.setStatusCode(200).end();
					return;
				}
				});
				} else if (validator_reply.failed()) 
				{
					logger.info("Validator Failed");
					resp.setStatusCode(500).end();
					return;
				} else 
				{
					logger.info("No reply");
					resp.setStatusCode(500).end();
					return;
				}
			});
		} else 
		{
			logger.info("End-Point Not Found");
			resp.setStatusCode(404).end();
			return;
		}
	}


	private void search_attribute(HttpServerRequest event) {
		// TODO Auto-generated method stub
		// Example Query : curl -ik -XGET 'https://localhost:8443/cat/search/attribute?owner=rbccps&tags=(etoilet,sanitation)&attributeFilter=(id,latitude,longitude)'
		if (event.method().toString().equalsIgnoreCase("GET")) 
		{
			String query =  request.query();
			logger.info(query);
			
			String[] query_parameters = query.split("\\&");
			int query_parameter_length = query_parameters.length;
			logger.info(Integer.toString(query_parameter_length));
			
			request_body = new JsonObject();
			
			for(int i = 0; i < query_parameter_length; i++) {
				request_body.put(query_parameters[i].split("\\=")[0], query_parameters[i].split("\\=")[1]);
				logger.info(query_parameters[i]);
			}
			logger.info(request_body.toString());
			
			DeliveryOptions database_action = new DeliveryOptions();logger.info(query);
			database_action.addHeader("action", "search-attribute");

			vertx.eventBus().send("database", request_body, database_action, database_reply -> {
				
			if (database_reply.succeeded()) 
				{	
					logger.info(database_reply.result().body().toString());
					resp.setStatusCode(200).end(database_reply.result().body().toString());
					return;
				} else if (database_reply.failed()) 
				{
					logger.info("Search Failed");
					resp.setStatusCode(500).end();
					return;
				} else
				{
					resp.setStatusCode(500).end();
					return;
				}
				});
		} else 
		{
			logger.info("End-Point Not Found");
			resp.setStatusCode(404).end();
			return;
		}	
	}

}
