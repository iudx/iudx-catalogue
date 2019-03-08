package iudx.catalogue.testcase;

public interface CatlogueTesting {

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
}
