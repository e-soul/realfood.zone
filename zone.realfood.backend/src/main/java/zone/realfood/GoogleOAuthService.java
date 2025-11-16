package zone.realfood;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Service wrapper for Google OAuth 2.0 + OpenID Connect using Google's official client library.
 * Provides secure authentication with automatic JWT signature verification.
 */
public class GoogleOAuthService {
    
    private static final String CLIENT_ID_PROP = "zone.realfood.google.clientId";
    private static final String CLIENT_SECRET_PROP = "zone.realfood.google.clientSecret";
    private static final String REDIRECT_URI_PROP = "zone.realfood.google.redirectUri";
    
    private final NetHttpTransport httpTransport;
    private final GsonFactory jsonFactory;
    
    public GoogleOAuthService() {
        this.httpTransport = new NetHttpTransport();
        this.jsonFactory = GsonFactory.getDefaultInstance();
    }
    
    /**
     * Get configured client ID from system properties or environment variables.
     */
    public static String getClientId() {
        return System.getProperty(CLIENT_ID_PROP, System.getenv("GOOGLE_CLIENT_ID"));
    }
    
    /**
     * Get configured client secret from system properties or environment variables.
     */
    public static String getClientSecret() {
        return System.getProperty(CLIENT_SECRET_PROP, System.getenv("GOOGLE_CLIENT_SECRET"));
    }
    
    /**
     * Get configured redirect URI from system properties or environment variables.
     */
    public static String getRedirectUri() {
        return System.getProperty(REDIRECT_URI_PROP, System.getenv("GOOGLE_REDIRECT_URI"));
    }
    
    /**
     * Build authorization URL for redirecting user to Google consent screen.
     * 
     * @param state CSRF protection token to be validated on callback
     * @return Complete authorization URL
     */
    public String buildAuthorizeUrl(String state) {
        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                getClientId(),
                getClientSecret(),
                Collections.singletonList("openid email profile")
            ).build();
            
            return flow.newAuthorizationUrl()
                .setRedirectUri(getRedirectUri())
                .setState(state)
                .setAccessType("online")
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build authorization URL", e);
        }
    }
    
    /**
     * Exchange authorization code for user information with full JWT validation.
     * 
     * @param code Authorization code from callback
     * @return Verified user information
     * @throws IOException if token exchange or validation fails
     */
    public GoogleUser exchangeCodeForUser(String code) throws IOException {
        try {
            // Exchange code for tokens
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                jsonFactory,
                getClientId(),
                getClientSecret(),
                code,
                getRedirectUri()
            ).execute();
            
            // Verify and parse ID token with signature validation
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
                .setAudience(Collections.singletonList(getClientId()))
                .build();
            
            GoogleIdToken idToken = verifier.verify(tokenResponse.getIdToken());
            if (idToken == null) {
                throw new IOException("ID token verification failed - invalid signature or claims");
            }
            
            GoogleIdToken.Payload payload = idToken.getPayload();
            
            // Extract user claims
            String sub = payload.getSubject();
            String email = payload.getEmail();
            Boolean emailVerified = payload.getEmailVerified();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            
            return new GoogleUser(
                sub,
                email,
                emailVerified != null ? emailVerified : false,
                name,
                picture,
                tokenResponse.getIdToken()
            );
            
        } catch (GeneralSecurityException e) {
            throw new IOException("Security validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate cryptographically secure random state token for CSRF protection.
     * 
     * @return Base64-URL encoded random state
     */
    public static String randomState() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * User information returned from Google OAuth with verified claims.
     */
    public record GoogleUser(
        String sub,
        String email,
        boolean emailVerified,
        String name,
        String picture,
        String idToken
    ) {}
}
