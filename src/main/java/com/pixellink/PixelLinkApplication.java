package com.pixellink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PixelLinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(PixelLinkApplication.class, args);
    }
}
