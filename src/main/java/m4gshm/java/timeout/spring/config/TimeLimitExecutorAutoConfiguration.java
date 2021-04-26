package m4gshm.java.timeout.spring.config;

import m4gshm.java.timeout.Clock;
import m4gshm.java.timeout.TimeLimitExecutor;
import m4gshm.java.timeout.TimeLimitExecutorImpl;
import m4gshm.java.timeout.TimeoutsFormula;
import m4gshm.java.timeout.spring.properties.TimeLimitExecutorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
@EnableConfigurationProperties(TimeLimitExecutorProperties.class)
public class TimeLimitExecutorAutoConfiguration {

    @Bean
    TimeLimitExecutor deadlineExecutor(TimeLimitExecutorProperties properties) {
        Clock<Instant> now = Clock.now();
        return new TimeLimitExecutorImpl(now, TimeoutsFormula.rateForDeadline(properties.getConnectionToRequestTimeoutRate(), now));
    }

}
