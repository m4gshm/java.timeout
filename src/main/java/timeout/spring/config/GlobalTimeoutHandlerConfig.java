package timeout.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.ConnectTimeoutExecutor;
import timeout.ws.GlobalTimeoutHandler;

@Configuration
public class GlobalTimeoutHandlerConfig {

    @Bean
    GlobalTimeoutHandler globalTimeoutHandler(ConnectTimeoutExecutor timeoutService) {
        return new GlobalTimeoutHandler(timeoutService);
    }
}
