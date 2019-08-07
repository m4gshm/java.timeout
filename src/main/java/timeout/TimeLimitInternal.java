package timeout;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import timeout.TimeLimitExecutor.DeadlineExceedFunction;
import timeout.TimeLimitExecutor.TimeoutsFunction;

import java.time.Instant;
import java.util.concurrent.Callable;

import static java.time.Duration.between;

@UtilityClass
public class TimeLimitInternal {
    private static boolean isExceed(@NonNull Instant checkTime, Instant deadline) {
        if (deadline == null) return false;
        val timeLeft = between(checkTime, deadline);
        val exceed = timeLeft.isZero() || timeLeft.isNegative();
        return exceed;
    }

    static <T> void timeLimitedRun(Instant deadline, Clock<Instant> clock,
                                   Runnable runnable,
                                   DeadlineExceedFunction<T> exceedFunction) {
        val checkTime = clock.time();
        if (isExceed(checkTime, deadline)) exceedFunction.apply(checkTime, deadline);
        else runnable.run();
    }

    @SneakyThrows
    static <T> T timeLimitedCall(Instant deadline, Clock<Instant> clock,
                                 Callable<T> callable, DeadlineExceedFunction<T> exceedFunction) {
        val checkTime = clock.time();
        if (isExceed(checkTime, deadline)) return exceedFunction.apply(checkTime, deadline);
        else return callable.call();
    }

    static <T> T connectionCall(Instant deadline, Clock<Instant> clock, TimeoutsFormula timeoutsFormula, TimeoutsFunction<T> function,
                                DeadlineExceedFunction<T> exceedFunction) {
        val checkTime = clock.time();
        return isExceed(checkTime, deadline)
                ? exceedFunction.apply(checkTime, deadline)
                : timeoutsFormula.calc(deadline, function);
    }
}
