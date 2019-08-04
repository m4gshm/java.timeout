package timeout.feign;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import timeout.DeadlineExecutor;
import timeout.http.HttpDeadlineHelper;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;

import static feign.Request.create;
import static java.util.Collections.singleton;
import static timeout.http.HttpDeadlineHelper.newDeadlineExceedExceptionFromHeaders;
import static timeout.http.HttpHeaders.*;
import static timeout.http.HttpStatuses.GATEWAY_TIMEOUT;

@Slf4j
public class DeadlineDefaultFeignClient extends Client.Default {

    private final DeadlineExecutor executor;
    private final String deadlineHeaderName;
    private final String deadlineExceedHeaderName;
    private final String deadlineCheckTimeHeaderName;
    private final int deadlineExceedStatus;
    private final Function<String, Long> parser;
    private final Function<Long, String> formatter;

    public DeadlineDefaultFeignClient(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier,
                                      @NonNull DeadlineExecutor executor, @NonNull String deadlineHeaderName,
                                      @NonNull String deadlineExceedHeaderName,
                                      @NonNull String deadlineCheckTimeHeaderName,
                                      int deadlineExceedStatus,
                                      @NonNull Function<String, Long> parser,
                                      @NonNull Function<Long, String> formatter
    ) {
        super(sslContextFactory, hostnameVerifier);
        this.executor = executor;
        this.deadlineHeaderName = deadlineHeaderName;
        this.deadlineExceedHeaderName = deadlineExceedHeaderName;
        this.deadlineCheckTimeHeaderName = deadlineCheckTimeHeaderName;
        this.deadlineExceedStatus = deadlineExceedStatus;
        this.parser = parser;
        this.formatter = formatter;
    }

    public DeadlineDefaultFeignClient(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier,
                                      DeadlineExecutor executor) {
        this(sslContextFactory, hostnameVerifier, executor,
                DEADLINE_HEADER, DEADLINE_EXCEED_HEADER,
                DEADLINE_CHECK_TIME_HEADER, GATEWAY_TIMEOUT,
                HttpDeadlineHelper::parseHttpDate, HttpDeadlineHelper::formatHttpDate
        );
    }

    private static Options newOptions(Options options, long connectionTimeout, long readTimeout) {
        return new Options((int) connectionTimeout, (int) readTimeout, options.isFollowRedirects());
    }

    @Override
    public Response execute(Request request, Options options) {
        return executor.call((connectionTimeout, readTimeout, readDeadline) -> {
            Options newOptions;
            val setConnectionTO = connectionTimeout != null && connectionTimeout >= 0;
            val setRequestTO = readTimeout != null && readTimeout >= 0;
            val url = request.url();
            if (setConnectionTO || setRequestTO) {
                newOptions = newOptions(options, setConnectionTO ? connectionTimeout : options.connectTimeoutMillis(),
                        setRequestTO ? readTimeout : options.readTimeoutMillis());
                if (log.isTraceEnabled()) log.trace("url:{} {}", url,
                        (setConnectionTO ? "connectTimeout:" + connectionTimeout : "") +
                                (setConnectionTO && setRequestTO ? ", " : "") +
                                (setRequestTO ? "readTimeout:" + readTimeout : ""));
            } else {
                log.debug("connectionTimeout:{} or readTimeout:{} equal null or less than 0",
                        connectionTimeout, readTimeout);
                newOptions = options;
            }
            var newRequest = request;
            if (readDeadline != null) {
                val headers = new HashMap<String, Collection<String>>(request.headers());
                val expires = formatter.apply(readDeadline);
                headers.put(deadlineHeaderName, singleton(expires));
                log.trace("converts readDeadline:{} to http headers. header:{}, value:{}",
                        readDeadline, deadlineHeaderName, expires);
                newRequest = create(request.httpMethod(), url, headers, request.requestBody());
            }
            val response = superExecute(newRequest, newOptions);
            if (deadlineExceedStatus == response.status()) {
                val headers = response.headers();
                val exception = newDeadlineExceedExceptionFromHeaders(headers,
                        deadlineExceedHeaderName, deadlineCheckTimeHeaderName, parser);
                if (exception != null) throw exception;

            }
            return response;
        });
    }

    @SneakyThrows
    private Response superExecute(Request request, Options options) {
        return super.execute(request, options);
    }

}
