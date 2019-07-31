package timeout.servlet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import timeout.DeadlineHolder;
import timeout.http.HttpDateHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;

import static timeout.DeadlineHolder.calc;
import static timeout.http.HttpHeaders.EXPIRES_HEADER;

@Slf4j
@RequiredArgsConstructor
public class ExpiresHeaderFilter implements Filter {

    private final String headerName;
    private final Function<String, Long> parser;
    private final Duration defaultDeadline;

    public ExpiresHeaderFilter() {
        this(null);
    }

    public ExpiresHeaderFilter(Duration defaultDeadline) {
        this(EXPIRES_HEADER, HttpDateHelper::parseHttpDate, defaultDeadline);
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var clear = false;
        try {
            val httpServletRequest = (HttpServletRequest) request;
            val expires = httpServletRequest.getHeader(headerName);
            var deadline = parser.apply(expires);
            if (deadline == null && defaultDeadline != null) deadline = calc(defaultDeadline, log);
            DeadlineHolder.setDeadline(deadline);
            clear = true;
        } finally {
            try {
                chain.doFilter(request, response);
            } finally {
                if (clear) DeadlineHolder.clear();
            }
        }
    }


    @Override
    public void destroy() {

    }
}
