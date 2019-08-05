package timeout.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.TimeLimitExecutorImpl;
import timeout.ws.GlobalTimeoutHandler;

@Configuration
public class GlobalTimeoutHandlerAutoConfiguration {

    @Bean
    GlobalTimeoutHandler globalTimeoutHandler(TimeLimitExecutorImpl timeoutService) {
        return new GlobalTimeoutHandler(timeoutService);
    }
}
