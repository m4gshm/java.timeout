package timeout.spring.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.TimeLimitExecutor;
import timeout.servlet.DeadlineHeaderFilter;
import timeout.spring.properties.TimeLimitExecutorProperties;

import javax.servlet.Filter;

@Configuration
@ConditionalOnClass(Filter.class)
public class ExpiresHeaderFilterAutoConfiguration {

    @Bean
    public DeadlineHeaderFilter expiresHeaderFilter(TimeLimitExecutorProperties properties,
                                                    TimeLimitExecutor executor) {
        return new DeadlineHeaderFilter(properties.getDefaultDeadline(), executor);
    }
}
