package zone.realfood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import zone.realfood.db.DynamoDbTools;

/**
 * Unit tests for {@link MainHandler}. These tests exercise routing logic and response generation. They assume a local DynamoDB instance is running (same
 * assumption strategy as {@link DynamoDbTest}).
 */
public class MainHandlerTest {

	private static final TestContext TEST_CONTEXT = new TestContext();
	private static final String TEST_USER_ID = "test-user-123";
	private static final String TEST_SESSION_ID = "test-session-xyz";

	@BeforeAll
	static void beforeAll() {
		Fixtures.configureDynamoDbAccess();

		Assumptions.assumeTrue(Fixtures.isDynamoDbLocalRunning(), () -> "Local DynamoDB must be running.");

		// Seed a test user with valid session
		DynamoDbTable<UserProfile> userProfileTable = DynamoDbTools.initDynamoDbUserProfileTable();
		UserProfile testUser = new UserProfile();
		testUser.setUserId(TEST_USER_ID);
		testUser.setEmail("test@example.com");
		testUser.setName("Test User");
		testUser.setSessionId(TEST_SESSION_ID);
		testUser.setSessionExpiresAt(Instant.now().plus(Duration.ofDays(30)));
		userProfileTable.putItem(testUser);
	}

	private MainHandler newHandler() {
		return new MainHandler();
	}

	private APIGatewayProxyRequestEvent request(String path) {
		return new APIGatewayProxyRequestEvent().withPath(path);
	}

	@Test
	@DisplayName("GET / returns index page HTML")
	void rootPathReturnsIndex() {
		MainHandler h = newHandler();
		APIGatewayProxyResponseEvent res = h.handleRequest(request("/"), TEST_CONTEXT);
		assertEquals(200, res.getStatusCode());
		assertTrue(res.getBody().contains("Hi, Stranger!"), "Index page should greet stranger when no session");
		assertEquals("text/html; charset=utf-8", res.getMultiValueHeaders().get("Content-Type").get(0));
	}

	@Test
	@DisplayName("GET /login returns login page when no sid cookie")
	void loginPageWithoutSession() {
		MainHandler h = newHandler();
		APIGatewayProxyResponseEvent res = h.handleRequest(request("/login"), TEST_CONTEXT);
		assertEquals(200, res.getStatusCode());
		assertTrue(res.getBody().contains("Sign in with Google"));
	}

	@Test
	@DisplayName("GET /login redirects to / when sid cookie present")
	void loginRedirectsWhenSession() {
		MainHandler h = newHandler();
		APIGatewayProxyRequestEvent req = request("/login");
		// Use the test user seeded in beforeAll() with valid session
		String cookie = Cookies.buildCookie("sid", TEST_USER_ID + ":" + TEST_SESSION_ID, Duration.ofDays(30));
		req.setMultiValueHeaders(Map.of("Cookie", List.of(cookie)));
		APIGatewayProxyResponseEvent res = h.handleRequest(req, TEST_CONTEXT);
		assertEquals(302, res.getStatusCode());
		assertEquals("/", res.getMultiValueHeaders().get("Location").get(0));
	}

	@Test
	@DisplayName("GET /privacy returns privacy policy page")
	void privacyPage() {
		MainHandler h = newHandler();
		APIGatewayProxyResponseEvent res = h.handleRequest(request("/privacy"), TEST_CONTEXT);
		assertEquals(200, res.getStatusCode());
		assertTrue(res.getBody().contains("Privacy Policy"));
	}

	@Test
	@DisplayName("GET /terms returns terms page")
	void termsPage() {
		MainHandler h = newHandler();
		APIGatewayProxyResponseEvent res = h.handleRequest(request("/terms"), TEST_CONTEXT);
		assertEquals(200, res.getStatusCode());
		assertTrue(res.getBody().contains("Terms of Service"));
	}

	@Test
	@DisplayName("Unknown path returns 404")
	void unknownPath() {
		MainHandler h = newHandler();
		APIGatewayProxyResponseEvent res = h.handleRequest(request("/does-not-exist"), TEST_CONTEXT);
		assertEquals(404, res.getStatusCode());
	}

