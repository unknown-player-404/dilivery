package com.delivery.order.config;


import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ① sentinelResourceAspect：让 @SentinelResource 注解生效
 *    （Spring Cloud Alibaba 不会自动注册这个切面，必须手动加 Bean）
 * ② loadDemoFlowRule：可选的"代码方式限流"开关：
 *    启动参数加 --sentinel.demo.flow-qps=1 时给下单接口限流，不用控制台也能演示；默认关闭。
 */

@Slf4j
@Configuration
public class SentinelConfig {
    /**
     * 将对象注入spring容器里面
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect(){
        return new SentinelResourceAspect();
    }

    /**
     * 加载限流规则
     */
    @Bean
    //CommandLineRunner--在项目启动后，自动执行一段初始化代码
    public CommandLineRunner loadDemoFlowRule(@Value("${sentinel.demo.flow-qps:0}") int qps){
        return args -> {
            //默认为0，不限流
            if (qps<=0){
                return;
            }
            FlowRule rule = new FlowRule("createOrder");
            rule.setGrade(RuleConstant.FLOW_GRADE_QPS);     //按每秒请求数限流
            rule.setCount(qps);                             //每秒最多几次
            //将rule加载到Sentinel配置里面
            FlowRuleManager.loadRules(List.of(rule));
            log.info(">>>一家在Sentinel限流规则: createOrder 每秒最多{}次",qps);
        };
    }
}
