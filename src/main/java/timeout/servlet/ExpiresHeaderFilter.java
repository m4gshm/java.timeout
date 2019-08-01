package timeout.servlet;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.slf4j.Logger;
import timeout.DeadlineExecutor;
import timeout.http.HttpDateHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.function.Function;

import static java.time.ZonedDateTime.now;
import static java.util.Objects.requireNonNull;
import static timeout.http.HttpHeaders.EXPIRES_HEADER;

@Slf4j
public class ExpiresHeaderFilter implements Filter {

    private final String headerName;
    private final Function<String, Long> parser;
    private final Duration defaultDeadline;
    private final DeadlineExecutor executor;

    public ExpiresHeaderFilter(DeadlineExecutor executor) {
        this(null, executor);
    }

    public ExpiresHeaderFilter(Duration defaultDeadline, DeadlineExecutor executor) {
        this(EXPIRES_HEADER, HttpDateHelper::parseHttpDate, defaultDeadline, executor);
    }

    public ExpiresHeaderFilter(String headerName, Function<String, Long> parser,
                               Duration defaultDeadline, DeadlineExecutor executor) {
        this.headerName = requireNonNull(headerName, "headerName");
        this.parser = requireNonNull(parser, "parser");
        this.defaultDeadline = defaultDeadline;
        this.executor = requireNonNull(executor, "executor");
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
        val expires = httpServletRequest.getHeader(headerName);
        var deadline = parser.apply(expires);
        if (deadline == null && defaultDeadline != null) deadline = calc(defaultDeadline, log);
        executor.run(deadline, () -> goChain(chain, request, response));
    }

    @SneakyThrows
    private void goChain(FilterChain chain, ServletRequest request, ServletResponse response) {
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
