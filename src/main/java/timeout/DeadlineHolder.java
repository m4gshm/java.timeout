package timeout;


import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
class DeadlineHolder {
    private static final ThreadLocal<Long> holder = new InheritableThreadLocal<>();

    static Long getDeadline() {
        return holder.get();
    }

    static void setDeadline(Long deadline) {
        holder.set(deadline);
        log.trace("set deadline:{}", deadline);
    }

    static void clear() {
        holder.set(null);
        log.trace("clear deadline");
    }

}
