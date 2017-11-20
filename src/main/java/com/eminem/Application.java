package com.eminem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by ysen6 on 2017/1/31.
 */
@EnableTransactionManagement
@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    /*
        @Override
        protected SpringApplicationBuilder configure(SpringApplicationBuilder builder){
               return builder.sources(this.getClass());
        }
    */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
