package timeout.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import timeout.DeadlineExecutor;
import timeout.http.HttpDeadlineHelper;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static javax.xml.ws.handler.MessageContext.*;
import static timeout.http.HttpDeadlineHelper.newDeadlineExceedExceptionFromHeaders;
import static timeout.http.HttpHeaders.*;
import static timeout.http.HttpStatuses.GATEWAY_TIMEOUT;

@Slf4j
@RequiredArgsConstructor
public class GlobalTimeoutHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String JAVAX_XML_WS_SERVICE_ENDPOINT_ADDRESS = "javax.xml.ws.service.endpoint.address";
    private static final String COM_SUN_XML_INTERNAL_WS_CONNECT_TIMEOUT = "com.sun.xml.internal.ws.connect.timeout";
    private static final String COM_SUN_XML_INTERNAL_WS_REQUEST_TIMEOUT = "com.sun.xml.internal.ws.request.timeout";
    private final DeadlineExecutor executor;
    private final String deadlineHeaderName;
    private final String deadlineExceedHeaderName;
    private final String deadlineCheckTimeHeaderName;
    private final int deadlineExceedStatus;
    private final Function<String, Long> parser;
    private final Function<Long, String> formatter;

    public GlobalTimeoutHandler(DeadlineExecutor executor) {
        this(executor, DEADLINE_HEADER, DEADLINE_EXCEED_HEADER, DEADLINE_CHECK_TIME_HEADER, GATEWAY_TIMEOUT,
                HttpDeadlineHelper::parseHttpDate,
                HttpDeadlineHelper::formatHttpDate
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> getHeaders(SOAPMessageContext context, String type) {
        var headers = (Map<String, List<String>>) context.get(type);
        if (headers == null) headers = new HashMap();
        return headers;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        val request = isRequest(context);
        if (request) handleRequest(context);
        else handleResponse(context);
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        val request = isRequest(context);
        if (!request) handleResponse(context);
        return true;
    }

    private boolean isRequest(SOAPMessageContext context) {
        return TRUE.equals(context.get(MESSAGE_OUTBOUND_PROPERTY));
    }

    private void handleRequest(SOAPMessageContext context) {
        executor.run((connectTimeout, requestTimeout, readDeadline) -> {
            val setConnectTO = connectTimeout != null && connectTimeout >= 0;
            if (setConnectTO) {
                context.put(COM_SUN_XML_INTERNAL_WS_CONNECT_TIMEOUT, connectTimeout.intValue());
            }

            val setRequestTO = requestTimeout != null && requestTimeout >= 0;
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
                val headers = getHeaders(context, HTTP_REQUEST_HEADERS);
                val expires = formatter.apply(readDeadline);
                headers.put(deadlineHeaderName, singletonList(expires));
                log.trace("converts readDeadline:{} to http headers. header:{}, value:{}", readDeadline, deadlineHeaderName, expires);
                context.put(HTTP_REQUEST_HEADERS, headers);
            }
        });
    }

    private void handleResponse(SOAPMessageContext context) {
        val rawStatus = context.get(HTTP_RESPONSE_CODE);
        if (rawStatus instanceof Integer && (Integer) deadlineExceedStatus == rawStatus) {
            val headers = getHeaders(context, HTTP_RESPONSE_HEADERS);
            val exception = newDeadlineExceedExceptionFromHeaders(headers,
                    deadlineExceedHeaderName, deadlineCheckTimeHeaderName, parser);
            if (exception != null) throw exception;
        }
    }


    @Override
    public void close(MessageContext context) {

    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }
}
