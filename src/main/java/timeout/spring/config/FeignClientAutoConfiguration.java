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
import timeout.TimeLimitExecutorImpl;
import timeout.feign.DeadlineDefaultFeignClient;
import timeout.feign.FeignRequestDeadlineStrategy;
import timeout.http.HttpDeadlineHelper;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import static timeout.http.HttpHeaders.*;
import static timeout.http.HttpStatuses.GATEWAY_TIMEOUT;

@Configuration
@ConditionalOnClass({Feign.class})
@AutoConfigureBefore({FeignAutoConfiguration.class, FeignRibbonClientAutoConfiguration.class})
public class FeignClientAutoConfiguration {


    @Autowired(required = false)
    SSLSocketFactory sslContextFactory;
    @Autowired(required = false)
    HostnameVerifier hostnameVerifier;
    @Autowired
    TimeLimitExecutorImpl service;
    @Autowired
    private FeignRequestDeadlineStrategy timeLimitStrategy;

    @ConditionalOnMissingBean
    @Bean
    Client feignClient() {
        return new DeadlineDefaultFeignClient(sslContextFactory, hostnameVerifier, service, timeLimitStrategy);
    }

}
