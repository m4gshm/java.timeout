package timeout.spring.config;

import com.netflix.loadbalancer.ILoadBalancer;
import feign.Client;
import feign.Feign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.DeadlineExecutor;
import timeout.feign.DeadlineDefaultFeignClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

@Configuration
@ConditionalOnClass({ILoadBalancer.class, Feign.class})
@AutoConfigureBefore({FeignAutoConfiguration.class, FeignRibbonClientAutoConfiguration.class, FeignClientAutoConfiguration.class})
public class RibbonFeignClientAutoConfiguration {


    @Autowired(required = false)
    SSLSocketFactory sslContextFactory;
    @Autowired(required = false)
    HostnameVerifier hostnameVerifier;
    @Autowired
    DeadlineExecutor service;

    @ConditionalOnMissingBean
    @Bean
    public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
                              SpringClientFactory clientFactory) {
        return new LoadBalancerFeignClient(new DeadlineDefaultFeignClient(sslContextFactory, hostnameVerifier, service),
                cachingFactory, clientFactory);
    }
}
