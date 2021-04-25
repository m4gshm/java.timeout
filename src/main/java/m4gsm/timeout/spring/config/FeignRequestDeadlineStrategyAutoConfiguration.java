package m4gsm.timeout.spring.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import m4gsm.timeout.feign.FeignRequestDeadlineStrategy;
import m4gsm.timeout.feign.FeignRequestTimeLimitStrategy;
import m4gsm.timeout.http.HttpDeadlineHelper;

import static m4gsm.timeout.http.HttpHeaders.*;
import static m4gsm.timeout.http.HttpStatuses.GATEWAY_TIMEOUT;

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
