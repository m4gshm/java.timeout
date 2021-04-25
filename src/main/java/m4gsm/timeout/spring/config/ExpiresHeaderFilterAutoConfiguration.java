package m4gsm.timeout.spring.config;

import m4gsm.timeout.TimeLimitExecutor;
import m4gsm.timeout.servlet.DeadlineHeaderFilter;
import m4gsm.timeout.spring.properties.TimeLimitExecutorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
