package timeout;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.function.Consumer;
import java.util.function.Function;

import static timeout.DeadlineExceededException.throwDefaultException;

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
    }

    interface DeadlineExceedFunction<T> {
        T apply(Instant checkTime, Instant deadline) throws DeadlineExceededException;
    }

    interface DeadlineExceedConsumer {
        void consume(Instant checkTime, Instant deadline) throws DeadlineExceededException;
    }

}
