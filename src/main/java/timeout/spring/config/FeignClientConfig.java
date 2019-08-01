package timeout.spring.config;

import feign.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import timeout.DeadlineExecutor;
import timeout.feign.GlobalTimeoutDefaultClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

@Configuration
public class FeignClientConfig {

    @Bean
    Client client(@Autowired(required = false) SSLSocketFactory sslContextFactory,
                  @Autowired(required = false) HostnameVerifier hostnameVerifier,
                  DeadlineExecutor service) {
        return new GlobalTimeoutDefaultClient(sslContextFactory, hostnameVerifier, service);
    }
}
