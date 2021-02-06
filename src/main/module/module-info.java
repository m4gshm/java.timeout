open module spring.boot.ssl.client {
    requires static lombok;

    requires org.slf4j;
    requires reactor.core;
    requires static javax.servlet.api;
    requires static java.xml;
    requires static java.xml.ws;
    requires static spring.beans;
    requires static spring.boot.autoconfigure;
    requires static spring.context;
    requires static spring.cloud.openfeign.core;
    requires static feign.core;
    requires static org.reactivestreams;

    exports timeout;

}
