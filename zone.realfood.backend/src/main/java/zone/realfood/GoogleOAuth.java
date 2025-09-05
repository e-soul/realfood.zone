package zone.realfood;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class GoogleOAuth {
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    // Minimal parsing without external libs: naive field extraction by quotes

    public static String getClientId() {
        return System.getenv("GOOGLE_CLIENT_ID");
    }

    public static String getClientSecret() {
        return System.getenv("GOOGLE_CLIENT_SECRET");
    }

    public static String getRedirectUri() {
        return System.getenv("GOOGLE_REDIRECT_URI");
    }

    // Minimal scopes for login: openid email (no profile unless we want picture/name)
    private static String getScope() {
        return url("openid email profile");
    }

    public static String buildAuthorizeUrl(String state) {
        StringBuilder sb = new StringBuilder(AUTH_URL).append("?client_id=").append(url(getClientId())).append("&response_type=code").append("&redirect_uri=")
                .append(url(getRedirectUri())).append("&scope=").append(getScope()).append("&access_type=online").append("&include_granted_scopes=false")
                .append("&state=").append(url(state));
        return sb.toString();
    }

    public static GoogleUser exchangeCodeForUser(String code) throws IOException, InterruptedException {
        String form = "code=" + url(code) + "&client_id=" + url(getClientId()) + "&client_secret=" + url(getClientSecret()) + "&redirect_uri="
                + url(getRedirectUri()) + "&grant_type=authorization_code";

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest tokenReq = HttpRequest.newBuilder().uri(URI.create(TOKEN_URL)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> tokenRes = client.send(tokenReq, HttpResponse.BodyHandlers.ofString());
        if (tokenRes.statusCode() < 200 || tokenRes.statusCode() >= 300) {
            throw new IOException("Token exchange failed: " + tokenRes.statusCode() + " " + tokenRes.body());
        }
        String idToken = extractJsonString(tokenRes.body(), "id_token");
        String accessToken = extractJsonString(tokenRes.body(), "access_token");

        // Use OIDC userinfo with access token for email claim
        HttpRequest userReq = HttpRequest.newBuilder().uri(URI.create(USERINFO_URL)).timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken).GET().build();
        HttpResponse<String> userRes = client.send(userReq, HttpResponse.BodyHandlers.ofString());
        if (userRes.statusCode() < 200 || userRes.statusCode() >= 300) {
            throw new IOException("Userinfo failed: " + userRes.statusCode() + " " + userRes.body());
        }
        String userBody = userRes.body();
        String sub = extractJsonString(userBody, "sub");
        String email = extractJsonString(userBody, "email");
        boolean emailVerified = extractJsonBoolean(userBody, "email_verified");
        // Name and picture might not be present without profile scope
        String name = extractJsonString(userBody, "name");
        String picture = extractJsonString(userBody, "picture");
        return new GoogleUser(sub, email, emailVerified, name, picture, idToken);
    }

    public static String randomState() {
        byte[] b = new byte[18];
        new java.security.SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public record GoogleUser(String sub, String email, boolean emailVerified, String name, String picture, String idToken) {
    }

    private static String extractJsonString(String json, String field) {
        if (json == null)
            return null;
        String needle = quote(field) + ":";
        int i = json.indexOf(needle);
        if (i < 0)
            return null;
        int start = json.indexOf('"', i + needle.length());
        if (start < 0)
            return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0)
            return null;
        return json.substring(start + 1, end);
    }

    private static boolean extractJsonBoolean(String json, String field) {
        if (json == null)
            return false;
        String needle = quote(field) + ":";
        int i = json.indexOf(needle);
        if (i < 0)
            return false;
        int start = i + needle.length();
        int end = json.indexOf(',', start);
        if (end < 0)
            end = json.indexOf('}', start);
        if (end < 0)
            return false;
        String val = json.substring(start, end).trim();
        return "true".equalsIgnoreCase(val);
    }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }
}
