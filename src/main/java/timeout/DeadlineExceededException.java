package timeout;

import lombok.Getter;
import timeout.TimeLimitExecutor.DeadlineExceedConsumer;
import timeout.TimeLimitExecutor.DeadlineExceedFunction;

import java.time.Instant;


public class DeadlineExceededException extends RuntimeException {
    @Getter
    protected final Instant checkTime;
    @Getter
    private final Instant deadline;

    public DeadlineExceededException(Instant checkTime, Instant deadline) {
        super("Deadline exceed '" + deadline + "'. check time '" + checkTime + "'");
        this.checkTime = checkTime;
        this.deadline = deadline;
    }

    private final static DeadlineExceedFunction<?> throwFunc = (checkTime, deadline) -> {
        throw new DeadlineExceededException(checkTime, deadline);
    };

    final static DeadlineExceedConsumer throwDefaultException = (checkTime, deadline) -> {
        throw new DeadlineExceededException(checkTime, deadline);
    };

    @SuppressWarnings("unchecked")
    static <T> DeadlineExceedFunction<T> throwDefaultException() {
        return (DeadlineExceedFunction<T>) throwFunc;
    }

}
