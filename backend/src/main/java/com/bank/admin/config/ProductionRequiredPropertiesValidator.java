package com.bank.admin.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductionRequiredPropertiesValidator implements InitializingBean {

    private static final List<RequiredProperty> REQUIRED_PROPERTIES = List.of(
            new RequiredProperty("JWT_SECRET", "jwt.secret"),
            new RequiredProperty("SPRING_DATASOURCE_URL", "spring.datasource.url"),
            new RequiredProperty("SPRING_DATASOURCE_USERNAME", "spring.datasource.username"),
            new RequiredProperty("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password")
    );

    private final Environment environment;

    public ProductionRequiredPropertiesValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        List<String> missing = REQUIRED_PROPERTIES.stream()
                .filter(property -> isBlankOrUnresolved(environment.getProperty(property.propertyKey())))
                .map(RequiredProperty::environmentName)
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Production profile requires environment variables: " + String.join(", ", missing));
        }
    }

    private boolean isBlankOrUnresolved(String value) {
        return value == null || value.isBlank() || value.startsWith("${");
    }

    private record RequiredProperty(String environmentName, String propertyKey) {
    }
}
