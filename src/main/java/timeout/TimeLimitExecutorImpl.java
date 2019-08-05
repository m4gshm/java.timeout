package timeout;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.time.Duration.between;

@Slf4j
@RequiredArgsConstructor
public class TimeLimitExecutorImpl implements TimeLimitExecutor {

    private static final ThreadLocal<Instant> holder = new InheritableThreadLocal<>();
    private final Clock<Instant> clock;
    private final TimeoutsFormula timeoutsFormula;

    private static Callable<Object> c(Runnable runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }

    private static <T> DeadlineExceedFunction<T> f(TimeLimitExceedConsumer<Instant> consumer) {
        return (checkTime, d) -> {
            consumer.consume(checkTime, d);
            return null;
        };
    }

    private static Instant getThreadDeadline() {
        return holder.get();
    }

    private static boolean setThreadDeadline(Instant deadline) {
        val existed = getThreadDeadline();
        val owner = existed == null;
        if (owner) {
            holder.set(deadline);
            log.trace("set deadline:{}", deadline);
        } else log.trace("deadline has already been set. {}", existed);
        return owner;
    }

    private static boolean isExceed(@NonNull Instant checkTime, Instant deadline) {
        if (deadline == null) return false;
        val timeLeft = between(checkTime, deadline);
        return timeLeft.isZero() || timeLeft.isNegative();
    }

    private static void clearDeadline() {
        holder.set(null);
    }

    private static DeadlineExceedFunction<Void> f(DeadlineExceedConsumer exceedConsumer) {
        return (checkTime, deadline) -> {
            exceedConsumer.consume(checkTime, deadline);
            return null;
        };
    }

    @Override
    public <T> T call(Instant deadline, Function<Context<T>, T> contextConsumer, DeadlineExceedFunction<T> deadlineExceedFunction) {
        val owner = setThreadDeadline(deadline);
        try {
            return contextConsumer.apply(new DeadlineContext<T>(deadline, clock, timeoutsFormula, deadlineExceedFunction));
        } finally {
            if (owner) clearDeadline();
        }
    }

    @Override
    public <T> T call(Function<Context<T>, T> contextConsumer, DeadlineExceedFunction<T> deadlineExceedFunction) {
        return contextConsumer.apply(new DeadlineContext<T>(getThreadDeadline(), clock,
                timeoutsFormula, deadlineExceedFunction));
    }

    @Override
    public void run(Consumer<Context<Void>> contextConsumer, DeadlineExceedConsumer exceedConsumer) {
        contextConsumer.accept(new DeadlineContext<>(getThreadDeadline(), clock, timeoutsFormula, f(exceedConsumer)));
    }

    @Override
    public void run(Instant deadline, Consumer<Context<Void>> contextConsumer, DeadlineExceedConsumer exceedConsumer) {
        val owner = setThreadDeadline(deadline);
        try {
            contextConsumer.accept(new DeadlineContext<>(deadline, clock, timeoutsFormula, f(exceedConsumer)));
        } finally {
            if (owner) clearDeadline();
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true)
    static class DeadlineContext<T> implements Context<T> {
        Instant deadline;
        Clock<Instant> clock;
        TimeoutsFormula timeoutsFormula;
        DeadlineExceedFunction<T> exceedFunction;

        boolean isExceed(Instant checkTime) {
            return TimeLimitExecutorImpl.isExceed(checkTime, this.deadline);
        }

        @Override
        public T timeouts(TimeoutsFunction<T> function) {
            val checkTime = clock.time();
            return isExceed(checkTime)
                    ? exceedFunction.apply(checkTime, deadline)
                    : timeoutsFormula.calc(deadline, function);
        }

        @Override
        public void run(Runnable runnable) {
            val checkTime = clock.time();
            if (isExceed(checkTime)) exceedFunction.apply(checkTime, deadline);
            else runnable.run();
        }
    }


    @FieldDefaults(makeFinal = true)
    static class TimeoutContext<T> extends DeadlineContext<T> {
        Duration timeout;
        Instant startTime;

        public TimeoutContext(Instant startTime, Duration timeout, Clock<Instant> clock,
                              TimeoutsFormula timeoutsFormula,
                              DeadlineExceedFunction<T> exceedFunction) {
            super(startTime.plus(timeout), clock, timeoutsFormula, exceedFunction);
            this.startTime = startTime;
            this.timeout = timeout;
        }
    }
}
