package m4gsm.timeout.http;

import lombok.experimental.UtilityClass;
import lombok.val;
import m4gsm.timeout.DeadlineExceededException;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static java.time.Instant.from;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

@UtilityClass
public class HttpDeadlineHelper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = RFC_1123_DATE_TIME;

    public static Instant parseHttpDate(String value) {
        if (value == null) return null;
        val temporalAccessor = RFC_1123_DATE_TIME.parse(value);
        return from(temporalAccessor);
    }

    public static String formatHttpDate(Instant instant) {
        if (instant == null) return null;
        val from = ZonedDateTime.ofInstant(instant, UTC);
        return DATE_TIME_FORMATTER.format(from);
    }

    public static DeadlineExceededException newDeadlineExceedExceptionFromHeaders(
            Map<String, ? extends Collection<String>> headers,
            String deadlineExceedHeaderName, String deadlineCheckTimeHeaderName,
            Function<String, Instant> parser
    ) {
        val exceedsVal = headers.get(deadlineExceedHeaderName);
        val checkTimeVal = headers.get(deadlineCheckTimeHeaderName);
        val exceedStr = getFirst(exceedsVal);
        val checkTimeStr = getFirst(checkTimeVal);
        if (exceedStr != null) {
            val checkTime = parser.apply(checkTimeStr);
            val deadline = parser.apply(exceedStr);
            return new DeadlineExceededException(checkTime, deadline);
        } else return null;
    }

    private static String getFirst(Collection<String> collection) {
        return !(collection == null || collection.isEmpty()) ? collection.iterator().next() : null;
    }

}
