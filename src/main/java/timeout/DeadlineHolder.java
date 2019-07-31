package timeout;


import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.Logger;

import java.time.Duration;

import static java.time.ZonedDateTime.now;

@UtilityClass
@Slf4j
public class DeadlineHolder {
    private static final ThreadLocal<Long> holder = new ThreadLocal<>();

    public static Long getDeadline() {
        return holder.get();
    }

    public static void setDeadline(Long deadline) {
        holder.set(deadline);
        log.trace("set deadline:{}", deadline);
    }

    public static void clear() {
        holder.set(null);
        log.trace("clear deadline");
    }

    public static Long calc(Duration defaultDeadline, Logger log) {
        val dateTime = now().plus(defaultDeadline);
        val deadline = dateTime.toInstant().toEpochMilli();
        log.trace("uses default deadline:{}, as date-time:{}", deadline, dateTime);
        return deadline;
    }

}