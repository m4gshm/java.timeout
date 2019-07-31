package timeout.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.servlet.ExpiresHeaderFilter;
import timeout.spring.properties.ConnectTimeoutProperties;

@Configuration
public class ExpiresHeaderFilterConfig {

    @Bean
    public ExpiresHeaderFilter expiresHeaderFilter(ConnectTimeoutProperties properties) {
        return new ExpiresHeaderFilter(properties.getDefaultDeadline());
    }
}
