package timeout;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newCachedThreadPool;

@Slf4j
@RequiredArgsConstructor
public class DeadlineExecutor {

    private final double connectionToRequestTimeoutRate;
    private final double childDeadlineRate;
    private final ExecutorService defaultExecutor;

    public DeadlineExecutor() {
        this(0.3, 1, newCachedThreadPool());
    }

    public DeadlineExecutor(double connectionToRequestTimeoutRate, double childDeadlineRate) {
        this(connectionToRequestTimeoutRate, childDeadlineRate, newCachedThreadPool());
    }

    private static void check(long current, long deadline) throws DeadlineExceededException {
        if (deadline <= current) throw new DeadlineExceededException(current, deadline);
    }

    private static void check(Long deadline) {
        if (deadline != null) check(currentTimeMillis(), deadline);
    }

    private static Callable<Object> toCallable(Runnable runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }

    /**
     * transform to TimeoutsFunction
     */
    private static TimeoutsFunction<Void> toTimeoutsFunc(TimeoutsConsumer consumer) {
        return (deadline, connectionTimeout, requestTimeout) -> {
            consumer.consume(deadline, connectionTimeout, requestTimeout);
            return null;
        };
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
    public <T> T call(Long deadline, Callable<T> callable) {
        DeadlineHolder.setDeadline(deadline);
        try {
            return _checkAndCall(deadline, callable);
        } finally {
            DeadlineHolder.clear();
        }
    }

    /**
     * runs Runnable in new deadline context. Checks if the deadline is exceeded and puts it in ThreadLocal.
     *
     * @param deadline - deadline as millis of epoch
     * @param runnable - some runnable
     */
    public void run(Long deadline, Runnable runnable) {
        call(deadline, toCallable(runnable));
    }

    public void run(Runnable runnable) {
        call(toCallable(runnable));
    }

    public <T> T call(Callable<T> callable) {
        return _checkAndCall(getDeadline(), callable);
    }

    public void run(Long deadline, ChildDeadlineConsumer consumer) {
        call(deadline, childDeadline -> {
            consumer.consume(childDeadline);
            return null;
        });
    }

    public void run(Long deadline, TimeoutsConsumer consumer) {
        call(deadline, toTimeoutsFunc(consumer));
    }

    public void run(TimeoutsConsumer consumer) {
        val deadline = getDeadline();
        _checkAndCall(deadline, toCallable(deadline, toChildDeadlineFunc(deadline, toTimeoutsFunc(consumer))));
    }

    public <T> T call(TimeoutsFunction<T> function) {
        val deadline = getDeadline();
        return _checkAndCall(deadline, toCallable(deadline, toChildDeadlineFunc(deadline, function)));
    }

    public <T> T call(Long deadline, TimeoutsFunction<T> function) {
        return call(deadline, toChildDeadlineFunc(deadline, function));
    }

    public <T> T call(Long deadline, ChildDeadlineFunction<T> function) {
        return call(deadline, toCallable(deadline, function));
    }

    /**
     * calculates child deadline and returns Callable object
     */
    private <T> Callable<T> toCallable(Long deadline, ChildDeadlineFunction<T> function) {
        return () -> function.apply(calcChild(deadline));
    }

    @SneakyThrows
    private <T> T _checkAndCall(Long deadline, Callable<T> callable) {
        check(deadline);
        return callable.call();
    }

    /**
     * calculates connection and request timeouts for network connections and transform to ChildDeadlineFunction class
     */
    private <T> ChildDeadlineFunction<T> toChildDeadlineFunc(Long deadline, TimeoutsFunction<T> function) {
        return childDeadline -> {
            val current = currentTimeMillis();
            long connectTimeout;
            long requestTimeout;
            if (deadline != null) {
                val sumTimeout = deadline - current;
                connectTimeout = (long) (sumTimeout * connectionToRequestTimeoutRate);
                requestTimeout = sumTimeout - connectTimeout;
            } else {
                connectTimeout = -1;
                requestTimeout = -1;
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

    public <T> Future<T> asyncCall(TimeoutsFunction<T> function) {
        return asyncCall(defaultExecutor, function);
    }

    public <T> Future<T> asyncCall(ExecutorService executor, TimeoutsFunction<T> function) {
        return asyncCall(executor, getDeadline(), function);
    }

    private <T> Future<T> asyncCall(ExecutorService executor, Long deadline, TimeoutsFunction<T> function) {
        return executor.submit(() -> DeadlineExecutor.this.call(deadline, function));
    }

    private Long calcChild(Long deadline) {
        return deadline == null ? null : (long) (deadline * childDeadlineRate);
    }

    public interface TimeoutsConsumer {
        void consume(Long childDeadline, long connectionTimeout, long requestTimeout);
    }

    public interface ChildDeadlineConsumer {
        void consume(Long childDeadline);
    }

    public interface ChildDeadlineFunction<T> {
        T apply(Long childDeadline);
    }

    public interface TimeoutsFunction<T> {
        T apply(Long childDeadline, long connectionTimeout, long requestTimeout);
    }
}
