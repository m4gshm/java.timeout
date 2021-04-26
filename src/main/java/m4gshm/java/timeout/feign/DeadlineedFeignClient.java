package m4gshm.java.timeout.feign;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import m4gshm.java.timeout.TimeLimitExecutor;

import java.time.Duration;
import java.time.Instant;

import static feign.Request.create;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class DeadlineedFeignClient implements Client {

    private final TimeLimitExecutor executor;
    private final FeignRequestTimeLimitStrategy timeLimitStrategy;
    private final Client delegate;

    public DeadlineedFeignClient(TimeLimitExecutor executor, FeignRequestTimeLimitStrategy timeLimitStrategy, Client delegate) {
        this.executor = executor;
        this.timeLimitStrategy = timeLimitStrategy;
        this.delegate = delegate;
    }


    private static Options newOptions(Options options, Duration connectionTimeout, Duration readTimeout,
                                      boolean setConnectionTO, boolean setRequestTO) {
        return new Options(
                setConnectionTO ? connectionTimeout.toMillis() : options.connectTimeoutMillis(), MILLISECONDS,
                setRequestTO ? readTimeout.toMillis() : options.readTimeoutMillis(), MILLISECONDS,
                options.isFollowRedirects()
        );
    }

    @Override
    public Response execute(Request request, Options options) {
        return executor.call(context -> context.timeouts((connectionTimeout, readTimeout, readDeadline) -> {
            Options newOptions;
            val setConnectionTO = connectionTimeout != null && !(connectionTimeout.isZero() || connectionTimeout.isNegative());
            val setRequestTO = readTimeout != null && !(readTimeout.isZero() || readTimeout.isNegative());
            val url = request.url();
            if (setConnectionTO || setRequestTO) {
                newOptions = newOptions(options, connectionTimeout, readTimeout, setConnectionTO, setRequestTO);
                if (log.isTraceEnabled()) log.trace("url {} {}", url,
                        (setConnectionTO ? "connectTimeout " + connectionTimeout : "") +
                                (setConnectionTO && setRequestTO ? ", " : "") +
                                (setRequestTO ? "readTimeout " + readTimeout : ""));
            } else {
                log.debug("connectionTimeout {} or readTimeout {} equal null or less than 0",
                        connectionTimeout, readTimeout);
                newOptions = options;
            }
            var newRequest = putToHeaders(request, readDeadline, url);
            val response = superExecute(newRequest, newOptions);
            timeLimitStrategy.checkDeadlineExceed(response);
            return response;
        }));
    }

    protected Request putToHeaders(Request request, Instant readDeadline, String url) {
        Request newRequest = request;
        Request.Body body = Request.Body.create(request.body(), request.charset());
        if (readDeadline != null) newRequest = create(request.httpMethod(), url,
                timeLimitStrategy.putToHeaders(readDeadline, request.headers()), body, request.requestTemplate());
        return newRequest;
    }


    @SneakyThrows
    private Response superExecute(Request request, Options options) {
        return delegate.execute(request, options);
    }

}
