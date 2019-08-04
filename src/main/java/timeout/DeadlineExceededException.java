package timeout;

import lombok.Getter;

import java.util.Date;


public class DeadlineExceededException extends RuntimeException {
    @Getter
    private final Long checkTime;
    @Getter
    private final Long deadline;

    public DeadlineExceededException(Long checkTime, Long deadline) {
        super("Deadline exceed '" + toDate(deadline) + "'. check time '" + toDate(checkTime) + "'");
        this.checkTime = checkTime;
        this.deadline = deadline;
    }

    private static String toDate(Long millis) {
        return millis != null ? new Date(millis).toString(): "unknown";
    }

}
