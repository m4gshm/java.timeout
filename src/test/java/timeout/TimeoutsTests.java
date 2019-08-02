package timeout;

import lombok.val;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;

public class TimeoutsTests {

    @Test
    public void testDefaultTimeouts() {
        val checkpoint = currentTimeMillis();
        val executor = newExecutor(checkpoint);
        val deadline = checkpoint + 1000;
        executor.run(deadline, (connectionTimeout, requestTimeout, readDeadline) -> {
            assertEquals(100, (long) connectionTimeout);
            assertEquals(900, (long) requestTimeout);
            assertEquals(checkpoint + requestTimeout, (long) readDeadline);
        });
    }

    private DeadlineExecutor newExecutor(long checkpoint) {
        return new DeadlineExecutor(() -> checkpoint);
    }
}
