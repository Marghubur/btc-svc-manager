package bt.conference.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthCookieFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String RefreshTokenFieldName = "refreshToken";
    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Skip this filter entirely for any request that does NOT start with /api/auth/
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(AUTH_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingResponseWrapper wrappedResponse =
                new ContentCachingResponseWrapper(response);

        // BEFORE: extract refreshToken from cookie and set as request attribute
        // so downstream services (e.g. regenerateToken) can access it
        extractRefreshTokenFromCookie(request);

        // Execute next filter/controller
        filterChain.doFilter(request, wrappedResponse);

        // AFTER: process the response
        String path = request.getRequestURI();
        String responseBody = new String(
                wrappedResponse.getContentAsByteArray(),
                response.getCharacterEncoding()
        );

        if (wrappedResponse.getStatus() == 200
                && !path.toLowerCase().contains("logout")) {
            // For successful auth responses (login, token regeneration, etc.)
            // extract RefreshToken from ApiAuthResponse, set it as HttpOnly cookie,
            // and strip it from the response body
            responseBody = handleAuthResponse(response, responseBody);
        } else if (path.toLowerCase().contains("logout")) {
            // For logout, clear the refresh token cookie
            deleteRefreshTokenCookie(response);
        }

        // Write modified response back
        wrappedResponse.resetBuffer();
        wrappedResponse.getOutputStream()
                .write(responseBody.getBytes(response.getCharacterEncoding()));

        wrappedResponse.copyBodyToResponse();
    }

    /**
     * Read refreshToken from cookie and set as request attribute
     * so the regenerateToken service can access it.
     */
    private void extractRefreshTokenFromCookie(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null)
            return;

        for (Cookie cookie : cookies) {

            if (RefreshTokenFieldName.equals(cookie.getName())) {

                request.setAttribute(RefreshTokenFieldName, cookie.getValue());
                break;
            }
        }
    }

    /**
     * Extract RefreshToken from the ApiAuthResponse JSON (root level),
     * set it as an HttpOnly cookie, and remove it from the response body.
     *
     * ApiAuthResponse JSON structure:
     * {
     *   "ResponseBody": { ... },
     *   "AccessToken": "...",
     *   "RefreshToken": "..."    <-- at root level
     * }
     */
    private String handleAuthResponse(HttpServletResponse response,
                                      String responseBody) {

        try {

            JsonNode root = objectMapper.readTree(responseBody);

            // RefreshToken is a root-level field in ApiAuthResponse
            JsonNode refreshTokenNode = root.get(RefreshTokenFieldName);

            if (refreshTokenNode == null
                    || refreshTokenNode.isNull()
                    || refreshTokenNode.asText().isBlank()) {
                return responseBody;
            }

            String refreshToken = refreshTokenNode.asText();

            // Set HttpOnly cookie on the response
            setRefreshTokenCookie(response, refreshToken);

            // Remove RefreshToken from response JSON so it's not exposed to the client
            ObjectNode cleanRoot = (ObjectNode) root;
            cleanRoot.remove(RefreshTokenFieldName);

            return objectMapper.writeValueAsString(cleanRoot);

        } catch (Exception ex) {
            return responseBody;
        }
    }

    /**
     * Set secure HttpOnly refresh token cookie
     */
    private void setRefreshTokenCookie(HttpServletResponse response,
                                       String token) {

        boolean isProd = activeProfile.equalsIgnoreCase("prod")
                || activeProfile.equalsIgnoreCase("production");

        String sameSite = isProd ? "Strict" : "Lax";

        response.addHeader(HttpHeaders.SET_COOKIE,
                String.format(
                        "refreshToken=%s; Max-Age=%d; Path=/api/auth; HttpOnly; %sSameSite=%s",
                        token,
                        Duration.ofDays(7).getSeconds(),
                        isProd ? "Secure; " : "",
                        sameSite
                ));
    }

    /**
     * Delete refresh token cookie
     */
    private void deleteRefreshTokenCookie(HttpServletResponse response) {

        response.addHeader(HttpHeaders.SET_COOKIE,
                RefreshTokenFieldName + "=; Max-Age=0; Path=/api/auth; HttpOnly; SameSite=Lax");
    }
}