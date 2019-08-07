package timeout;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static lombok.AccessLevel.PRIVATE;
import static timeout.DeadlineExceededException.throwDefaultException;
import static timeout.TimeLimitInternal.*;

public interface TimeLimitExecutor {

    <T> T call(Instant deadline, Function<Context<T>, T> contextConsumer, DeadlineExceedFunction<T> deadlineExceedFunction);

    default <T> T call(Instant deadline, Function<Context<T>, T> contextConsumer) {
        return call(deadline, contextConsumer, throwDefaultException());
    }

    <T> T call(Function<Context<T>, T> contextConsumer, DeadlineExceedFunction<T> deadlineExceedFunction);

    default <T> T call(Function<Context<T>, T> contextConsumer) {
        return call(contextConsumer, throwDefaultException());
    }

    void run(Consumer<Context<Void>> contextConsumer, DeadlineExceedConsumer exceedConsumer);

    default void run(Consumer<Context<Void>> contextConsumer) {
        run(contextConsumer, throwDefaultException);
    }

    void run(Instant deadline, Consumer<Context<Void>> contextConsumer, DeadlineExceedConsumer exceedConsumer);

    default void run(Instant deadline, Consumer<Context<Void>> contextConsumer) {
        run(deadline, contextConsumer, throwDefaultException);
    }

    default void run(Instant deadline, Runnable runnable, DeadlineExceedConsumer exceedConsumer) {
        run(deadline, context -> context.run(runnable), exceedConsumer);
    }

    default void run(Instant deadline, Runnable runnable) {
        run(deadline, runnable, throwDefaultException);
    }

    interface TimeLimitExceedConsumer<T> {
        void consume(Instant checkTime, T exceed);
    }

    interface TimeoutsConsumer {
        void consume(Duration connectionTimeout, Duration readTimeout, Instant readDeadline);
    }

    interface TimeoutsFunction<T> {
        T apply(Duration connectionTimeout, Duration readTimeout, Instant readDeadline);
    }

    interface Context<T> {
        T timeouts(TimeoutsFunction<T> function);

        default void timeouts(TimeoutsConsumer consumer) {
            timeouts((connectionTimeout, readTimeout, readDeadline) -> {
                consumer.consume(connectionTimeout, readTimeout, readDeadline);
                return null;
            });
        }

        void run(Runnable runnable);

        T call(Callable<T> callable);
    }

    interface DeadlineExceedFunction<T> {
        T apply(Instant checkTime, Instant deadline) throws DeadlineExceededException;
    }

    interface DeadlineExceedConsumer {
        void consume(Instant checkTime, Instant exceededDeadline) throws DeadlineExceededException;
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = PRIVATE)
    class ContextImpl<T> implements Context<T> {
        Instant deadline;
        Clock<Instant> clock;
        TimeoutsFormula timeoutsFormula;
        DeadlineExceedFunction<T> exceedFunction;

        @Override
        public T timeouts(TimeoutsFunction<T> function) {
            return connectionCall(deadline, clock, timeoutsFormula, function, exceedFunction);
        }

        @Override
        public void run(Runnable runnable) {
            timeLimitedRun(deadline, clock, runnable, exceedFunction);
        }

        @Override
        @SneakyThrows
        public T call(Callable<T> callable) {
            return timeLimitedCall(this.deadline, clock, callable, exceedFunction);
        }

    }
}
