package com.ntx.user.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;


//@Configuration
public class FanoutConfig {

    /**
     * 交换机
     * @return
     */
    @Bean
    public FanoutExchange fanoutExchange(){
        return new FanoutExchange("fanoutExchange");
    }

    /**
     * 队列
     * @return
     */
    @Bean
    public Queue fanoutQueue(){
        return new Queue("fanoutQueue");
    }

    /**
     *绑定队列到交换机
     * @return
     */
    @Bean
    public Binding fanoutBinding(Queue fanoutQueue, FanoutExchange fanoutExchange){
        return BindingBuilder.
                bind(fanoutQueue).
                to(fanoutExchange);
    }


    /**
     * 队列
     * @return
     */
    @Bean
    public Queue fanoutQueue2(){
        return new Queue("fanoutQueue2");
    }

    /**
     *绑定队列到交换机
     * @return
     */
    @Bean
    public Binding fanoutBinding2(Queue fanoutQueue2, FanoutExchange fanoutExchange){
        return BindingBuilder.
                bind(fanoutQueue2).
                to(fanoutExchange);
    }

}
