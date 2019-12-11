package timeout;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.time.Duration.between;
import static lombok.AccessLevel.PRIVATE;
import static timeout.DeadlineExceededException.throwDefaultException;

public interface TimeLimitExecutor {

    <T> Mono<T> limited(Instant deadline, Mono<T> mono);
    <T> Mono<T> limited(Mono<T> mono);

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
        Runnable noExceedFinalizer;
        DeadlineExceedFunction<T> exceedFunction;

        boolean isExceed(@NonNull Instant checkTime) {
            if (this.deadline == null) return false;
            val timeLeft = between(checkTime, this.deadline);
            return timeLeft.isZero() || timeLeft.isNegative();
        }

        T exceed(@NonNull Instant checkTime) {
            return this.exceedFunction.apply(checkTime, this.deadline);
        }

        @Override
        public T timeouts(TimeoutsFunction<T> function) {
            val checkTime = clock.time();
            if (isExceed(checkTime)) return exceed(checkTime);
            else try {
                return timeoutsFormula.calc(deadline, function);
            } finally {
                noExceedFinalizer.run();
            }
        }

        @Override
        public void run(Runnable runnable) {
            val checkTime = clock.time();
            if (isExceed(checkTime)) exceed(checkTime);
            else try {
                runnable.run();
            } finally {
                noExceedFinalizer.run();
            }
        }

        @Override
        @SneakyThrows
        public T call(Callable<T> callable) {
            val checkTime = clock.time();
            if (isExceed(checkTime)) return exceed(checkTime);
            else try {
                return callable.call();
            } finally {
                noExceedFinalizer.run();
            }
        }

    }
}
