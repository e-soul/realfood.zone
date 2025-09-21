package zone.realfood;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CookiesTest {

    private Map<String, String> headers() {
        return new LinkedHashMap<>(); // preserve insertion order for duplicate name test
    }

    @Nested
    @DisplayName("getCookie")
    class GetCookieTests {
        @Test
        void returnsNullWhenHeadersNull() {
            assertNull(Cookies.getCookie(null, "foo"));
        }

        @Test
        void returnsNullWhenCookieHeaderMissing() {
            Map<String, String> h = headers();
            h.put("X-Other", "value");
            assertNull(Cookies.getCookie(h, "foo"));
        }

        @Test
        void returnsNullWhenCookieHeaderBlank() {
            Map<String, String> h = headers();
            h.put("Cookie", "   ");
            assertNull(Cookies.getCookie(h, "foo"));
        }

        @Test
        void findsSingleCookie() {
            Map<String, String> h = headers();
            h.put("Cookie", "foo=bar");
            assertEquals("bar", Cookies.getCookie(h, "foo"));
        }

        @Test
        void findsAmongMultipleCookies() {
            Map<String, String> h = headers();
            h.put("Cookie", "a=1; foo=bar; c=3");
            assertEquals("bar", Cookies.getCookie(h, "foo"));
        }

        @Test
        void caseInsensitiveHeaderKey() {
            Map<String, String> h = headers();
            h.put("cookie", "foo=bar"); // lowercase key
            assertEquals("bar", Cookies.getCookie(h, "foo"));
        }

        @Test
        void emptyCookieValue() {
            Map<String, String> h = headers();
            h.put("Cookie", "foo=");
            assertEquals("", Cookies.getCookie(h, "foo"));
        }

        @Test
        void valueContainingEqualsSigns() {
            Map<String, String> h = headers();
            h.put("Cookie", "token=abc==; other=x");
            assertEquals("abc==", Cookies.getCookie(h, "token"));
        }

        @Test
        void duplicateCookieNamesReturnsFirst() {
            Map<String, String> h = headers();
            // order preserved by LinkedHashMap, simulate duplicate name segments
            h.put("Cookie", "foo=first; foo=second; bar=3");
            assertEquals("first", Cookies.getCookie(h, "foo"));
        }
    }

    @Nested
    @DisplayName("buildCookie")
    class BuildCookieTests {
        @Test
        void buildsBasicCookie() {
            String c = Cookies.buildCookie("foo", "bar", null);
            assertTrue(c.startsWith("foo=bar; Path=/"));
            assertTrue(c.contains("Secure"));
            assertTrue(c.contains("HttpOnly"));
            assertTrue(c.contains("SameSite=Lax"));
            assertFalse(c.contains("Max-Age"));
        }

        @Test
        void nullValueBecomesEmptyString() {
            String c = Cookies.buildCookie("foo", null, null);
            assertTrue(c.startsWith("foo=; Path=/"));
        }

        @Test
        void includesMaxAge() {
            String c = Cookies.buildCookie("foo", "bar", Duration.ofMinutes(5));
            assertTrue(c.contains("Max-Age=" + 300));
        }

        @Test
        void zeroMaxAge() {
            String c = Cookies.buildCookie("foo", "bar", Duration.ZERO);
            assertTrue(c.contains("Max-Age=0"));
        }
    }
}
