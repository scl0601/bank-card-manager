package com.bank.admin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final SchemaPatch schemaPatch = new SchemaPatch();
    private final BootstrapUsers bootstrapUsers = new BootstrapUsers();
    private final Cors cors = new Cors();

    @Getter
    @Setter
    public static class SchemaPatch {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class BootstrapUsers {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedOriginPatterns = new ArrayList<>();
    }
}
