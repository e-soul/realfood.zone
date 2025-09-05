package zone.realfood;

public class LayoutModel {
  private final String title;
  private final String staticBaseUrl;
  private final String cssUrl;
  private final String userEmail;

  public LayoutModel(String title, String staticBaseUrl, String cssUrl, String userEmail) {
    this.title = title;
    this.staticBaseUrl = staticBaseUrl;
    this.cssUrl = cssUrl;
    this.userEmail = userEmail;
  }

  public String getTitle() { return title; }
  public String getStaticBaseUrl() { return staticBaseUrl; }
  public String getCssUrl() { return cssUrl; }
  public String getUserEmail() { return userEmail; }
}
