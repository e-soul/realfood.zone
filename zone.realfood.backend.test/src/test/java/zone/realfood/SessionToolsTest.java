package zone.realfood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import zone.realfood.db.DynamoDbTools;

public class SessionToolsTest {

    private static DynamoDbTable<UserProfile> userProfileTable;

    @BeforeAll
    static void beforeAll() {
        Fixtures.configureDynamoDbAccess();

        Assumptions.assumeTrue(Fixtures.isDynamoDbLocalRunning(), "Local DynamoDB must be running.");

        userProfileTable = DynamoDbTools.initDynamoDbUserProfileTable();
    }

    @Test
    void testGetUserProfileFromSession_Valid() {
        String userId = "user-valid";
        String sessionId = "session-valid";
        UserProfile user = new UserProfile();
        user.setUserId(userId);
        user.setSessionId(sessionId);
        user.setSessionExpiresAt(Instant.now().plus(Duration.ofHours(1)));
        userProfileTable.putItem(user);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "sid=" + userId + ":" + sessionId);

        UserProfile result = SessionTools.getUserProfileFromSession(headers, userProfileTable);
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void testGetUserProfileFromSession_Expired() {
        String userId = "user-expired";
        String sessionId = "session-expired";
        UserProfile user = new UserProfile();
        user.setUserId(userId);
        user.setSessionId(sessionId);
        user.setSessionExpiresAt(Instant.now().minus(Duration.ofHours(1)));
        userProfileTable.putItem(user);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "sid=" + userId + ":" + sessionId);

        UserProfile result = SessionTools.getUserProfileFromSession(headers, userProfileTable);
        assertNull(result);
    }

    @Test
    void testGetUserProfileFromSession_InvalidSessionId() {
        String userId = "user-invalid-sid";
        String sessionId = "session-real";
        UserProfile user = new UserProfile();
        user.setUserId(userId);
        user.setSessionId(sessionId);
        user.setSessionExpiresAt(Instant.now().plus(Duration.ofHours(1)));
        userProfileTable.putItem(user);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "sid=" + userId + ":wrong-session-id");

        UserProfile result = SessionTools.getUserProfileFromSession(headers, userProfileTable);
        assertNull(result);
    }

    @Test
    void testGetUserProfileFromSession_NoCookie() {
        Map<String, String> headers = new HashMap<>();
        UserProfile result = SessionTools.getUserProfileFromSession(headers, userProfileTable);
        assertNull(result);
    }

    @Test
    void testCreateSession() {
        String userId = "user-create-session";
        UserProfile user = new UserProfile();
        user.setUserId(userId);
        // Initially no session
        userProfileTable.putItem(user);

        String cookieString = SessionTools.createSession(user, userProfileTable);

        assertNotNull(cookieString);
        assertTrue(cookieString.contains("sid="));

        // Verify DB update
        UserProfile updatedUser = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(userId)));
        assertNotNull(updatedUser.getSessionId());
        assertNotNull(updatedUser.getSessionExpiresAt());
        assertTrue(updatedUser.getSessionExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void testInvalidateSession() {
        String userId = "user-invalidate";
        String sessionId = "session-invalidate";
        UserProfile user = new UserProfile();
        user.setUserId(userId);
        user.setSessionId(sessionId);
        user.setSessionExpiresAt(Instant.now().plus(Duration.ofHours(1)));
        userProfileTable.putItem(user);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "sid=" + userId + ":" + sessionId);

        SessionTools.invalidateSession(headers, userProfileTable);

        UserProfile updatedUser = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(userId)));
        assertNull(updatedUser.getSessionId());
        assertNull(updatedUser.getSessionExpiresAt());
    }
}
