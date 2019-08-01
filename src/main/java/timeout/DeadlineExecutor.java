package timeout;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newCachedThreadPool;

@Slf4j
@RequiredArgsConstructor
public class DeadlineExecutor {
    private final static DeadlineExceedFunction<?> throwFunc = (long checkTime, long deadline) -> {
        throw new DeadlineExceededException(checkTime, deadline);
    };

    private final double connectionToRequestTimeoutRate;
    private final double childDeadlineRate;
    private final ExecutorService defaultExecutor;

    public DeadlineExecutor() {
        this(0.3, 1, newCachedThreadPool());
    }

    public DeadlineExecutor(double connectionToRequestTimeoutRate, double childDeadlineRate) {
        this(connectionToRequestTimeoutRate, childDeadlineRate, newCachedThreadPool());
    }

    private static Callable<Object> c(Runnable runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }

    private static <T> DeadlineExceedFunction<T> f(DeadlineExceedConsumer deadlineExceed) {
        return (checkTime, d) -> {
            deadlineExceed.consume(checkTime, d);
            return null;
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> DeadlineExceedFunction<T> throwFunc() {
        return (DeadlineExceedFunction<T>) throwFunc;
    }

    private Long getDeadline() {
        return DeadlineHolder.getDeadline();
    }

    /**
     * calls Callable in new deadline context. Checks if the deadline is exceeded and puts it in ThreadLocal.
     *
     * @param deadline - deadline as millis of epoch
     * @param callable - some callable
     * @param <T>      - result type
     * @return - callable result
     */
    public <T> T call(Long deadline, Callable<T> callable, DeadlineExceedFunction<T> deadlineExceed) {
        return startContext(deadline, callable, deadlineExceed);
    }

    public <T> T call(Callable<T> callable, DeadlineExceedFunction<T> deadlineExceed) {
        return inExistedContext(callable, deadlineExceed);
    }

    /**
     * runs Runnable in new deadline context. Checks if the deadline is exceeded and puts it in ThreadLocal.
     *
     * @param deadline - deadline as millis of epoch
     * @param runnable - some runnable
     */
    public void run(Long deadline, Runnable runnable) {
        startContext(deadline, c(runnable), throwFunc());
    }

    public void run(Long deadline, Runnable runnable, DeadlineExceedConsumer deadlineExceed) {
        startContext(deadline, c(runnable), f(deadlineExceed));
    }

    public void run(Runnable runnable) {
        inExistedContext(c(runnable), throwFunc());
    }

    public void run(Runnable runnable, DeadlineExceedConsumer deadlineExceed) {
        inExistedContext(c(runnable), f(deadlineExceed));
    }

    public void run(Long deadline, ChildDeadlineConsumer consumer) {
        startContext(deadline, c(deadline, consumer), throwFunc());
    }

    public void run(Long deadline, ChildDeadlineConsumer consumer, DeadlineExceedConsumer deadlineExceed) {
        startContext(deadline, c(deadline, consumer), f(deadlineExceed));
    }

    public void run(Long deadline, TimeoutsConsumer consumer) {
        startContext(deadline, c(deadline, consumer), throwFunc());
    }

    public void run(Long deadline, TimeoutsConsumer consumer, DeadlineExceedConsumer deadlineExceed) {
        startContext(deadline, c(deadline, consumer), f(deadlineExceed));
    }

    public void run(TimeoutsConsumer consumer) {
        inExistedContext(consumer, throwFunc());
    }

    public void run(TimeoutsConsumer consumer, DeadlineExceedConsumer deadlineExceed) {
        inExistedContext(consumer, f(deadlineExceed));
    }

    public <T> T call(TimeoutsFunction<T> function) {
        return inExistedContext(function, throwFunc());
    }

    public <T> T call(TimeoutsFunction<T> function, DeadlineExceedFunction<T> deadlineExceed) {
        return inExistedContext(function, deadlineExceed);
    }

    public <T> T call(Long deadline, TimeoutsFunction<T> function, DeadlineExceedFunction<T> deadlineExceedConsumer) {
        return startContext(deadline, c(deadline, function), deadlineExceedConsumer);
    }

    public <T> T call(Long deadline, ChildDeadlineFunction<T> function, DeadlineExceedFunction<T> deadlineExceedConsumer) {
        return startContext(deadline, c(deadline, function), deadlineExceedConsumer);
    }

    private <T> T startContext(Long deadline, Callable<T> callable, DeadlineExceedFunction<T> deadlineExceed) {
        DeadlineHolder.setDeadline(deadline);
        try {
            return checkAndCall(deadline, callable, deadlineExceed);
        } catch (DeadlineExceededException e) {
            if (deadlineExceed == throwFunc) throw e;
            else return deadlineExceed.apply(e.getCheckTime(), e.getDeadline());
        } finally {
            DeadlineHolder.clear();
        }
    }

    private <T> T inExistedContext(TimeoutsFunction<T> function, DeadlineExceedFunction<T> deadlineExceed) {
        val deadline = getDeadline();
        return checkAndCall(deadline, c(deadline, function), deadlineExceed);
    }

    private <T> T inExistedContext(Callable<T> callable, DeadlineExceedFunction<T> deadlineExceed) {
        return checkAndCall(getDeadline(), callable, deadlineExceed);
    }

    private void inExistedContext(TimeoutsConsumer consumer, DeadlineExceedFunction<Void> deadlineExceed) {
        val deadline = getDeadline();
        checkAndCall(deadline, c(deadline, consumer), deadlineExceed);
    }

    @SneakyThrows
    private <T> T checkAndCall(Long deadline, Callable<T> callable, DeadlineExceedFunction<T> deadlineExceed) {
        if (deadline != null) {
            val checkTime = currentTimeMillis();
            if (deadline <= checkTime) {
                if (log.isTraceEnabled()) log.trace("Deadline exceed '{}'. check time '{}'",
                        new Date(deadline), new Date(checkTime));
                return deadlineExceed.apply(checkTime, deadline);
            }
        }
        return callable.call();
    }

    /**
     * calculates child deadline and transforms Callable object
     */
    private <T> Callable<T> c(Long deadline, ChildDeadlineFunction<T> function) {
        return () -> function.apply(calcChild(deadline));
    }

    private Callable<Void> c(Long deadline, ChildDeadlineConsumer consumer) {
        return c(deadline, childDeadline -> {
            consumer.consume(childDeadline);
            return null;
        });
    }

    private Callable<Void> c(Long deadline, TimeoutsConsumer consumer) {
        return c(deadline, f(deadline, (deadline1, connectionTimeout, requestTimeout) -> {
            consumer.consume(deadline1, connectionTimeout, requestTimeout);
            return null;
        }));
    }

    private <T> Callable<T> c(Long deadline, TimeoutsFunction<T> function) {
        return c(deadline, f(deadline, function));
    }

    /**
     * calculates connection and request timeouts for network connections and transforms to ChildDeadlineFunction class
     */
    private <T> ChildDeadlineFunction<T> f(Long deadline, TimeoutsFunction<T> function) {
        return childDeadline -> {
            val current = currentTimeMillis();
            Long connectTimeout = null;
            Long requestTimeout = null;
            if (deadline != null) {
                val sumTimeout = deadline - current;
                connectTimeout = (long) (sumTimeout * connectionToRequestTimeoutRate);
                requestTimeout = sumTimeout - connectTimeout;
            }
            log.trace("connectTimeout:{}, requestTimeout:{}", connectTimeout, requestTimeout);
            return function.apply(childDeadline, connectTimeout, requestTimeout);
        };
    }

    public void asyncRun(TimeoutsConsumer consumer) {
        asyncRun(defaultExecutor, consumer);
    }

    public void asyncRun(Executor executor, TimeoutsConsumer consumer) {
        asyncRun(executor, getDeadline(), consumer);
    }

    public void asyncRun(Executor executor, Long deadline, TimeoutsConsumer consumer) {
        executor.execute(() -> DeadlineExecutor.this.run(deadline, consumer));
    }

    public <T> Future<T> asyncCall(TimeoutsFunction<T> function, DeadlineExceedFunction<T> deadlineExceedConsumer) {
        return asyncCall(defaultExecutor, function, deadlineExceedConsumer);
    }

    public <T> Future<T> asyncCall(ExecutorService executor, TimeoutsFunction<T> function,
                                   DeadlineExceedFunction<T> deadlineExceedConsumer) {
        return asyncCall(executor, getDeadline(), function, deadlineExceedConsumer);
    }

    private <T> Future<T> asyncCall(ExecutorService executor, Long deadline, TimeoutsFunction<T> function,
                                    DeadlineExceedFunction<T> deadlineExceedConsumer) {
        return executor.submit(() -> startContext(deadline, c(deadline, f(deadline, function)), deadlineExceedConsumer));
    }

    private Long calcChild(Long deadline) {
        return deadline == null ? null : (long) (deadline * childDeadlineRate);
    }

    public interface TimeoutsConsumer {
        void consume(Long childDeadline, Long connectionTimeout, Long requestTimeout);
    }

    public interface ChildDeadlineConsumer {
        void consume(Long childDeadline);
    }

    public interface ChildDeadlineFunction<T> {
        T apply(Long childDeadline);
    }

    public interface TimeoutsFunction<T> {
        T apply(Long childDeadline, Long connectionTimeout, Long requestTimeout);
    }

    public interface DeadlineExceedFunction<T> {
        T apply(long checkTime, long deadline) throws DeadlineExceededException;
    }

    public interface DeadlineExceedConsumer {
        void consume(long checkTime, long deadline) throws DeadlineExceededException;
    }
}
