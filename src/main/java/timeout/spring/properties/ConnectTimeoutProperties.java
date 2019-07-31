package timeout.spring.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties("connect.timeout")
@Data
public class ConnectTimeoutProperties {

    double connectionToRequestTimeoutRate = 0.3;
    @DurationUnit(ChronoUnit.SECONDS)
    Duration defaultDeadline = Duration.ofSeconds(30);
}
