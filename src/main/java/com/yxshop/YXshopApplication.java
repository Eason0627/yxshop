package com.yxshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
//@ComponentScan(basePackages = "com.yxshop.Controller")
public class YXshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(YXshopApplication.class, args);
    }

}
