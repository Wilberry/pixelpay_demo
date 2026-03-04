package com.pixelwallet.dto;

/**
 * Data Transfer Object for authentication response data.
 * <p>
 * <strong>Purpose:</strong>
 * Returned after successful user login or registration. Contains the JWT access token
 * and metadata required for subsequent authenticated API calls.
 * </p>
 * <p>
 * <strong>Response Structure:</strong>
 * <ul>
 *   <li>accessToken: JWT token for API authentication</li>
 *   <li>tokenType: Always "Bearer" for JWT tokens</li>
 *   <li>expiresIn: Token validity duration in seconds</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Usage:</strong>
 * <ul>
 *   <li>POST /api/auth/login response</li>
 *   <li>POST /api/auth/register response</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Client Integration:</strong>
 * Clients should include the token in Authorization header:
 * <pre>
 * Authorization: Bearer {accessToken}
 * </pre>
 * </p>
 * <p>
 * <strong>Token Expiration:</strong>
 * <ul>
 *   <li>Tokens expire after 24 hours (86400 seconds)</li>
 *   <li>Clients should handle token refresh or re-authentication</li>
 *   <li>Server validates token expiration on each request</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Builder Pattern:</strong>
 * Provides fluent builder API for convenient object construction:
 * <pre>
 * AuthResponseDTO response = AuthResponseDTO.builder()
 *     .accessToken("eyJhbGciOiJIUzI1NiIs...")
 *     .expiresIn(86400)
 *     .build();
 * </pre>
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
public class AuthResponseDTO {

    /**
     * JWT access token for API authentication.
     * <p>
     * Contains user identity and claims, signed with HMAC-SHA512.
     * Must be included in Authorization header for protected endpoints.
     * </p>
     */
    private String accessToken;

    /**
     * Type of the access token.
     * <p>
     * Always "Bearer" for JWT tokens as per OAuth 2.0 specification.
     * Used by clients to construct proper Authorization headers.
     * </p>
     */
    private String tokenType;

    /**
     * Token expiration time in seconds.
     * <p>
     * Number of seconds until the token expires.
     * Current implementation uses 24 hours (86400 seconds).
     * </p>
     */
    private long expiresIn;

    /**
     * Default constructor for frameworks and deserialization.
     */
    public AuthResponseDTO() {}

    /**
     * Constructor with access token and expiration.
     * <p>
     * Sets tokenType to default "Bearer" value.
     * </p>
     *
     * @param accessToken The JWT access token
     * @param expiresIn Token expiration time in seconds
     */
    public AuthResponseDTO(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }

    /**
     * Constructor with all fields.
     *
     * @param accessToken The JWT access token
     * @param tokenType The token type (usually "Bearer")
     * @param expiresIn Token expiration time in seconds
     */
    public AuthResponseDTO(String accessToken, String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    /**
     * Gets the JWT access token.
     *
     * @return The access token string
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the JWT access token.
     *
     * @param accessToken The access token to set
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Gets the token type.
     *
     * @return The token type (e.g., "Bearer")
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the token type.
     *
     * @param tokenType The token type to set
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Gets the token expiration time in seconds.
     *
     * @return Expiration time in seconds
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the token expiration time in seconds.
     *
     * @param expiresIn Expiration time in seconds
     */
    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Creates a new builder instance for fluent construction.
     *
     * @return A new AuthResponseDTOBuilder instance
     */
    public static AuthResponseDTOBuilder builder() {
        return new AuthResponseDTOBuilder();
    }

    /**
     * Builder class for fluent AuthResponseDTO construction.
     * <p>
     * Provides a fluent API for creating AuthResponseDTO instances
     * with method chaining and optional field setting.
     * </p>
     */
    public static class AuthResponseDTOBuilder {
        private String accessToken;
        private String tokenType = "Bearer";
        private long expiresIn;

        /**
         * Sets the access token for the builder.
         *
         * @param accessToken The JWT access token
         * @return This builder instance for method chaining
         */
        public AuthResponseDTOBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        /**
         * Sets the token type for the builder.
         *
         * @param tokenType The token type (default: "Bearer")
         * @return This builder instance for method chaining
         */
        public AuthResponseDTOBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        /**
         * Sets the expiration time for the builder.
         *
         * @param expiresIn Token expiration time in seconds
         * @return This builder instance for method chaining
         */
        public AuthResponseDTOBuilder expiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        /**
         * Builds the AuthResponseDTO instance.
         *
         * @return A new AuthResponseDTO with the configured values
         */
        public AuthResponseDTO build() {
            return new AuthResponseDTO(accessToken, tokenType, expiresIn);
        }
    }
}
