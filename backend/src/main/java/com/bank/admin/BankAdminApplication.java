package com.bank.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 银行卡管理后台 - 启动类
 */
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class BankAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankAdminApplication.class, args);
    }
}
