package timeout.http;

import lombok.experimental.UtilityClass;
import lombok.val;
import timeout.DeadlineExceededException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static java.time.Instant.from;
import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

@UtilityClass
public class HttpDeadlineHelper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = RFC_1123_DATE_TIME;

    public static Long parseHttpDate(String value) {
        if (value == null) return null;
        val temporalAccessor = RFC_1123_DATE_TIME.parse(value);
        return from(temporalAccessor).toEpochMilli();
    }

    public static String formatHttpDate(Long epochMilli) {
        if (epochMilli == null) return null;
        val temporal = ofEpochMilli(epochMilli);
        val from = ZonedDateTime.ofInstant(temporal, UTC);
        val format = DATE_TIME_FORMATTER.format(from);
        return format;
    }

    public static DeadlineExceededException newDeadlineExceedExceptionFromHeaders(
            Map<String, ? extends Collection<String>> headers,
            String deadlineExceedHeaderName, String deadlineCheckTimeHeaderName,
            Function<String, Long> parser
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
