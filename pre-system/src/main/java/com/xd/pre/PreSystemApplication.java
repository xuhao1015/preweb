package com.xd.pre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 系统入口
 */
@SpringBootApplication
@EnableJms    //启动消息队列
@EnableAsync
@EnableScheduling
public class PreSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(PreSystemApplication.class, args);
    }

}
