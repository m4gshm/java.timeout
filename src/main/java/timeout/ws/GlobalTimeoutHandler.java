package timeout.ws;

import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public class GlobalTimeoutHandler implements SOAPHandler<SOAPMessageContext> {

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
        executor.run((Long deadline, long connectTimeout, long requestTimeout) -> {
            if (connectTimeout >= 0) context.put(COM_SUN_XML_INTERNAL_WS_CONNECT_TIMEOUT, (int) connectTimeout);
            if (requestTimeout >= 0) context.put(COM_SUN_XML_INTERNAL_WS_REQUEST_TIMEOUT, (int) requestTimeout);
            var headers = (Map<String, Object>) context.get(HTTP_REQUEST_HEADERS);
            if (headers == null) headers = new HashMap();
            headers.put(headerName, singletonList(formatter.apply(deadline)));
            context.put(HTTP_REQUEST_HEADERS, headers);
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
