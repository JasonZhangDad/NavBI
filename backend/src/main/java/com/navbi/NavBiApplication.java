package com.navbi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NavBiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NavBiApplication.class, args);
    }
}
