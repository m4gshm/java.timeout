package timeout.feign;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import timeout.TimeLimitExecutor;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.time.Instant;

import static feign.Request.create;

@Slf4j
public class DeadlineDefaultFeignClient extends Client.Default {

    private final TimeLimitExecutor executor;
    private final FeignRequestTimeLimitStrategy timeLimitStrategy;

    public DeadlineDefaultFeignClient(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier,
                                      TimeLimitExecutor executor, FeignRequestTimeLimitStrategy timeLimitStrategy) {
        super(sslContextFactory, hostnameVerifier);
        this.executor = executor;
        this.timeLimitStrategy = timeLimitStrategy;
    }

    private static Options newOptions(Options options, long connectionTimeout, long readTimeout) {
        return new Options((int) connectionTimeout, (int) readTimeout, options.isFollowRedirects());
    }

    @Override
    public Response execute(Request request, Options options) {
        return executor.call(context -> context.timeouts((connectionTimeout, readTimeout, readDeadline) -> {
            Options newOptions;
            val setConnectionTO = connectionTimeout != null && connectionTimeout.toMillis() >= 0;
            val setRequestTO = readTimeout != null && readTimeout.toMillis() >= 0;
            val url = request.url();
            if (setConnectionTO || setRequestTO) {
                newOptions = newOptions(options, setConnectionTO ? connectionTimeout.toMillis() : options.connectTimeoutMillis(),
                        setRequestTO ? readTimeout.toMillis() : options.readTimeoutMillis());
                if (log.isTraceEnabled()) log.trace("url:{} {}", url,
                        (setConnectionTO ? "connectTimeout:" + connectionTimeout : "") +
                                (setConnectionTO && setRequestTO ? ", " : "") +
                                (setRequestTO ? "readTimeout:" + readTimeout : ""));
            } else {
                log.debug("connectionTimeout:{} or readTimeout:{} equal null or less than 0",
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
        if (readDeadline != null) newRequest = create(request.httpMethod(), url,
                timeLimitStrategy.putToHeaders(readDeadline, request.headers()), request.requestBody());
        return newRequest;
    }


    @SneakyThrows
    private Response superExecute(Request request, Options options) {
        return super.execute(request, options);
    }

}
