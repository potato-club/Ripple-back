package org.example.rippleback;

import org.example.rippleback.core.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@ConfigurationPropertiesScan(basePackages = "org.example.rippleback")
public class RippleBackApplication {

    public static void main(String[] args) {

        SpringApplication.run(RippleBackApplication.class, args);
    }

}
