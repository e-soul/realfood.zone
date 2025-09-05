package zone.realfood;

public class LoginModel {
  private final String title;
  private final String cssUrl;
  private final String googleAuthPath;

  public LoginModel(String title, String cssUrl, String googleAuthPath) {
    this.title = title;
    this.cssUrl = cssUrl;
    this.googleAuthPath = googleAuthPath;
  }

  public String getTitle() { return title; }
  public String getCssUrl() { return cssUrl; }
  public String getGoogleAuthPath() { return googleAuthPath; }
}
