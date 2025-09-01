package zone.realfood;

public class IndexModel {
  private final String title;
  private final String greeting;
  private final String name;
  private final String cssUrl;
  private final String profile;
  private final String userId;
  private final String error;

  public IndexModel(String title, String greeting, String name, String cssUrl, String profile, String userId, String error) {
    this.title = title;
    this.greeting = greeting;
    this.name = name;
    this.cssUrl = cssUrl;
    this.profile = profile;
    this.userId = userId;
    this.error = error;
  }

  public String getTitle() { return title; }
  public String getGreeting() { return greeting; }
  public String getName() { return name; }
  public String getCssUrl() { return cssUrl; }
  public String getProfile() { return profile; }
  public String getUserId() { return userId; }
  public String getError() { return error; }
}
