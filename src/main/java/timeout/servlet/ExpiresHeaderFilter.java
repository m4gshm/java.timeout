package timeout.servlet;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import timeout.DeadlineExecutor;
import timeout.http.HttpDeadlineHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.function.Function;

import static java.time.ZonedDateTime.now;
import static timeout.http.HttpHeaders.*;
import static timeout.http.HttpStatuses.GATEWAY_TIMEOUT;

@Slf4j
@RequiredArgsConstructor
public class ExpiresHeaderFilter implements Filter {

    private final @NonNull String deadlineHeaderName;
    private final @NonNull String deadlineExceedHeaderName;
    private final @NonNull String deadlineCheckTimeHeaderName;
    private final int deadlineExceedStatus;
    private final @NonNull Function<String, Long> parser;
    private final @NonNull Function<Long, String> formatter;
    private final @NonNull Duration defaultDeadline;
    private final @NonNull DeadlineExecutor executor;

    public ExpiresHeaderFilter(DeadlineExecutor executor) {
        this(null, executor);
    }

    public ExpiresHeaderFilter(Duration defaultDeadline, DeadlineExecutor executor) {
        this(DEADLINE_HEADER, DEADLINE_EXCEED_HEADER, DEADLINE_CHECK_TIME_HEADER, GATEWAY_TIMEOUT,
                HttpDeadlineHelper::parseHttpDate, HttpDeadlineHelper::formatHttpDate, defaultDeadline, executor
        );
    }

    private static long calc(Duration defaultDeadline, Logger log) {
        val dateTime = now().plus(defaultDeadline);
        val deadline = dateTime.toInstant().toEpochMilli();
        log.trace("uses default deadline:{}, as date-time:{}", deadline, dateTime);
        return deadline;
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        val httpServletRequest = (HttpServletRequest) request;
        val expires = httpServletRequest.getHeader(deadlineHeaderName);
        var deadline = parser.apply(expires);
        if (deadline == null && defaultDeadline != null) deadline = calc(defaultDeadline, log);
        executor.run(deadline, () -> goChain(chain, request, response), (checkTime, deadlineExceed) -> {
            val httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setHeader(deadlineExceedHeaderName, formatter.apply(deadlineExceed));
            httpServletResponse.setHeader(deadlineCheckTimeHeaderName, formatter.apply(checkTime));
            httpServletResponse.setStatus(deadlineExceedStatus);
        });
    }

    @SneakyThrows
    private void goChain(FilterChain chain, ServletRequest request, ServletResponse response) {
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
