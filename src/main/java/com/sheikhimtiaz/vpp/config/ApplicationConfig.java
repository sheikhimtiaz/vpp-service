package com.sheikhimtiaz.vpp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@EnableAspectJAutoProxy
@Slf4j
public class ApplicationConfig {

    private final Environment env;

    public ApplicationConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> applicationReadyEventListener() {
        return event -> {
            String protocol = "http";
            String serverPort = env.getProperty("server.port", "8080");
            String contextPath = env.getProperty("server.servlet.context-path", "/");
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }

            String hostAddress;
            try {
                hostAddress = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                hostAddress = "localhost";
            }

            log.info("\n----------------------------------------------------------\n" +
                            "Application '{}' is running! Access URLs:\n" +
                            "Local: \t\t{}://localhost:{}{}\n" +
                            "External: \t{}://{}:{}{}\n" +
                            "Profile(s): \t{}\n" +
                            "----------------------------------------------------------",
                    env.getProperty("spring.application.name"),
                    protocol,
                    serverPort,
                    contextPath,
                    protocol,
                    hostAddress,
                    serverPort,
                    contextPath,
                    env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles());
        };
    }
}