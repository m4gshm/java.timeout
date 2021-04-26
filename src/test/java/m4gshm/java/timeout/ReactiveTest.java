package m4gshm.java.timeout;

import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.time.Instant.now;
import static reactor.core.publisher.Mono.just;
import static m4gshm.java.timeout.DeadlineTest.discreteClockByRequest;
import static m4gshm.java.timeout.DeadlineTest.newExecutor;

public class ReactiveTest {

    Instant checkpoint;
    TimeLimitExecutor executor;

    @Before
    public void init() {
        checkpoint = now();
        executor = newExecutor(discreteClockByRequest(checkpoint, 1000));
    }

    @Test
    public void subscribeTest() {
        val deadline = checkpoint.plusMillis(2000);
        val result = executor.limited(deadline, just("just")).log().block();
        Assert.assertEquals(result, "just");
    }

    @Test
    public void flatMapFailTest() {
        AtomicBoolean failed = new AtomicBoolean();
        val deadline = checkpoint.plusMillis(2000);
        executor.limited(deadline, Mono.<String>create(sink -> {
            sink.success("create");
        })).flatMap(r -> executor.limited(just(r + "flatMapJust"))).log().subscribe(s -> {
            Assert.fail("must fail by timeout");
        }, e -> {
            Assert.assertTrue("is DeadlineExceededException", e instanceof DeadlineExceededException);
            failed.set(true);
        });

        Assert.assertTrue(failed.get());
    }


}
