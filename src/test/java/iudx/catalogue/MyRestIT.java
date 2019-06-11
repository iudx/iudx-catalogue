package iudx.catalogue;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

class MyRestIT {

	static File clientCertificateDirectory;
	static File resourcesDirectory;
	static File clientConfiguration;
	static String server_URI;
	static long server_port;
	static String client_keystore_password;
	static String client_basic_auth;

	@BeforeAll
	public static void configureRestAssured() {

		clientCertificateDirectory = new File("client.jks");
		resourcesDirectory = new File("src/test/resources/testData");
		clientConfiguration = new File("src/test/resources/IT_conf.json");

		// JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();
		JSONObject config = null;
		try (FileReader reader = new FileReader(clientConfiguration.getAbsolutePath())) {
			// Read JSON file
			config = (JSONObject) jsonParser.parse(reader);
			server_URI = (String) config.get("server_URI");
			server_port = (long) config.get("server_port");
			client_keystore_password = (String) config.get("client_keystore_password");
			client_basic_auth = (String) config.get("client_basic_authorization");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		RestAssured.baseURI = server_URI;
		RestAssured.port = (int) server_port;
		RestAssured.keyStore(clientCertificateDirectory.getAbsolutePath(), client_keystore_password);
		RestAssured.trustStore(clientCertificateDirectory.getAbsolutePath(), client_keystore_password);

	}

	@Test
	public void test() {

		Response response = given().when().get("/list/catalogue/item-types").then().extract().response();

	}

	@Test
	public void postTest() {

		// JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();
		JSONObject payload = null;
		try (FileReader reader = new FileReader(resourcesDirectory.getAbsolutePath() + "/valid_item.json")) {
			// Read JSON file
			payload = (JSONObject) jsonParser.parse(reader);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		Response response = given().contentType(ContentType.JSON).header("skip_validation", "true")
				.header("authorization", "Basic " + client_basic_auth).body(payload.toJSONString())
				.post("/create/catalogue/resource-item").then().extract().response();

	}

	  @AfterAll
	  public static void unconfigureRestAssured() {
	    RestAssured.reset();
	  }

}
