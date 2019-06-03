package iudx.catalogue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

class MyRestIT {
  @BeforeAll
  public static void configureRestAssured() {
    RestAssured.baseURI = "https://localhost";
    RestAssured.port = Integer.getInteger("http.port", 8443);
    RestAssured.useRelaxedHTTPSValidation();
  }

  @AfterAll
  public static void unconfigureRestAssured() {
    RestAssured.reset();
  }

  @Test
  public void test() {
    given().when().get("/list/catalogue/item-types").then().assertThat().statusCode(200);
  }
}
