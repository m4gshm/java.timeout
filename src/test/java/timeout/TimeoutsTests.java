package timeout;

import lombok.val;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.System.currentTimeMillis;

public class TimeoutsTests {
    
    @Test
    public void testDefaultTimeouts() {
        val checkpoint = currentTimeMillis();
        val executor = newExecutor(checkpoint);
        val deadline = checkpoint + 1000;
        executor.run(deadline, (connectionTimeout, requestTimeout) -> {
            Assert.assertEquals(300, (long) connectionTimeout);
            Assert.assertEquals(700, (long) requestTimeout);
        });
    }


    private DeadlineExecutor newExecutor(long checkpoint) {
        return new DeadlineExecutor(() -> checkpoint);
    }
}
