package com.avradeep.QuestionService.security;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignTokenPropagationConfig {

    @Bean
    public RequestInterceptor userTokenPropagationInterceptor() {

        return requestTemplate -> {

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes)
                            RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            HttpServletRequest request =
                    attributes.getRequest();

            String authorization =
                    request.getHeader("Authorization");

            if (authorization != null &&
                    !authorization.isBlank()) {

                requestTemplate.header(
                        "Authorization",
                        authorization
                );
            }
        };
    }
}