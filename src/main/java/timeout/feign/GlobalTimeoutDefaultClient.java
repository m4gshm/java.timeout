package timeout.feign;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;
import lombok.SneakyThrows;
import lombok.val;
import timeout.ConnectTimeoutExecutor;
import timeout.http.HttpDateHelper;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static feign.Request.create;
import static java.util.Collections.singleton;
import static timeout.http.HttpHeaders.EXPIRES_HEADER;

public class GlobalTimeoutDefaultClient extends Client.Default {

    private final ConnectTimeoutExecutor executor;
    private final String expiresHeaderName;
    private final Function<Long, String> formatter;

    public GlobalTimeoutDefaultClient(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier,
                                      ConnectTimeoutExecutor executor, String expiresHeaderName,
                                      Function<Long, String> formatter) {
        super(sslContextFactory, hostnameVerifier);
        this.executor = executor;
        this.expiresHeaderName = expiresHeaderName;
        this.formatter = formatter;
    }

    public GlobalTimeoutDefaultClient(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier,
                                      ConnectTimeoutExecutor executor) {
        this(sslContextFactory, hostnameVerifier, executor, EXPIRES_HEADER, HttpDateHelper::formatHttpDate);
    }

    private static Options newOptions(Options options, long connectionTimeout, long requestTimeout) {
        return new Options((int) connectionTimeout, (int) requestTimeout, options.isFollowRedirects());
    }

    @Override
    public Response execute(Request request, Options options) {
        return executor.call(((deadline, connectionTimeout, requestTimeout) -> {
            val newOptions = newOptions(options, connectionTimeout, requestTimeout);
            val headers = new HashMap<String, Collection<String>>(request.headers());
            headers.put(expiresHeaderName, singleton(formatter.apply(deadline)));

            val newRequest = create(request.httpMethod(), request.url(), headers, request.requestBody());
            return superExecute(newRequest, newOptions);
        }));
    }

    @SneakyThrows
    private Response superExecute(Request request, Options options) {
        return super.execute(request, options);
    }

}
