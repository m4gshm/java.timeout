package timeout;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static java.lang.System.currentTimeMillis;

@Slf4j
@RequiredArgsConstructor
public class ConnectTimeoutExecutor {

    private final double connectionToRequestTimeoutRate;
    private final double nextDeadlineRate;

    public ConnectTimeoutExecutor() {
        this(0.3, 1);
    }

    public static void check(long current, long deadline) throws DeadlineExceededException {
        if (deadline <= current) throw new DeadlineExceededException(current, deadline);
    }

    public void run(TimeoutsConsumer consumer) {
        call((long deadline, long connectionTimeout, long requestTimeout) -> {
            consumer.consume(deadline, connectionTimeout, requestTimeout);
            return null;
        });
    }

    public <T> T call(TimeoutsFunction<T> function) {
        val deadline = DeadlineHolder.getDeadline();
        if (deadline != null && deadline > 0) {
            long current = currentTimeMillis();
            check(current, deadline);
            long sumTimeout = deadline - current;
            long connectTimeout = (long) (sumTimeout * connectionToRequestTimeoutRate);
            long requestTimeout = sumTimeout - connectTimeout;

            long nextDeadline = (long) (deadline * nextDeadlineRate);
            log.trace("deadline:{}, nextDeadline:{}, connectTimeout:{}, requestTimeout:{}",
                    deadline, nextDeadline, connectTimeout, requestTimeout);
            return function.consume(nextDeadline, connectTimeout, requestTimeout);
        } else {
            log.trace("no deadline");
            return function.consume(-1, -1, -1);
        }
    }

    public Timeouts getTimeouts() {
        return call(Timeouts::new);
    }

    public interface TimeoutsConsumer {
        void consume(long deadline, long connectionTimeout, long requestTimeout);
    }

    public interface TimeoutsFunction<T> {
        T consume(long deadline, long connectionTimeout, long requestTimeout);
    }

    @Data
    @RequiredArgsConstructor
    public class Timeouts {
        final long deadline;
        final long connectionTimeout;
        final long requestTimeout;
    }

}
