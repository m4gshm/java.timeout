package timeout.spring.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.DeadlineExecutor;
import timeout.spring.properties.DeadlineExecutorProperties;

@Configuration
@EnableConfigurationProperties(DeadlineExecutorProperties.class)
public class DeadlineExecutorAutoConfiguration {

    @Bean
    DeadlineExecutor deadlineExecutor(DeadlineExecutorProperties properties) {
        return new DeadlineExecutor(
                properties.getConnectionToRequestTimeoutRate(),
                properties.getChildDeadlineRate());
    }

}
