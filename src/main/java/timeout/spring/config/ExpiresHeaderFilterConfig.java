package timeout.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.DeadlineExecutor;
import timeout.servlet.ExpiresHeaderFilter;
import timeout.spring.properties.DeadlineExecutorProperties;

@Configuration
public class ExpiresHeaderFilterConfig {

    @Bean
    public ExpiresHeaderFilter expiresHeaderFilter(DeadlineExecutorProperties properties,
                                                   DeadlineExecutor executor) {
        return new ExpiresHeaderFilter(properties.getDefaultDeadline(), executor);
    }
}
