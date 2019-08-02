package timeout;


import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@UtilityClass
@Slf4j
class DeadlineHolder {
    private static final ThreadLocal<Long> holder = new InheritableThreadLocal<>();

    static Long getDeadline() {
        return holder.get();
    }

    static void setDeadline(Long deadline) {
        holder.set(deadline);
        if (log.isTraceEnabled()) log.trace("set deadline:{}", deadline != null ? new Date(deadline) : deadline);
    }

    static void clear() {
        holder.set(null);
        log.trace("clear deadline");
    }

}
