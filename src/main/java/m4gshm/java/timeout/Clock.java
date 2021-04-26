package m4gshm.java.timeout;

import java.time.Instant;

public interface Clock<T> {
    static Clock<Instant> now() {
        return Instant::now;
    }

    T time();
}