	@Test
	@DisplayName("GET /auth/google returns 500 when OAuth not configured")
	void googleAuthNotConfigured() {
		// Ensure no properties are set (clear any leftovers from other tests)
		System.clearProperty("zone.realfood.google.clientId");
		System.clearProperty("zone.realfood.google.clientSecret");
		System.clearProperty("zone.realfood.google.redirectUri");

		MainHandler h = newHandler();
		APIGatewayProxyResponseEvent res = h.handleRequest(request("/auth/google"), TEST_CONTEXT);
		assertEquals(500, res.getStatusCode());
		assertTrue(res.getBody().contains("Google OAuth is not configured"));
	}

	@Test
	@DisplayName("GET /auth/google sets state cookie and redirects when configured")
	void googleAuthRedirect() {
		// Configure minimal OAuth properties
		System.setProperty("zone.realfood.google.clientId", "test-client-id");
		System.setProperty("zone.realfood.google.clientSecret", "test-client-secret");
		System.setProperty("zone.realfood.google.redirectUri", "https://example.com/callback");

		MainHandler h = newHandler();
		APIGatewayProxyResponseEvent res = h.handleRequest(request("/auth/google"), TEST_CONTEXT);
		assertEquals(302, res.getStatusCode());
		String location = res.getMultiValueHeaders().get("Location").get(0);
		assertNotNull(location);
		assertTrue(location.startsWith("https://accounts.google.com/o/oauth2"), "Should redirect to Google auth domain");
		assertNotNull(res.getMultiValueHeaders());
		assertTrue(res.getMultiValueHeaders().get("Set-Cookie").stream().anyMatch(c -> c.startsWith("g_state=")), "State cookie should be set");
	}

	@Test
	@DisplayName("GET /auth/google/callback returns 400 with invalid/missing state")
	void googleCallbackInvalidState() {
		// Provide OAuth config so handler attempts validation
		System.setProperty("zone.realfood.google.clientId", "test-client-id");
		System.setProperty("zone.realfood.google.clientSecret", "test-client-secret");
		System.setProperty("zone.realfood.google.redirectUri", "https://example.com/callback");

		MainHandler h = newHandler();
		APIGatewayProxyRequestEvent req = request("/auth/google/callback");
		Map<String, String> query = new HashMap<>();
		query.put("code", "dummyCode");
		query.put("state", "STATE_FROM_QUERY");
		req.setQueryStringParameters(query);
		// Intentionally no matching g_state cookie
		req.setHeaders(Map.of());
		APIGatewayProxyResponseEvent res = h.handleRequest(req, TEST_CONTEXT);
		assertEquals(400, res.getStatusCode());
		assertTrue(res.getBody().contains("Invalid OAuth state"));
	}

	// Minimal stub Context; expand as necessary
	private static class TestContext implements Context {
		@Override
		public String getAwsRequestId() {
			return "req-1";
		}

		@Override
		public String getLogGroupName() {
			return "log-group";
		}

		@Override
		public String getLogStreamName() {
			return "log-stream";
		}

		@Override
		public String getFunctionName() {
			return "function";
		}

		@Override
		public String getFunctionVersion() {
			return "1";
		}

		@Override
		public String getInvokedFunctionArn() {
			return "arn:aws:lambda:eu-central-1:123:function:function";
		}

		@Override
		public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() {
			return null;
		}

		@Override
		public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() {
			return null;
		}

		@Override
		public int getRemainingTimeInMillis() {
			return 30000;
		}

		@Override
		public int getMemoryLimitInMB() {
			return 512;
		}

		@Override
		public com.amazonaws.services.lambda.runtime.LambdaLogger getLogger() {
			return new com.amazonaws.services.lambda.runtime.LambdaLogger() {
				@Override
				public void log(String message) {
					/* no-op */ }

				@Override
				public void log(byte[] message) {
					/* no-op */ }
			};
		}
	}
}
