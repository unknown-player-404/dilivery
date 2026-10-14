package com.delivery.order.dto;


import lombok.Data;

import java.math.BigDecimal;


/**
 * food-service里面Dish的镜像
 */
@Data
public class DishDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private String description;
}
