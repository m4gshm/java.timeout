package timeout.spring.config;

import feign.Client;
import feign.Feign;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.TimeLimitExecutor;
import timeout.feign.DeadlineedFeignClient;
import timeout.feign.FeignRequestTimeLimitStrategy;

@Configuration
@ConditionalOnClass({Feign.class})
@AutoConfigureAfter({FeignAutoConfiguration.class})
public class FeignClientAutoConfiguration {
    @Autowired
    TimeLimitExecutor service;
    @Autowired
    private FeignRequestTimeLimitStrategy timeLimitStrategy;

    @Bean
    public BeanPostProcessor deadlinedFeignClientProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof Client && !(bean instanceof DeadlineedFeignClient))
                    return new DeadlineedFeignClient(service, timeLimitStrategy, (Client) bean);
                else return bean;
            }
        };
    }

}
