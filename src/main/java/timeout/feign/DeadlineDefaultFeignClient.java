package timeout.feign;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import timeout.DeadlineExecutor;
import timeout.http.HttpDateHelper;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;

import static feign.Request.create;
import static java.util.Collections.singleton;
import static timeout.http.HttpHeaders.EXPIRES_HEADER;

@Slf4j
public class DeadlineDefaultFeignClient extends Client.Default {

    private final DeadlineExecutor executor;
    private final String expiresHeaderName;
    private final Function<Long, String> formatter;

    public DeadlineDefaultFeignClient(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier,
                                      DeadlineExecutor executor, String expiresHeaderName,
                                      Function<Long, String> formatter) {
        super(sslContextFactory, hostnameVerifier);
        this.executor = executor;
        this.expiresHeaderName = expiresHeaderName;
        this.formatter = formatter;
    }

    public DeadlineDefaultFeignClient(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier,
                                      DeadlineExecutor executor) {
        this(sslContextFactory, hostnameVerifier, executor, EXPIRES_HEADER, HttpDateHelper::formatHttpDate);
    }

    private static Options newOptions(Options options, long connectionTimeout, long requestTimeout) {
        return new Options((int) connectionTimeout, (int) requestTimeout, options.isFollowRedirects());
    }

    @Override
    public Response execute(Request request, Options options) {
        return executor.call((time, connectionTimeout, requestTimeout) -> {
            Options newOptions;
            boolean setConnectionTO = connectionTimeout != null && connectionTimeout >= 0;
            boolean setRequestTO = requestTimeout != null && requestTimeout >= 0;
            val url = request.url();
            if (setConnectionTO || setRequestTO) {
                newOptions = newOptions(options, setConnectionTO ? connectionTimeout : options.connectTimeoutMillis(),
                        setRequestTO ? requestTimeout : options.readTimeoutMillis());

                if (log.isTraceEnabled()) log.trace("url:{} timeouts {}", url,
                        (setConnectionTO ? "connect:" + connectionTimeout : "") +
                                (setConnectionTO && setRequestTO ? ", " : "") +
                                (setRequestTO ? "request:" + requestTimeout : ""));


            } else {
                log.debug("connectionTimeout:{} or requestTimeout:{} equal null or less than 0",
                        connectionTimeout, requestTimeout);
                newOptions = options;
            }
            var newRequest = request;
            Long deadline = requestTimeout != null ? time + requestTimeout : null;
            if (deadline != null) {
                val headers = new HashMap<String, Collection<String>>(request.headers());
                val expires = formatter.apply(deadline);
                headers.put(expiresHeaderName, singleton(expires));
                log.trace("converts deadline:{} to http headers. header:{}, value:{}", deadline, expiresHeaderName, expires);
                newRequest = create(request.httpMethod(), url, headers, request.requestBody());
            }
            return superExecute(newRequest, newOptions);
        });
    }

    @SneakyThrows
    private Response superExecute(Request request, Options options) {
        return super.execute(request, options);
    }

}
