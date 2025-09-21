package zone.realfood;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

public class Cookies {

    public static String getCookie(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        String cookieHeader = headers.getOrDefault("Cookie", headers.getOrDefault("cookie", null));
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        String[] parts = cookieHeader.split(";\\s*");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(p -> p.startsWith(name + "="))
                .map(p -> p.substring(name.length() + 1))
                .findFirst()
                .orElse(null);
    }

    public static String buildCookie(String name, String value, Duration maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value == null ? "" : value);
        sb.append("; Path=/");
        // In API Gateway + Lambda behind HTTPS, Secure is safe and recommended
        sb.append("; Secure");
        sb.append("; HttpOnly");
        sb.append("; SameSite=Lax");
        if (maxAge != null) {
            sb.append("; Max-Age=").append(maxAge.getSeconds());
        }
        return sb.toString();
    }
}
