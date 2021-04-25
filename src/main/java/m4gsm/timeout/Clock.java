package m4gsm.timeout;

import java.time.Instant;

public interface Clock<T> {
    static Clock<Instant> now() {
        return Instant::now;
    }

    T time();
}
