package m4gshm.java.timeout.http;

import lombok.val;
import org.junit.Test;

import static java.time.OffsetDateTime.of;
import static java.time.OffsetDateTime.ofInstant;
import static java.time.ZoneOffset.UTC;
import static org.junit.Assert.assertEquals;

public class HttpDateTest {

    @Test
    public void testFormat() {
        val dateTime = of(2018, 1, 1, 1, 1, 1, 0, UTC);
        val epochMilli = dateTime.toInstant();
        val expiresValue = HttpDeadlineHelper.formatHttpDate(epochMilli);

        assertEquals("Mon, 1 Jan 2018 01:01:01 GMT", expiresValue);
    }


    @Test
    public void testParse() {
        val expected = of(2018, 1, 1, 1, 1, 1, 0, UTC);
        val epochMilli = HttpDeadlineHelper.parseHttpDate("Mon, 1 Jan 2018 01:01:01 GMT");
        val expiresDateTime = ofInstant(epochMilli, UTC);
        assertEquals(expected, expiresDateTime);
    }

}
