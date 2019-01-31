package iudx.catalogue.database;

import java.util.logging.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class DatabaseVerticle extends AbstractVerticle {
	
	private final static Logger logger = Logger.getLogger(DatabaseVerticle.class.getName());
	String action;
	JsonObject request_body;
	
	@Override
	public void start(Future<Void> startFuture) {

		logger.info("Database Verticle started!");
		vertx.eventBus().consumer("database", message -> {

			Future<Void> requestHandler = validateRequest(message);

			if (requestHandler.succeeded()) 
			{
				message.reply("success");
			} else 
			{
				message.fail(0, "failure");
			}

		});
	}

	private Future<Void> validateRequest(Message<Object> message) {
		// TODO Auto-generated method stub
		logger.info("Database Verticle received message.body() = " + message.body());
		
		action = (String) message.headers().get("action");
		request_body = (JsonObject) message.body();
		
		Future<Void> messageHandler = Future.future();

		switch (action) 
		{
			case "read-item" : 
			{
				read_item(messageHandler);
				break;
			}
		
			case "read-schema" : 
			{
				read_schema(messageHandler);
				break;
			}

			
			case "write-item" : 
			{
				write_item(messageHandler);
				break;
			}

			case "write-schema" : 
			{
				write_schema(messageHandler);
				break;
			}
			
			case "update-item" : 
			{
				update_item(messageHandler);
				break;
			}
			
			case "update-schema" : 
			{
				update_schema(messageHandler);
				break;
			}
			
			case "delete-item" : 
			{
				delete_item(messageHandler);
				break;
			}

			case "delete-schema" : 
			{
				delete_schema(messageHandler);
				break;
			}
			
			case "search-attribute" : 
			{
				search_attribute(messageHandler);
				break;
			}
		}
		
		return messageHandler;
	}

	private void search_attribute(Future<Void> messageHandler) {
		// TODO Auto-generated method stub
		// Implement the block
		messageHandler.complete();	
	}

	private void delete_schema(Future<Void> messageHandler) {
		// TODO Auto-generated method stub
		// Implement the block
		messageHandler.complete();		
	}

	private void delete_item(Future<Void> messageHandler) {
		// TODO Auto-generated method stub
		// Implement the block
		messageHandler.complete();		
	}

	private void update_schema(Future<Void> messageHandler) {
		// TODO Auto-generated method stub
		// Implement the block
		messageHandler.complete();		
	}

	private void update_item(Future<Void> messageHandler) {
		// TODO Auto-generated method stub
		// Implement the block
		messageHandler.complete();		
	}

	private void write_schema(Future<Void> messageHandler) {
		// TODO Auto-generated method stub
		// Implement the block
		messageHandler.complete();
	}

	private void write_item(Future<Void> messageHandler) {
		// TODO Auto-generated method stub
		// Implement the block
		messageHandler.complete();
	}

	private void read_schema(Future<Void> messageHandler) {
		// TODO Auto-generated method stub
		// Implement the block
		messageHandler.complete();
	}

	private void read_item(Future<Void> messageHandler) {
		// TODO Auto-generated method stub
		// Implement the block
		messageHandler.complete();
		
	}
}
