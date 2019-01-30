package iudx.catalogue.apiserver;

import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
			if (event.method().toString().equalsIgnoreCase("POST")) 
			{
			request.bodyHandler(body -> 
			{
				request_body = body.toJsonObject();
			});
			
			DeliveryOptions action = new DeliveryOptions();
			action.addHeader("action", "validate-item");
			
			vertx.eventBus().send("validator", request_body, action, validator_reply -> {
			if (validator_reply.succeeded()) 
			{
				vertx.eventBus().send("database", "write-item", database_reply -> {
					
				if (database_reply.succeeded()) 
					{
						resp.setStatusCode(200).end();
					} else if (database_reply.failed()) 
					{
						logger.info("Database Failed");
						resp.setStatusCode(500).end();
					} else
					{
						resp.setStatusCode(500).end();
					}
					});
					} else if (validator_reply.failed()) 
					{
						logger.info("Validator Failed");
						resp.setStatusCode(500).end();
					} else 
					{
						logger.info("No reply");
						resp.setStatusCode(500).end();
					}
				});
			  } else 
			  {
				logger.info("End-Point Not Found");
				resp.setStatusCode(404).end();  
			  }
			break;
			}
		case "/cat/schemas": 
		{
			if (event.method().toString().equalsIgnoreCase("POST")) 
			{
			request.bodyHandler(body -> 
			{
				request_body = body.toJsonObject();
			});

			DeliveryOptions action = new DeliveryOptions();
			action.addHeader("action", "validate-item");

			vertx.eventBus().send("validator", request_body, action, validator_reply -> {
			if (validator_reply.succeeded()) 
			{
				vertx.eventBus().send("database", "write-schema", database_reply -> {
					
				if (database_reply.succeeded()) 
					{
						resp.setStatusCode(200).end();
					} else if (database_reply.failed()) 
					{
						logger.info("Validator Failed");
						resp.setStatusCode(500).end();
					} else
					{
						resp.setStatusCode(200).end();
					}
					});
					} else if (validator_reply.failed()) 
					{
						logger.info("Validator Failed");
						resp.setStatusCode(500).end();
					} else 
					{
						logger.info("No reply");
						resp.setStatusCode(500).end();
					}
				});
			} else 
			{
				logger.info("End-Point Not Found");
				resp.setStatusCode(404).end();  
			}
				break;
		}

		default:
			resp.setStatusCode(400).end();
		}
	}
}
