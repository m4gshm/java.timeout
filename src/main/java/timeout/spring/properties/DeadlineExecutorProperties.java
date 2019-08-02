package timeout.spring.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.SECONDS;

@ConfigurationProperties("deadline")
@Data
public class DeadlineExecutorProperties {

    double connectionToRequestTimeoutRate = 0.3;
    long childDeadlineLag = 200;
    @DurationUnit(SECONDS)
    Duration defaultDeadline;
}
