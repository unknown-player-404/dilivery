package com.delivery.order.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.delivery.order.DTO.OrderCreateDTO;
import com.delivery.order.VO.OrderDetailVO;
import com.delivery.order.entity.Orders;



public interface OrdersService extends IService<Orders> {
    //两个方法都是VO是因为本质都是返回一个详细信息
    //DTO只包含dishId和quantity两个参数
    OrderDetailVO createOrder(OrderCreateDTO dto);

    OrderDetailVO getOrderDetail(Long id);

}
