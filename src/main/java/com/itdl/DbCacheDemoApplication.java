package com.itdl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DbCacheDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbCacheDemoApplication.class, args);
    }

}
