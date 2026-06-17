package com.linkedinagent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String env = "development";
    private String secretKey;
    private String frontendUrl = "http://localhost:5173";
    private String backendUrl = "http://localhost:8080";

    private final Jwt jwt = new Jwt();
    private final Supabase supabase = new Supabase();
    private final Gemini gemini = new Gemini();
    private final LinkedIn linkedin = new LinkedIn();
    private final Tavily tavily = new Tavily();
    private final Encryption encryption = new Encryption();

    @Getter
    @Setter
    public static class Jwt {
        private long accessTokenExpiration = 900_000L;
        private long refreshTokenExpiration = 604_800_000L;
    }

    @Getter
    @Setter
    public static class Supabase {
        private String url;
        private String serviceKey;
        private String storageBucket = "linkedin-ai-images";
    }

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey;
        private String model = "gemini-2.0-flash";
        private int rpmLimit = 12;
        private int dailyLimit = 1400;
    }

    @Getter
    @Setter
    public static class LinkedIn {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
    }

    @Getter
    @Setter
    public static class Tavily {
        private String apiKey;
    }

    @Getter
    @Setter
    public static class Encryption {
        private String aesKey;
    }
}
