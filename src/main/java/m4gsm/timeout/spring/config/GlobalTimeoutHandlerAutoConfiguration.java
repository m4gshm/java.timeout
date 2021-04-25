package m4gsm.timeout.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import m4gsm.timeout.TimeLimitExecutor;
import m4gsm.timeout.ws.GlobalTimeoutHandler;

@Configuration
public class GlobalTimeoutHandlerAutoConfiguration {

    @Bean
    GlobalTimeoutHandler globalTimeoutHandler(TimeLimitExecutor timeoutService) {
        return new GlobalTimeoutHandler(timeoutService);
    }
}
