package zone.realfood;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

public class SessionTools {

    public static UserProfile getUserProfileFromSession(Map<String, String> headers, DynamoDbTable<UserProfile> userProfileTable) {
        String sid = Cookies.getCookie(headers, "sid");
        if (sid == null || sid.isBlank()) {
            return null;
        }
        String[] parts = sid.split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        String userId = parts[0];
        String sessionId = parts[1];

        UserProfile userProfile = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(userId)));
        if (userProfile != null) {
            boolean sessionMatches = sessionId.equals(userProfile.getSessionId());
            boolean notExpired = userProfile.getSessionExpiresAt() != null && userProfile.getSessionExpiresAt().isAfter(Instant.now());
            if (sessionMatches && notExpired) {
                return userProfile;
            }
        }
        return null;
    }

    public static String createSession(UserProfile userProfile, DynamoDbTable<UserProfile> userProfileTable) {
        String sessionId = generateSessionId();
        userProfile.setSessionId(sessionId);
        userProfile.setSessionExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        userProfileTable.putItem(userProfile);
        return Cookies.buildCookie("sid", userProfile.getUserId() + ":" + sessionId, Duration.ofDays(30));
    }

    public static void invalidateSession(Map<String, String> headers, DynamoDbTable<UserProfile> userProfileTable) {
        // We manually parse here to ensure we clear the session even if it's expired,
        // as long as the session ID matches what's in the DB.
        String sid = Cookies.getCookie(headers, "sid");
        if (sid != null && !sid.isBlank()) {
            String[] parts = sid.split(":", 2);
            if (parts.length == 2) {
                String userId = parts[0];
                String sessionId = parts[1];
                UserProfile profile = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(userId)));
                if (profile != null && sessionId.equals(profile.getSessionId())) {
                    profile.setSessionId(null);
                    profile.setSessionExpiresAt(null);
                    userProfileTable.putItem(profile);
                }
            }
        }
    }

    private static String generateSessionId() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
