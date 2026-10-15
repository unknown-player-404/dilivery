package com.delivery.order.service.Impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.delivery.order.client.DishClient;
import com.delivery.order.common.result.Result;
import com.delivery.order.dto.DishDTO;
import com.delivery.order.dto.OrderCreateDTO;
import com.delivery.order.vo.OrderDetailVO;
import com.delivery.order.entity.Orders;
import com.delivery.order.mapper.OrdersMapper;
import com.delivery.order.service.OrdersService;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;


@Service
@RequiredArgsConstructor
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
    /**
     * 构造注入
     */
    private final OrdersMapper ordersMapper;
    private final DishClient dishClient;    //注入Feign接口，调用它=调用菜品服务
    private final RocketMQTemplate rocketMQTemplate;


    /**
     * 设置订单号的方法
     */
    private String generateOrderNo() {
        int random = new Random().nextInt(9000) + 1000;
        //不加""就是数字，加了""就是字符串拼接
        return System.currentTimeMillis() + "" + random;
    }

    /**
     * 将order和dish拼接并转化为成VO的方法
     */
    private OrderDetailVO toVO(Orders order, DishDTO dish) {

        //Lombok的@Builder方法
        OrderDetailVO vo = OrderDetailVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .dishId(order.getDishId())
                .dishName(dish.getName())
                .dishPrice(dish.getPrice())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .createTime(order.getCreateTime())
                .build();

        return vo;
    }

    /**
     * 创建订单
     */
    @Override
    //一个分布式事务
    @GlobalTransactional(timeoutMills = 300000, name = "create-order-tx")
    public OrderDetailVO createOrder(OrderCreateDTO dto) {
        if (dto == null || dto.getDishId() == null) {
            throw new RuntimeException("菜品id不能为空");
        }
        //设置为1是因为业务问题，一般防止数量为0的订单存在
        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        if (quantity <= 0) {
            throw new RuntimeException("数量必须大于0");
        }

        // 【替换 mock】远程查菜品：Feign 会自动去 Nacos 找 food-service 并发 HTTP GET /dish/{id}
        //这里不能直接拿出来用，因为得到的result是一个结果Result<DishDTO>，是一个json
        Result<DishDTO> result = dishClient.getDishById(dto.getDishId());
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new RuntimeException("菜品不存在或菜品调用服务失败");
        }
        //这个才是真正的dish
        DishDTO dish = result.getData();

        Orders order = Orders.builder()
                .dishId(dto.getDishId())
                .quantity(quantity)
                .orderNo(generateOrderNo())
                .totalPrice(dish.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .status(0)
                .createTime(LocalDateTime.now())
                .build();

        ordersMapper.insert(order);

        //新增：远程和库存，事务的第二个分支
        //库存不足时服务返回code=500，这里抛异常->全局回滚
        Result<Void> deductResult = dishClient.deductStock(dto.getDishId(), quantity);
        if (deductResult == null || deductResult.getCode() != 200) {
            throw new RuntimeException(deductResult == null ? "扣减库存失败" : deductResult.getMessage());
        }

        //发送RocketMQ消息，菜品服务消费后更新销量
        String msg = "{\"dishId\":" + dto.getDishId() + ",\"quantity\":" + quantity + "}";
        rocketMQTemplate.syncSend("order-created-topic", MessageBuilder.withPayload(msg).build());

        return toVO(order, dish);
    }

    /**
     * 获取订单详情(查询订单）
     */
    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        //用id把dish传进来
        Result<DishDTO> result = dishClient.getDishById(id);
        if (result == null | result.getCode() != 200 || result.getData() == null) {
            throw new RuntimeException("查询菜品信息失败");
        }
        DishDTO dish = result.getData();
        return toVO(order, dish);
    }


}
