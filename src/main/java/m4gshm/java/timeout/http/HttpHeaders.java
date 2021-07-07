package m4gshm.java.timeout.http;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpHeaders {
    public static final String DEADLINE_HEADER = "X-Deadline";
    public static final String DEADLINE_EXCEED_HEADER = "X-Deadline-Exceed";
    public static final String DEADLINE_CHECK_TIME_HEADER = "X-Deadline-Check-Time";
}
