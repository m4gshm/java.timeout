package m4gsm.timeout.servlet;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import m4gsm.timeout.TimeLimitExecutor;
import m4gsm.timeout.http.HttpDeadlineHelper;
import m4gsm.timeout.http.HttpHeaders;
import m4gsm.timeout.http.HttpStatuses;
import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import static java.time.ZonedDateTime.now;

@Slf4j
@RequiredArgsConstructor
public class DeadlineHeaderFilter implements Filter {

    private final @NonNull String deadlineHeaderName;
    private final @NonNull String deadlineExceedHeaderName;
    private final @NonNull String deadlineCheckTimeHeaderName;
    private final int deadlineExceedStatus;
    private final @NonNull Function<String, Instant> parser;
    private final @NonNull Function<Instant, String> formatter;
    private final Duration defaultDeadline;
    private final @NonNull TimeLimitExecutor executor;

    public DeadlineHeaderFilter(TimeLimitExecutor executor) {
        this(null, executor);
    }

    public DeadlineHeaderFilter(Duration defaultDeadline, TimeLimitExecutor executor) {
        this(HttpHeaders.DEADLINE_HEADER, HttpHeaders.DEADLINE_EXCEED_HEADER, HttpHeaders.DEADLINE_CHECK_TIME_HEADER, HttpStatuses.GATEWAY_TIMEOUT,
                HttpDeadlineHelper::parseHttpDate, HttpDeadlineHelper::formatHttpDate, defaultDeadline, executor
        );
    }

    private static Instant calc(Duration defaultDeadline, Logger log) {
        val dateTime = now().plus(defaultDeadline);
        val deadline = dateTime.toInstant();
        log.trace("uses default deadline:{}", deadline);
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
        executor.run(deadline, context -> context.run(() -> goChain(chain, request, response)), (checkTime, deadlineExceed) -> {
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
