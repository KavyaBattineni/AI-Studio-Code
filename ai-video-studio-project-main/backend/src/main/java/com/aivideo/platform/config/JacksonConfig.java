package com.aivideo.platform.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Most Jackson behavior is already controlled via the spring.jackson.* properties in
 * application.properties (date format, null-field inclusion, etc.) - Spring Boot's
 * auto-configured ObjectMapper reads those directly and already registers JavaTimeModule
 * (for Instant) and full record support out of the box.
 *
 * This class exists as the place to add further Jackson customization (e.g. a custom
 * serializer/deserializer) later. It intentionally customizes Spring Boot's auto-configured
 * builder via Jackson2ObjectMapperBuilderCustomizer rather than replacing the ObjectMapper
 * bean outright, so nothing Spring Boot wires in by default is lost.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonBuilderCustomizer() {
        // No overrides needed beyond the spring.jackson.* properties today - this bean is a
        // deliberate extension point, kept even though it's currently a no-op customizer.
        return builder -> {
        };
    }
}
