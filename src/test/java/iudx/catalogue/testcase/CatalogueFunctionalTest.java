package iudx.catalogue.testcase;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

@RunWith(JUnitPlatform.class)
@DisplayName("Catalogue Functional Testing")
public class CatalogueFunctionalTest {

  private static final Logger logger = Logger.getLogger(CatalogueFunctionalTest.class.getName());

  static HttpClient httpClient = null;

  static final String catalogue_home_page_url = "https://localhost:8443";
  static final String catalogue_search_url = "https://localhost:8443/cat/search";
  static final String catalogue_search_tags_url =
      "https://localhost:8443/cat/search/attribute?tags=";

  static final int HTTP_STATUS_OK = 200;
  static final int HTTP_STATUS_CREATED = 201;
  static final int HTTP_STATUS_BAD_REQUEST = 400;
  static final int HTTP_STATUS_NOT_FOUND = 404;
  static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

  static final String CONTENT_TYPE_JSON = "Content-Type: application/json";
  static final String CONTENT_TYPE_HTML = "Content-Type: text/html";

  @BeforeAll
  public static void setUp() throws Exception {
    System.out.println("Setting up the HTTP Client");
    try {
      httpClient =
          HttpClients.custom()
              .setSSLContext(
                  new SSLContextBuilder()
                      .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                      .build())
              .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
              .build();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyStoreException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Testing valid item (JSON and auth) POST to catalogue.")
  public void post_item_with_valid_catalogue_item(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing in-valid item (JSON) POST to catalogue.")
  public void post_item_with_invalid_catalogue_item(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing in-valid auth for item (JSON) POST to catalogue.")
  public void post_item_with_invalid_catalogue_auth(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing in-valid (NULL) user ID for item (JSON) POST to catalogue.")
  public void post_item_with_null_user_catalogue_auth(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing in-valid (NULL) password for item (JSON) POST to catalogue.")
  public void post_item_with_null_password_catalogue_auth(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing in-valid (NOT_NULL) user ID for item (JSON) POST to catalogue.")
  public void post_item_with_invalid_user_catalogue_auth(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing in-valid (NOT_NULL) password for item (JSON) POST to catalogue.")
  public void post_item_with_invalid_password_catalogue_auth(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing writing with permission for item (JSON) POST to catalogue.")
  public void post_item_with_permission_catalogue_auth(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing writing without permission for item (JSON) POST to catalogue.")
  public void post_item_without_permission_catalogue_auth(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing valid tag search.")
  public void get_valid_catalogue_search_attribute_tags(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing in-valid tag search.")
  public void get_invalid_catalogue_search_attribute_tags(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing valid attribute filter search.")
  public void get_valid_catalogue_search_attribute_filters(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing in-valid attribute filter search.")
  public void get_invalid_catalogue_search_attribute_filters(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing home page.")
  public void get_catalogue_homepage(TestInfo testinfo) throws Exception {

    logger.info(testinfo.getDisplayName());

    HttpUriRequest upstream_request = new HttpGet(catalogue_home_page_url);

    // Create a custom response handler
    ResponseHandler<String> responseHandler =
        upstreamresponse -> {
          int status = upstreamresponse.getStatusLine().getStatusCode();
          assertEquals(HTTP_STATUS_OK, status);
          logger.info("Testing home page status code SUCCESS");

          String content_type = upstreamresponse.getEntity().getContentType().toString();
          assertEquals(CONTENT_TYPE_HTML, content_type);
          logger.info("Testing home page Content-Type SUCCESS");

          if (status >= 200 && status < 300) {
            HttpEntity entity = upstreamresponse.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
          } else {
            return "invalid-request";
          }
        };

    try {
      httpClient.execute(upstream_request, responseHandler);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Testing item search.")
  public void get_catalogue_items(TestInfo testinfo) throws Exception {
    logger.info(testinfo.getDisplayName());
    HttpUriRequest upstream_request = new HttpGet(catalogue_search_url);

    // Create a custom response handler
    ResponseHandler<String> responseHandler =
        upstreamresponse -> {
          int status = upstreamresponse.getStatusLine().getStatusCode();
          assertEquals(HTTP_STATUS_OK, status);
          logger.info("Testing item search status code SUCCESS");

          String content_type = upstreamresponse.getEntity().getContentType().toString();
          assertEquals(CONTENT_TYPE_JSON, content_type);
          logger.info("Testing item search Content-Type SUCCESS");

          if (status >= 200 && status < 300) {
            HttpEntity entity = upstreamresponse.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
          } else {
            return "invalid-request";
          }
        };

    try {
      httpClient.execute(upstream_request, responseHandler);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Testing catalogue search with in-valid key.")
  public void get_catalogue_item_with_invalid_key(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with empty key.")
  public void get_catalogue_item_with_empty_key(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with valid attribute filters.")
  public void get_catalogue_item_with_valid_attribute_filters(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with invalid attribute filters.")
  public void get_catalogue_item_with_invalid_attribute_filters(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with empty attribute filters.")
  public void get_catalogue_item_with_empty_attribute_filters(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with white-spaces in attribute filters.")
  public void get_catalogue_item_with_whitespace_in_attribute_filters(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with valid SINGLE TAG.")
  public void get_catalogue_item_with_single_valid_tags(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with valid MULTIPLE TAG.")
  public void get_catalogue_item_with_multiple_valid_tags(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with valid SINGLE in-valid TAG.")
  public void get_catalogue_item_with_single_invalid_tags(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing catalogue search with valid MULTIPLE in-valid TAG.")
  public void get_catalogue_item_with_multiple_invalid_tags(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with valid single in-case-sensitive TAG.")
  public void get_catalogue_item_with_single_case_insensitive_tags(TestInfo testinfo) throws Exception {}
  
  @Test
  @DisplayName("Testing catalogue search with valid ID.")
  public void get_catalogue_item_with_valid_ID(TestInfo testinfo) throws Exception {}

  @Test
  @DisplayName("Testing catalogue search with in-valid ID.")
  public void get_catalogue_item_with_invalid_ID(TestInfo testinfo) throws Exception {}
    
}
