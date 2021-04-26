package m4gshm.java.timeout.feign;

import feign.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import m4gshm.java.timeout.DeadlineExceededException;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singleton;
import static m4gshm.java.timeout.http.HttpDeadlineHelper.newDeadlineExceedExceptionFromHeaders;

@RequiredArgsConstructor
@Slf4j
public class FeignRequestDeadlineStrategy implements FeignRequestTimeLimitStrategy {

    private final String deadlineHeaderName;
    private final String deadlineExceedHeaderName;
    private final String deadlineCheckTimeHeaderName;
    private final int deadlineExceedStatus;
    private final Function<String, Instant> parser;
    private final Function<Instant, String> formatter;

    @Override
    public void checkDeadlineExceed(Response response) throws DeadlineExceededException {
        if (deadlineExceedStatus == response.status()) {
            val headers = response.headers();
            val exception = newDeadlineExceedExceptionFromHeaders(headers,
                    deadlineExceedHeaderName, deadlineCheckTimeHeaderName, parser);
            if (exception != null) throw exception;

        }
    }

    @Override
    public HashMap<String, Collection<String>> putToHeaders(Instant readDeadline,
                                                            Map<String, Collection<String>> srcHeaders) {
        val headers = new HashMap<String, Collection<String>>(srcHeaders);
        val expires = formatter.apply(readDeadline);
        headers.put(deadlineHeaderName, singleton(expires));
        log.trace("converts readDeadline:{} to http headers. header:{}, value:{}",
                readDeadline, deadlineHeaderName, expires);
        return headers;
    }

}
