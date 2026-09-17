package com.github.yannicklamprecht.mc.management.console;

import com.github.yannicklamprecht.mc.management.console.config.ConsoleUiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ConsoleUiProperties.class)
public class ManagementConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagementConsoleApplication.class, args);
    }
}
