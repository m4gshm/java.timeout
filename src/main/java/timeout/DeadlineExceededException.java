package timeout;

import lombok.Getter;

import java.util.Date;


public class DeadlineExceededException extends RuntimeException {
    @Getter
    private final long checkTime;
    @Getter
    private final long deadline;

    DeadlineExceededException(long checkTime, long deadline) {
        super("Deadline exceed '" + new Date(deadline) + "'. check time '" + new Date(checkTime) + "'");
        this.checkTime = checkTime;
        this.deadline = deadline;
    }

}
