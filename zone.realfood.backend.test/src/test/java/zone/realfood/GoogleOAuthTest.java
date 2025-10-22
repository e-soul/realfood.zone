package zone.realfood;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
// no additional imports required after refactor
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests GoogleOAuth without reaching real Google endpoints by using the local server endpoints.
 */
public class GoogleOAuthTest {

    private static final int MAIN_SERVER_PORT = 8050; // Port used by Main
    private static final AtomicBoolean environmentPrepared = new AtomicBoolean(false);

    @BeforeAll
    static void prepareEnvironment() {
        Assumptions.assumeTrue(isMainServerUp(), () -> "Main server must be running on port " + MAIN_SERVER_PORT + " (run devenv.bat)." );
        System.setProperty("zone.realfood.google.token.url", "http://localhost:" + MAIN_SERVER_PORT + "/__test/google-oauth/token");
        System.setProperty("zone.realfood.google.userinfo.url", "http://localhost:" + MAIN_SERVER_PORT + "/__test/google-oauth/userinfo");
        System.setProperty("zone.realfood.google.clientId", "test-client-id");
        System.setProperty("zone.realfood.google.clientSecret", "test-client-secret");
        System.setProperty("zone.realfood.google.redirectUri", "http://localhost/redirect");
        environmentPrepared.set(true);
    }

    private static boolean isMainServerUp() {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", MAIN_SERVER_PORT), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

	@Test
	@DisplayName("Successful code exchange and userinfo retrieval")
	void testExchangeSuccess() throws Exception {
		if (!environmentPrepared.get()) prepareEnvironment();
		String code = "abc123";
		GoogleOAuth.GoogleUser user = GoogleOAuth.exchangeCodeForUser(code);
		assertNotNull(user);
		assertEquals("sub-" + code, user.sub());
		assertEquals(code + "@example.test", user.email());
		assertTrue(user.emailVerified());
		assertEquals("Test User " + code, user.name());
		assertEquals("http://localhost/pic-" + code + ".png", user.picture());
		assertEquals("idtoken-" + code, user.idToken());
	}

	@Test
	@DisplayName("Token endpoint returns error for missing code")
	void testTokenError() {
		// Directly call low-level endpoint with missing code to ensure 400 then expect GoogleOAuth to fail when using blank code
		if (!environmentPrepared.get()) prepareEnvironment();
		assertThrows(IOException.class, () -> GoogleOAuth.exchangeCodeForUser(""));
	}

	@Test
	@DisplayName("Userinfo error propagates")
	void testUserinfoError() {
		// Simulate token success but tamper access token by pointing userinfo to require pattern
		// We cannot easily modify token JSON here; easiest approach: override userinfo URL to a bad one returning 404
        if (!environmentPrepared.get()) prepareEnvironment();
        System.setProperty("zone.realfood.google.userinfo.url", "http://localhost:" + MAIN_SERVER_PORT + "/__test/google-oauth/does-not-exist");
		try {
			assertThrows(IOException.class, () -> GoogleOAuth.exchangeCodeForUser("codeForUserinfoFail"));
		} finally {
			System.setProperty("zone.realfood.google.userinfo.url", "http://localhost:" + MAIN_SERVER_PORT + "/__test/google-oauth/userinfo");
		}
	}

	@Test
	@DisplayName("Random state uniqueness and length")
	void testRandomState() {
		String a = GoogleOAuth.randomState();
		String b = GoogleOAuth.randomState();
		assertNotEquals(a, b);
		assertTrue(a.length() >= 10); // 18 bytes base64url without padding will be >10
	}
}
