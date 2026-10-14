package com.delivery.order.DTO;


import lombok.Data;

@Data
public class OrderCreateDTO {
    /**
     * DTO->发送
     * 创建订单的请求参数
     */
    private Long dishId;
    private Integer quantity;
}
