package timeout;

import lombok.Getter;

import java.util.Date;

public class BadChildDeadlineException extends RuntimeException {
    @Getter
    private final Long deadline;
    @Getter
    private final Long childDeadline;

    public BadChildDeadlineException(Long deadline, Long childDeadline) {
        super("Bad child deadline. parent:'" + new Date(deadline) + "'. child:'" + new Date(childDeadline) + "'");
        this.deadline = deadline;
        this.childDeadline = childDeadline;
    }
}
