package timeout.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.DeadlineExecutor;
import timeout.ws.GlobalTimeoutHandler;

@Configuration
public class GlobalTimeoutHandlerAutoConfiguration {

    @Bean
    GlobalTimeoutHandler globalTimeoutHandler(DeadlineExecutor timeoutService) {
        return new GlobalTimeoutHandler(timeoutService);
    }
}
