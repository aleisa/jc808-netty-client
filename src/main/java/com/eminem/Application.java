package com.eminem;

import com.eminem.service.TCPServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

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


    @Bean(value = "TCPServer",initMethod = "startServer")
    public TCPServer getTCPServer(){
        return new TCPServer(20048);
    }


    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }
}
