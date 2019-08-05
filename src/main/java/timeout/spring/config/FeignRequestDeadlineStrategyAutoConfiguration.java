package timeout.spring.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.feign.FeignRequestDeadlineStrategy;
import timeout.feign.FeignRequestTimeLimitStrategy;
import timeout.http.HttpDeadlineHelper;

import static timeout.http.HttpHeaders.*;
import static timeout.http.HttpStatuses.GATEWAY_TIMEOUT;

@Configuration
public class FeignRequestDeadlineStrategyAutoConfiguration {
    @ConditionalOnMissingBean(FeignRequestTimeLimitStrategy.class)
    @Bean
    FeignRequestTimeLimitStrategy timeLimitStrategy() {
        return new FeignRequestDeadlineStrategy(DEADLINE_HEADER,
                DEADLINE_EXCEED_HEADER, DEADLINE_CHECK_TIME_HEADER, GATEWAY_TIMEOUT,
                HttpDeadlineHelper::parseHttpDate,
                HttpDeadlineHelper::formatHttpDate);
    }
}
