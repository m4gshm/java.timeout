package timeout.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import timeout.DeadlineExecutor;
import timeout.http.HttpDateHelper;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static javax.xml.ws.handler.MessageContext.HTTP_REQUEST_HEADERS;
import static timeout.http.HttpHeaders.EXPIRES_HEADER;

@Slf4j
@RequiredArgsConstructor
public class GlobalTimeoutHandler implements SOAPHandler<SOAPMessageContext> {

    public static final String JAVAX_XML_WS_SERVICE_ENDPOINT_ADDRESS = "javax.xml.ws.service.endpoint.address";
    private static final String COM_SUN_XML_INTERNAL_WS_CONNECT_TIMEOUT = "com.sun.xml.internal.ws.connect.timeout";
    private static final String COM_SUN_XML_INTERNAL_WS_REQUEST_TIMEOUT = "com.sun.xml.internal.ws.request.timeout";
    private final DeadlineExecutor executor;
    private final String headerName;
    private final Function<Long, String> formatter;

    public GlobalTimeoutHandler(DeadlineExecutor executor) {
        this(executor, EXPIRES_HEADER, HttpDateHelper::formatHttpDate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean handleMessage(SOAPMessageContext context) {
        executor.run((connectTimeout, requestTimeout, readDeadline) -> {
            val setConnectTO = connectTimeout != null && connectTimeout >= 0;
            if (setConnectTO) {
                context.put(COM_SUN_XML_INTERNAL_WS_CONNECT_TIMEOUT, connectTimeout.intValue());
            }

            boolean setRequestTO = requestTimeout != null && requestTimeout >= 0;
            if (setRequestTO) {
                context.put(COM_SUN_XML_INTERNAL_WS_REQUEST_TIMEOUT, requestTimeout.intValue());
            }

            if (setConnectTO || setRequestTO && log.isTraceEnabled()) {
                val endpoint = context.get(JAVAX_XML_WS_SERVICE_ENDPOINT_ADDRESS);
                log.trace("endpoint:{} {}", endpoint, (setConnectTO ? "connectTimeout:" + connectTimeout : "") +
                        (setConnectTO && setRequestTO ? ", " : "") +
                        (setRequestTO ? "readTimeout:" + requestTimeout : ""));
            }

            if (readDeadline != null) {
                var headers = (Map<String, Object>) context.get(HTTP_REQUEST_HEADERS);
                if (headers == null) headers = new HashMap();
                val expires = formatter.apply(readDeadline);
                headers.put(headerName, singletonList(expires));
                log.trace("converts readDeadline:{} to http headers. header:{}, value:{}", readDeadline, headerName, expires);
                context.put(HTTP_REQUEST_HEADERS, headers);
            }
        });
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {

    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }
}
