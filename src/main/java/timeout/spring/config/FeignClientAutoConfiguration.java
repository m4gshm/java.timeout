package timeout.spring.config;

import feign.Client;
import feign.Feign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.TimeLimitExecutor;
import timeout.feign.DeadlineDefaultFeignClient;
import timeout.feign.FeignRequestTimeLimitStrategy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

@Configuration
@ConditionalOnClass({Feign.class})
@AutoConfigureBefore({FeignAutoConfiguration.class, FeignRibbonClientAutoConfiguration.class})
public class FeignClientAutoConfiguration {


    @Autowired(required = false)
    SSLSocketFactory sslContextFactory;
    @Autowired(required = false)
    HostnameVerifier hostnameVerifier;
    @Autowired
    TimeLimitExecutor service;
    @Autowired
    private FeignRequestTimeLimitStrategy timeLimitStrategy;

    @ConditionalOnMissingBean
    @Bean
    Client feignClient() {
        return new DeadlineDefaultFeignClient(sslContextFactory, hostnameVerifier, service, timeLimitStrategy);
    }

}
