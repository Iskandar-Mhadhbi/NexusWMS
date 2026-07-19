package com.nexuswms.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Api api = new Api();
    
    private Cors cors = new Cors();
    public Jwt getJwt() { return jwt; }
    public Api getApi() { return api; } 
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public static class Jwt {
        private String secret;
        private long expirationMs;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }
    public static class Api {
        private String version;

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
 
    public static class Cors {
        private List<String> allowedOrigins;

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }
}