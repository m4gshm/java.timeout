package timeout.http;

import lombok.experimental.UtilityClass;
import lombok.val;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static java.time.Instant.from;
import static java.time.Instant.ofEpochMilli;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

@UtilityClass
public class HttpDateHelper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = RFC_1123_DATE_TIME;

    public static Long parseHttpDate(String value) {
        if (value == null) return null;
        val temporalAccessor = RFC_1123_DATE_TIME.parse(value);
        return from(temporalAccessor).toEpochMilli();
    }

    public static String formatHttpDate(long epochMilli) {
        val temporal = ofEpochMilli(epochMilli);
        val from = ZonedDateTime.ofInstant(temporal, ZoneId.systemDefault());
        val format = DATE_TIME_FORMATTER.format(from);
        return format;
    }
}
