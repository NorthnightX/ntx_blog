package com.ntx.gateway.config;

import com.ntx.gateway.utils.JwtUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.List;


@Component
//@Order(1) // 设置过滤器执行顺序，越小优先级越高, 可以通过注解设置，也可以通过实现Ordered接口的方法实现
public class AuthorizeFilter implements GlobalFilter, Ordered {
    /**
     * 设置过滤器
     * @param exchange
     * @param chain
     * @return
     */
    private final List<String> allowedUris = Arrays.asList(
            "verificationCode", "login", "image", "getBlogPage",
            "getBlogById", "getActiveUserToday", "readNumMaxInTwoDays","queryByKeyword","getCommentByBlog"
    );
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取请求参数
        ServerHttpRequest request = exchange.getRequest();
        String uri = request.getURI().toString();
        boolean shouldAllow = allowedUris.stream().anyMatch(uri::contains);
        if (shouldAllow) {
            return chain.filter(exchange);
        }
        String token = request.getHeaders().getFirst("Authorization");;
        //2.获取请求参数的authorization
        if (token != null && JwtUtils.validateToken(token)) {
            //如果token有效，更新token
            String userFromToken = JwtUtils.getUserFromToken(token);
            String generateToken = JwtUtils.generateToken(userFromToken);
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().set("Authorization",generateToken);
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
