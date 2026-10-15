package com.delivery.food.common.rocketmq;


import com.delivery.food.service.DishService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单消息消费者：
 *   topic         监听的队列名，必须和生产端一致；
 *   consumerGroup 消费者组，同组多个实例会平分消息。
 */

@Component
@Slf4j
@RocketMQMessageListener(topic = "order-created-topic",consumerGroup = "food-service-sales-consumer")
@RequiredArgsConstructor
public class OrderCreateConsumer implements RocketMQListener<String> {
    private static final ObjectMapper mapper=new ObjectMapper();

    private final DishService dishService;
    public void onMessage(String message){
        //消息格式:{"dishId":1,"quantity":2}
        try {
            JsonNode node = mapper.readTree(message);
            Long dishId = node.get("dishId").asLong();
            int quantity = node.get("quantity").asInt();
            dishService.addSales(dishId,quantity);
            log.info(">>>收到订单消息并更新销量：dishId={},+{}份",dishId,quantity);
        }catch (Exception e){
            log.error(">>>消费订单消息失败：{}",e.getMessage());
        }
    }
}
