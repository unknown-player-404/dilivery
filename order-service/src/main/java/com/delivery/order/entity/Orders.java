package com.delivery.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
@Builder
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;
    //订单号
    private String orderNo;
    private Long dishId;
    //商品数量
    private Integer quantity;
    //总价
    private BigDecimal totalPrice;
    //支付状态  0待支付  1已支付
    private Integer status;
    private LocalDateTime createTime;
}
