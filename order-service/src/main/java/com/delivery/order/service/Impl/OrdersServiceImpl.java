package com.delivery.order.service.Impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.delivery.order.DTO.OrderCreateDTO;
import com.delivery.order.VO.OrderDetailVO;
import com.delivery.order.entity.Orders;
import com.delivery.order.mapper.OrdersMapper;
import com.delivery.order.service.OrdersService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;


@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
    /**
     * 构造注入
     */
    private final OrdersMapper ordersMapper;

    public OrdersServiceImpl(OrdersMapper ordersMapper) {
        this.ordersMapper = ordersMapper;
    }

    /**
     * 设置订单号的方法
     */
    private String generateOrderNo() {
        int random = new Random().nextInt(9000) + 1000;
        //不加""就是数字，加了""就是字符串拼接
        return System.currentTimeMillis() + "" + random;
    }

    /**
     * 将order拼接并转化为成VO的方法
     */
    private OrderDetailVO toVO(Orders order) {
        OrderDetailVO vo = new OrderDetailVO();

        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setDishId(order.getDishId());
        //接入OpenFeign后重做
        vo.setDishName("(mock:还没接入菜品服务）");
        vo.setQuantity(order.getQuantity());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());

        return vo;
    }

    /**
     * 创建订单
     */
    @Override
    public OrderDetailVO createOrder(OrderCreateDTO dto) {
        if (dto == null || dto.getDishId() == null) {
            throw new RuntimeException("菜品id不能为空");
        }
        //设置为1是因为业务问题，一般防止数量为0的订单存在
        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        if (quantity <= 0) {
            throw new RuntimeException("数量必须大于0");
        }
        Orders order = new Orders();
        //下面两个参数都是dto本身包含的
        order.setDishId(dto.getDishId());
        order.setQuantity(quantity);

        //下列参数都是现场设置

        //设置订单号（随机的）--自定义方法
        order.setOrderNo(generateOrderNo());
        //临时价格：等OpenFeign接入后换成真实价格
        order.setTotalPrice(BigDecimal.ONE.multiply(BigDecimal.valueOf(quantity)));
        //未支付状态
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());

        ordersMapper.insert(order);

        return toVO(order);
    }

    /**
     *获取订单详情(查询订单）
     */
    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order==null){
            throw new RuntimeException("订单不存在");
        }
        return toVO(order);
    }


}
