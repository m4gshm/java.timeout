package timeout.spring.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.DeadlineExecutor;
import timeout.servlet.ExpiresHeaderFilter;
import timeout.spring.properties.DeadlineExecutorProperties;

import javax.servlet.Filter;

@Configuration
@ConditionalOnClass(Filter.class)
public class ExpiresHeaderFilterAutoConfiguration {

    @Bean
    public ExpiresHeaderFilter expiresHeaderFilter(DeadlineExecutorProperties properties,
                                                   DeadlineExecutor executor) {
        return new ExpiresHeaderFilter(properties.getDefaultDeadline(), executor);
    }
}
