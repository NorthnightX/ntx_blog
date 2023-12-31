package com.ntx.blog;

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;


@SpringBootApplication
@EnableDiscoveryClient //添加注册中心支持
@EnableFeignClients(basePackages = ("com.ntx.common.client")) //开启feign支持，指定扫描包
@EnableScheduling
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);


    }

    /**
     * 负载均衡
     * @return
     */
//    @Bean
    public IRule randomRule(){
        return new RandomRule();
    }

    /**
     *  远程调用,注册成bean，便于后续调用，开启负载均衡
     * @return
     */
//    @Bean
//    @LoadBalanced
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
