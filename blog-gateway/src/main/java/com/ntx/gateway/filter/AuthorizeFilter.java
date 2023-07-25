package com.ntx.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
//@Component
//@Order(1) // 设置过滤器执行顺序，越小优先级越高, 可以通过注解设置，也可以通过实现Ordered接口的方法实现
public class AuthorizeFilter implements GlobalFilter, Ordered {
    /**
     * 设置过滤器
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取请求参数
        ServerHttpRequest request = exchange.getRequest();
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        //2.获取请求参数的authorization
        String authorization = queryParams.getFirst("authorization");
        //3.判断参数是否为admin
        if (authorization != null && authorization.equals("admin")) {
            //4.是
            return chain.filter(exchange);
        }
        //5.否
        //5.1设置状态码，HttpStatus是一个枚举类，里面定义了很多状态码，本次使用的unauthorized是未登录，状态码为401
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        //setComplete为结束请求
        return exchange.getResponse().setComplete();
    }

    /**
     * 设置过滤器的执行顺序
     * @return
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
