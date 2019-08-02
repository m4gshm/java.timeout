package timeout.http;

import lombok.val;
import org.junit.Test;

import static java.time.Instant.ofEpochMilli;
import static java.time.OffsetDateTime.of;
import static java.time.OffsetDateTime.ofInstant;
import static java.time.ZoneOffset.UTC;
import static org.junit.Assert.assertEquals;

public class HttpDateTest {

    @Test
    public void testFormat() {
        val dateTime = of(2018, 1, 1, 1, 1, 1, 0, UTC);
        val epochMilli = dateTime.toInstant().toEpochMilli();
        val expiresValue = HttpDateHelper.formatHttpDate(epochMilli);

        assertEquals("Mon, 1 Jan 2018 01:01:01 GMT", expiresValue);
    }


    @Test
    public void testParse() {
        val expected = of(2018, 1, 1, 1, 1, 1, 0, UTC);
        val epochMilli = HttpDateHelper.parseHttpDate("Mon, 1 Jan 2018 01:01:01 GMT");
        val expiresDateTime = ofInstant(ofEpochMilli(epochMilli), UTC);
        assertEquals(expected, expiresDateTime);
    }

}
