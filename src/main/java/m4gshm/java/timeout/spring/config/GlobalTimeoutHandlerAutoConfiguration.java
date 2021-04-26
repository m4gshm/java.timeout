package m4gshm.java.timeout.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import m4gshm.java.timeout.TimeLimitExecutor;
import m4gshm.java.timeout.ws.GlobalTimeoutHandler;

@Configuration
public class GlobalTimeoutHandlerAutoConfiguration {

    @Bean
    GlobalTimeoutHandler globalTimeoutHandler(TimeLimitExecutor timeoutService) {
        return new GlobalTimeoutHandler(timeoutService);
    }
}
