package com.bangbangbang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 帮帮帮小程序后端启动类
 */
@SpringBootApplication
@MapperScan("com.bangbangbang.mapper")
public class BangbangbangApplication {

    public static void main(String[] args) {
        SpringApplication.run(BangbangbangApplication.class, args);
    }
}
