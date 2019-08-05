package timeout;

import lombok.val;
import org.junit.Test;

import java.time.Instant;

import static java.time.Duration.ofMillis;
import static org.junit.Assert.assertEquals;
import static timeout.TimeoutsFormula.rateForDeadline;

public class TimeoutsTests {

    @Test
    public void testDefaultTimeouts() {
        val checkpoint = Instant.now();
        val executor = newExecutor(checkpoint);
        val deadline = checkpoint.plusMillis(1000);
        executor.run(deadline, context -> context.timeouts((connectionTimeout, requestTimeout, readDeadline) -> {
            assertEquals(ofMillis(100), connectionTimeout);
            assertEquals(ofMillis(900), requestTimeout);
            assertEquals(checkpoint.plus(requestTimeout), readDeadline);
        }));
    }

    private TimeLimitExecutorImpl newExecutor(Instant checkpoint) {
        return new TimeLimitExecutorImpl(() -> checkpoint, rateForDeadline(0.1, () -> checkpoint));
    }
}
