package iudx.catalogue;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class MyRestIT {

	private static File clientCertificateDirectory;
	private static File resourcesDirectory;
	private static File clientConfiguration;
	private static String server_URI;
	private static long server_port;
	private static String client_keystore_password;
	private static String client_basic_auth;

	@BeforeSuite
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
		ExtentTestManager.startTest("List item-types", "GET to /list/catalogue/item-types");
		Response response = given().when().get("/list/catalogue/item-types").then().extract().response();
		
		int statusCode = response.getStatusCode();
		// Assert that correct status code is returned.
		Assert.assertEquals(statusCode /* actual value */, 200 /* expected value */, "Correct status code returned");
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
		ExtentTestManager.startTest("Create resource-item", "POST to /create/catalogue/resource-item");

		Response response = given().header("skip_validation", "true")
				.header("authorization", "Basic " + client_basic_auth).body(payload.toJSONString())
				.post("/create/catalogue/resource-item").then().extract().response();

		int statusCode = response.getStatusCode();
		// Assert that correct status code is returned.
		Assert.assertEquals(statusCode /* actual value */, 201 /* expected value */, "Correct status code returned");

	}

	@AfterSuite
	public static void unconfigureRestAssured() {
		RestAssured.reset();
	}

}
