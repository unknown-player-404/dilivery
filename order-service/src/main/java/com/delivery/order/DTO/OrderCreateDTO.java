package com.delivery.order.DTO;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreateDTO {
    /**
     * DTO->发送
     * 创建订单的请求参数
     */
    private Long dishId;
    private Integer quantity;
}
