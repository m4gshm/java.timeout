package m4gshm.java.timeout.spring.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;

import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.SECONDS;

@ConfigurationProperties("timeout")
@Data
public class TimeLimitExecutorProperties {

    double connectionToRequestTimeoutRate = 0.1;
    long childDeadlineLag = 100;
    @DurationUnit(SECONDS)
    Duration defaultDeadline;
}
