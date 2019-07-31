package timeout.spring.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.ConnectTimeoutExecutor;
import timeout.spring.properties.ConnectTimeoutProperties;

@Configuration
@EnableConfigurationProperties(ConnectTimeoutProperties.class)
public class ConnectTimeoutServiceConfig {

    @Bean
    ConnectTimeoutExecutor connectTimeoutService(ConnectTimeoutProperties properties) {
        return new ConnectTimeoutExecutor(
                properties.getConnectionToRequestTimeoutRate(),
                properties.getNextDeadlineRate());
    }

}
