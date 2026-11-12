package com.tinyleaps.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/** Registers logging, gateway trust and error handling for every servlet-based Tiny Leaps service. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "jakarta.servlet.Filter")
@Import(GlobalExceptionHandler.class)
public class TinyLeapsWebAutoConfiguration {

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> tinyLeapsRequestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> reg = new FilterRegistrationBean<>(new RequestLoggingFilter());
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }

    @Bean
    @ConditionalOnProperty(name = "tinyleaps.security.gateway-key")
    public FilterRegistrationBean<GatewayTrustFilter> tinyLeapsGatewayTrustFilter(Environment env) {
        FilterRegistrationBean<GatewayTrustFilter> reg =
                new FilterRegistrationBean<>(new GatewayTrustFilter(env.getRequiredProperty("tinyleaps.security.gateway-key")));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return reg;
    }
}
