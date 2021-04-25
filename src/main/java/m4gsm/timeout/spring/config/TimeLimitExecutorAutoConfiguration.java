package m4gsm.timeout.spring.config;

import m4gsm.timeout.Clock;
import m4gsm.timeout.TimeLimitExecutor;
import m4gsm.timeout.TimeLimitExecutorImpl;
import m4gsm.timeout.TimeoutsFormula;
import m4gsm.timeout.spring.properties.TimeLimitExecutorProperties;
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
