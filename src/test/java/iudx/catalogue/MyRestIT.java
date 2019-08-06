package iudx.catalogue;

import static io.restassured.RestAssured.given;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import iudx.catalogue.ExtentTestManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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

    } catch (Exception e) {
      e.printStackTrace();
    }

    RestAssured.baseURI = server_URI;
    RestAssured.port = (int) server_port;
    RestAssured.keyStore(clientCertificateDirectory.getAbsolutePath(), client_keystore_password);
    RestAssured.trustStore(clientCertificateDirectory.getAbsolutePath(), client_keystore_password);
  }

  @DataProvider(name = "validItems")
  public Object[][] createTestDataRecords() {
    return new Object[][] {
      {"valid1.json"},
      {"valid2.json"},
      {"valid3.json"},
      {"valid4.json"},
      {"valid5.json"},
      {"valid6.json"},
      {"valid7.json"},
      {"valid8.json"},
      {"valid9.json"},
      {"valid10.json"}
    };
  }

  private String readFile(String path) {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream("testData/" + path);
    String result =
        new BufferedReader(new InputStreamReader(inputStream))
            .lines()
            .collect(Collectors.joining("\n"));
    return result;
  }

  @Test(dataProvider = "validItems")
  public void postValid(String pathToValidItems) {

    ExtentTestManager.startTest("Create resource-item", "POST to /create/catalogue/resource-item");

    given()
        .header("skip_validation", "true")
        .header("authorization", "Basic " + client_basic_auth)
        .body(readFile(pathToValidItems))
    .when()
        .post("/create/catalogue/resource-item")
    .then()
        .assertThat()
        .statusCode(201);

  }

  @AfterSuite
  public static void unconfigureRestAssured() {
    RestAssured.reset();
  }
}
