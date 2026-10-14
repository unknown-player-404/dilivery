package com.delivery.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDetailVO {
    /**
     * VO->接收
     * 订单详情：订单信息+菜品信息
     */
    private Long id;
    //订单单号：时间戳+随机数17550000000001234
    private String orderNo;
    private Long dishId;    //DTO参数
    private String dishName;
    private BigDecimal dishPrice;
    //商品数量
    private Integer quantity;   //DTO参数
    private BigDecimal totalPrice;
    //支付状态
    private Integer status;
    private LocalDateTime createTime;
}
