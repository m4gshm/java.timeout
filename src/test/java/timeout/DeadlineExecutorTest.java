package timeout;

import lombok.val;
import org.junit.Assert;
import org.junit.Test;
import timeout.DeadlineExecutor.ChildDeadlineConsumer;
import timeout.DeadlineExecutor.ChildDeadlineFormula;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;
import static timeout.DeadlineExecutor.lag;
import static timeout.DeadlineExecutor.rate;

public class DeadlineExecutorTest {

    @Test
    public void testChildDeadlineMoreThanDeadline() {
        val checkpoint = currentTimeMillis();
        ChildDeadlineFormula formula = deadline -> deadline + 1;
        val executor = newExecutor(checkpoint, formula);
        val deadline = checkpoint + 1000;
        try {
            executor.run(deadline, childDeadline -> Assert.fail());
            Assert.fail();
        } catch (BadChildDeadlineException e) {
            val message = e.getMessage();
            assertTrue(message, message.contains("Bad child deadline. parent"));
        }
    }

    private DeadlineExecutor newExecutor(long checkpoint, ChildDeadlineFormula formula) {
        return new DeadlineExecutor(() -> checkpoint, formula);
    }

    private DeadlineExecutor newExecutor(long checkpoint) {
        return new DeadlineExecutor(() -> checkpoint);
    }

    @Test
    public void testChildDeadlineNegativeLag() {
        val deadlineLag = -500;
        try {
            lag(deadlineLag);
        } catch (IllegalArgumentException e) {
            val message = e.getMessage();
            assertTrue(message, message.contains("invalid negative lag:"));
        }
    }


    @Test
    public void testChildDeadlineLag() {
        val deadlineLag = 500;
        val checkpoint = currentTimeMillis();
        val executor = newExecutor(checkpoint, lag(deadlineLag));

        val deadline = checkpoint + 1000;
        executor.run(deadline, childDeadline -> {
            assertNotNull(childDeadline);
            assertTrue(deadline > childDeadline);
            val possibleParentDeadline = childDeadline + deadlineLag;
            assertEquals(deadline, possibleParentDeadline);
        });
    }

    @Test
    public void testChildDeadlineRate() {
        val deadlineLag = 500;
        val checkpoint = currentTimeMillis();
        val executor = newExecutor(checkpoint, rate(calcRate(deadlineLag)));

        val deadline = currentTimeMillis() + 1000;
        executor.run(deadline, childDeadline -> {
            assertNotNull(childDeadline);
            assertTrue(deadline > childDeadline);
            val possibleParentDeadline = childDeadline + deadlineLag;
            assertTrue(deadline >= possibleParentDeadline);
        });
    }

    private double calcRate(int deadlineLag) {
        val child = currentTimeMillis();
        val parent = child + deadlineLag;
        return child / parent;
    }

    @Test
    public void childDeadlineByDefault() {
        val checkpoint = currentTimeMillis();
        val executor = newExecutor(checkpoint);
        val deadline = currentTimeMillis() + 1000;
        executor.run(deadline, childDeadline -> {
            assertNotNull(childDeadline);
            assertEquals(deadline, (long) childDeadline);
        });
    }

    @Test
    public void childDeadlineNull() {
        DeadlineExecutor executor = new DeadlineExecutor();
        executor.run(null, (ChildDeadlineConsumer) Assert::assertNull);
    }
}
