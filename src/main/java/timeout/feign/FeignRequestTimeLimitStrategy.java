package timeout.feign;

import feign.Response;
import timeout.DeadlineExceededException;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public interface FeignRequestTimeLimitStrategy {
    void checkDeadlineExceed(Response response) throws DeadlineExceededException;

    HashMap<String, Collection<String>> putToHeaders(Instant readDeadline,
                                                     Map<String, Collection<String>> srcHeaders);
}
