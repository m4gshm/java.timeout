package timeout;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = PRIVATE)
public class TimeLimitExecutorImpl implements TimeLimitExecutor {

    static final ThreadLocal<Instant> holder = new InheritableThreadLocal<>();
    Clock<Instant> clock;
    TimeoutsFormula timeoutsFormula;

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
            return contextConsumer.apply(new ContextImpl<T>(deadline, clock, timeoutsFormula, deadlineExceedFunction));
        } finally {
            if (owner) clearDeadline();
        }
    }

    @Override
    public <T> T call(Function<Context<T>, T> contextConsumer, DeadlineExceedFunction<T> deadlineExceedFunction) {
        return contextConsumer.apply(new ContextImpl<T>(getThreadDeadline(), clock,
                timeoutsFormula, deadlineExceedFunction));
    }

    @Override
    public void run(Consumer<Context<Void>> contextConsumer, DeadlineExceedConsumer exceedConsumer) {
        contextConsumer.accept(new ContextImpl<>(getThreadDeadline(), clock, timeoutsFormula, f(exceedConsumer)));
    }

    @Override
    public void run(Instant deadline, Consumer<Context<Void>> contextConsumer, DeadlineExceedConsumer exceedConsumer) {
        val owner = setThreadDeadline(deadline);
        try {
            contextConsumer.accept(new ContextImpl<>(deadline, clock, timeoutsFormula, f(exceedConsumer)));
        } finally {
            if (owner) clearDeadline();
        }
    }

    @Override
    public void run(Instant deadline, Runnable runnable, DeadlineExceedConsumer exceedConsumer) {
        run(deadline, context -> context.run(runnable), exceedConsumer);
    }
}
