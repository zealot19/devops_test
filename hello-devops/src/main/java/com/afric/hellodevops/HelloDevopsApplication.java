package com.afric.hellodevops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class HelloDevopsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloDevopsApplication.class, args);
    }

    @GetMapping("/")
    public String hello() {
        return "Hello from @fric DevOps Pipeline! ✅";
    }

    @GetMapping("/health")
    public String health() {
        return "UP";
    }
}
