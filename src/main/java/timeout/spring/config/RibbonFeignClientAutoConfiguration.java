package timeout.spring.config;

import com.netflix.loadbalancer.ILoadBalancer;
import feign.Client;
import feign.Feign;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({ILoadBalancer.class, Feign.class})
@AutoConfigureAfter({FeignAutoConfiguration.class, FeignRibbonClientAutoConfiguration.class, FeignClientAutoConfiguration.class})
public class RibbonFeignClientAutoConfiguration {

    @Bean
    public BeanPostProcessor ribbonLoadBalancerFeignClientProcessor(
            CachingSpringLoadBalancerFactory cachingFactory, SpringClientFactory clientFactory) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof Client && !(bean instanceof LoadBalancerFeignClient))
                    return new LoadBalancerFeignClient((Client) bean, cachingFactory, clientFactory);
                else return bean;
            }
        };
    }
}
