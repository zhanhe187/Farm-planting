package com.farm.fpms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FpmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FpmsApplication.class, args);
    }
}
