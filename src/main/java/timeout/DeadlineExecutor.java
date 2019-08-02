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
import static timeout.DeadlineHolder.*;

@Slf4j
@RequiredArgsConstructor
public class DeadlineExecutor {
    private static final double defaultTimeoutRate = 0.3;
    private static final ChildDeadlineFormula childDeadlineEqualsToParent = deadline -> deadline;
    private final static DeadlineExceedFunction<?> throwFunc = (long checkTime, long deadline) -> {
        throw new DeadlineExceededException(checkTime, deadline);
    };
    private final double connectionToRequestTimeoutRate;
    private final TimeFormula checkTimeFormula;
    private final ChildDeadlineFormula childDeadlineFormula;
    private final ExecutorService defaultExecutor;

    public DeadlineExecutor() {
        this(defaultTimeoutRate, childDeadlineEqualsToParent);
    }

    public DeadlineExecutor(TimeFormula checkTimeFormula,
                            ChildDeadlineFormula childDeadlineFormula) {
        this(defaultTimeoutRate, checkTimeFormula, childDeadlineFormula, newCachedThreadPool());
    }

    public DeadlineExecutor(TimeFormula checkTimeFormula) {
        this(defaultTimeoutRate, checkTimeFormula, childDeadlineEqualsToParent, newCachedThreadPool());
    }

    public DeadlineExecutor(double connectionToRequestTimeoutRate, TimeFormula checkTimeFormula,
                            ChildDeadlineFormula childDeadlineFormula) {
        this(connectionToRequestTimeoutRate, checkTimeFormula, childDeadlineFormula, newCachedThreadPool());
    }

    public DeadlineExecutor(double connectionToRequestTimeoutRate, ChildDeadlineFormula childDeadlineFormula) {
        this(connectionToRequestTimeoutRate, System::currentTimeMillis, childDeadlineFormula, newCachedThreadPool());
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

    public static ChildDeadlineFormula rate(double rate) {
        return deadline -> (long) (deadline * rate);
    }

    public static ChildDeadlineFormula lag(long lag) {
        if (lag < 0) throw new IllegalArgumentException("invalid negative lag:" + lag);
        return deadline -> deadline - lag;
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
        return startOrJoin(deadline, callable, deadlineExceed);
    }

    public <T> T call(Callable<T> callable, DeadlineExceedFunction<T> deadlineExceed) {
        return join(callable, deadlineExceed);
    }

    /**
     * runs Runnable in new deadline context. Checks if the deadline is exceeded and puts it in ThreadLocal.
     *
     * @param deadline - deadline as millis of epoch
     * @param runnable - some runnable
     */
    public void run(Long deadline, Runnable runnable) {
        startOrJoin(deadline, c(runnable), throwFunc());
    }

    public void run(Long deadline, Runnable runnable, DeadlineExceedConsumer deadlineExceed) {
        startOrJoin(deadline, c(runnable), f(deadlineExceed));
    }

    public void run(Runnable runnable) {
        join(c(runnable), throwFunc());
    }

    public void run(Runnable runnable, DeadlineExceedConsumer deadlineExceed) {
        join(c(runnable), f(deadlineExceed));
    }

    public void run(Long deadline, ChildDeadlineConsumer consumer) {
        startOrJoin(deadline, c(deadline, consumer), throwFunc());
    }

    public void run(Long deadline, ChildDeadlineConsumer consumer, DeadlineExceedConsumer deadlineExceed) {
        startOrJoin(deadline, c(deadline, consumer), f(deadlineExceed));
    }

    public void run(Long deadline, TimeoutsConsumer consumer) {
        startOrJoin(deadline, c(deadline, consumer), throwFunc());
    }

    public void run(Long deadline, TimeoutsConsumer consumer, DeadlineExceedConsumer deadlineExceed) {
        startOrJoin(deadline, c(deadline, consumer), f(deadlineExceed));
    }

    public void run(TimeoutsConsumer consumer) {
        join(consumer, throwFunc());
    }

    public void run(TimeoutsConsumer consumer, DeadlineExceedConsumer deadlineExceed) {
        join(consumer, f(deadlineExceed));
    }

    public <T> T call(TimeoutsFunction<T> function) {
        return join(function, throwFunc());
    }

    public <T> T call(TimeoutsFunction<T> function, DeadlineExceedFunction<T> deadlineExceed) {
        return join(function, deadlineExceed);
    }

    public <T> T call(Long deadline, TimeoutsFunction<T> function, DeadlineExceedFunction<T> deadlineExceedConsumer) {
        return startOrJoin(deadline, c(deadline, function), deadlineExceedConsumer);
    }

    public <T> T call(Long deadline, ChildDeadlineFunction<T> function, DeadlineExceedFunction<T> deadlineExceedConsumer) {
        return startOrJoin(deadline, c(deadline, function), deadlineExceedConsumer);
    }

    private <T> T startOrJoin(Long deadline, Callable<T> callable, DeadlineExceedFunction<T> deadlineExceed) {
        val existed = getDeadline();
        val owner = existed == null;
        if (owner) setDeadline(deadline);
        else if (log.isTraceEnabled()) log.trace("deadline has already been set. {}", new Date(existed));
        try {
            return checkAndCall(deadline, callable, deadlineExceed);
        } catch (DeadlineExceededException e) {
            if (deadlineExceed == throwFunc) throw e;
            else return deadlineExceed.apply(e.getCheckTime(), e.getDeadline());
        } finally {
            if (owner) clear();
        }
    }

    private <T> T join(TimeoutsFunction<T> function, DeadlineExceedFunction<T> deadlineExceed) {
        val deadline = getDeadline();
        return checkAndCall(deadline, c(deadline, function), deadlineExceed);
    }

    private <T> T join(Callable<T> callable, DeadlineExceedFunction<T> deadlineExceed) {
        return checkAndCall(getDeadline(), callable, deadlineExceed);
    }

    private void join(TimeoutsConsumer consumer, DeadlineExceedFunction<Void> deadlineExceed) {
        val deadline = getDeadline();
        checkAndCall(deadline, c(deadline, consumer), deadlineExceed);
    }

    @SneakyThrows
    private <T> T checkAndCall(Long deadline, Callable<T> callable, DeadlineExceedFunction<T> deadlineExceed) {
        if (deadline != null) {
            val checkTime = checkTimeFormula.time();
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
        return executor.submit(() -> startOrJoin(deadline, c(deadline, f(deadline, function)), deadlineExceedConsumer));
    }

    private Long calcChild(Long deadline) {
        val childDeadline = childDeadlineFormula.calc(deadline);
        if (log.isTraceEnabled()) log.trace("child deadline:{} for parent deadline:{}",
                new Date(childDeadline), new Date(deadline));

        if (childDeadline != null && deadline != null) {
            if (childDeadline > deadline) throw new BadChildDeadlineException(deadline, childDeadline);
            if (childDeadline < 0) throw new BadChildDeadlineException(deadline, childDeadline);
        }
        return childDeadline;
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

    public interface ChildDeadlineFormula {
        Long calc(Long deadline);
    }

    public interface TimeFormula {
        long time();
    }
}
