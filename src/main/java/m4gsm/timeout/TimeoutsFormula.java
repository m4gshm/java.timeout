package m4gsm.timeout;

import lombok.val;

import java.time.Duration;
import java.time.Instant;

import static java.time.Duration.between;
import static java.time.Duration.ofMillis;

public interface TimeoutsFormula {
    static TimeoutsFormula rateForDeadline(double connectionToRequestTimeoutRate, Clock<Instant> clock) {
        return new TimeoutsFormula() {
            @Override
            public <T> T calc(Instant deadline, TimeLimitExecutor.TimeoutsFunction<T> timeoutsApplier) {
                val time = clock.time();
                Duration connectTimeout = null;
                Duration requestTimeout = null;
                Instant requestDeadline = null;
                if (deadline != null) {
                    val parentTimeout = between(time, deadline);
                    connectTimeout = ofMillis((long) (parentTimeout.toMillis() * connectionToRequestTimeoutRate));
                    requestTimeout = parentTimeout.minus(connectTimeout);
                    requestDeadline = time.plus(requestTimeout);
                }
                return timeoutsApplier.apply(connectTimeout, requestTimeout, requestDeadline);
            }
        };
    }

    <T> T calc(Instant deadline, TimeLimitExecutor.TimeoutsFunction<T> timeoutsApplier);
}
