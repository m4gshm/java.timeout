package timeout.spring.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.TimeLimitExecutor;
import timeout.TimeLimitExecutorImpl;
import timeout.Clock;
import timeout.spring.properties.TimeLimitExecutorProperties;

import java.time.Duration;
import java.time.Instant;

import static timeout.Clock.now;
import static timeout.TimeoutsFormula.rateForDeadline;

@Configuration
@EnableConfigurationProperties(TimeLimitExecutorProperties.class)
public class TimeLimitExecutorAutoConfiguration {

    @Bean
    TimeLimitExecutor<Instant, Duration> deadlineExecutor(TimeLimitExecutorProperties properties) {
        Clock<Instant> now = now();
        return new TimeLimitExecutorImpl(now, rateForDeadline(properties.getConnectionToRequestTimeoutRate(), now));
    }

}
