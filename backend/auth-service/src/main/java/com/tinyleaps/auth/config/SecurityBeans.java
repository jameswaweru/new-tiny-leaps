package com.tinyleaps.auth.config;

import com.tinyleaps.common.security.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityBeans {

    @Bean
    public JwtTokenService jwtTokenService(AuthProperties props) {
        return new JwtTokenService(props.jwt().secret(), props.jwt().accessTtl());
    }
}
