package m4gshm.java.timeout;

import lombok.val;
import m4gshm.java.timeout.Clock;
import m4gshm.java.timeout.DeadlineExceededException;
import m4gshm.java.timeout.TimeLimitExecutor;
import m4gshm.java.timeout.TimeLimitExecutorImpl;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Instant.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static m4gshm.java.timeout.TimeLimitExecutorImpl.getThreadDeadline;
import static m4gshm.java.timeout.TimeoutsFormula.rateForDeadline;

public class DeadlineTest {

    static Clock<Instant> discreteClockByRequest(Instant start) {
        return discreteClockByRequest(start, 1000);
    }

    static Clock<Instant> discreteClockByRequest(Instant start, int oneTime) {
        return new Clock<Instant>() {
            Instant time = start;

            @Override
            public Instant time() {
                return time = time.plusMillis(oneTime);
            }
        };
    }

    static TimeLimitExecutor newExecutor(Instant checkpoint) {
        return newExecutor(() -> checkpoint);
    }

    static TimeLimitExecutorImpl newExecutor(Clock<Instant> clock) {
        return new TimeLimitExecutorImpl(clock, rateForDeadline(0.1, clock));
    }

    @Test(expected = DeadlineExceededException.class)
    public void testThrowDeadlineInRun() {
        val checkpoint = now();
        val executor = newExecutor(discreteClockByRequest(checkpoint));
        val deadline = checkpoint.plusMillis(3000);
        val successCounter = new AtomicInteger();
        try {
            executor.run(deadline, context -> {
                context.run(successCounter::incrementAndGet);
                context.run(successCounter::incrementAndGet);
                context.run(Assert::fail);
                Assert.fail();
            });
            Assert.fail();
        } finally {
            assertEquals(2, successCounter.get());
        }
    }

    @Test(expected = DeadlineExceededException.class)
    public void testThrowDeadlineInCall() {
        val checkpoint = now();
        val executor = newExecutor(discreteClockByRequest(checkpoint));
        val deadline = checkpoint.plusMillis(3000);
        executor.<String>call(deadline, context -> {
            val r = context.call(() -> "1") + context.call(() -> "2") + context.call(() -> {
                Assert.fail();
                return "3";
            });
            Assert.fail();
            return r;
        });
        Assert.fail();
    }

    @Test
    public void testDeadlineInCall() {
        val checkpoint = now();
        val executor = newExecutor(discreteClockByRequest(checkpoint));
        val deadline = checkpoint.plusMillis(3000);
        val result = executor.call(deadline, context -> context.call(() -> "1") + context.call(() -> "2") + context.call(() -> {
            Assert.fail();
            return "3";
        }), (checkTime, deadline1) -> "deadline");

        assertEquals("12deadline", result);
    }

    @Test
    public void testDeadlineExceedClearsThreadDeadlineBeforeInvoking() {
        val checkpoint = now();
        val executor = newExecutor(discreteClockByRequest(checkpoint));
        val deadline = checkpoint.plusMillis(2000);
        Instant result = executor.call(deadline, context -> context.call(() -> {
            val threadDeadline = getThreadDeadline();
            Assert.assertNotNull(threadDeadline);
            return context.call(() -> {
                Assert.fail();
                return getThreadDeadline();
            });
        }), (checkTime, deadline1) -> {
            val threadDeadline = getThreadDeadline();
            assertNull(threadDeadline);
            return threadDeadline;
        });

        assertNull(null, result);
    }
}
