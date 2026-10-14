package com.delivery.order.controller;

import com.delivery.order.dto.OrderCreateDTO;
import com.delivery.order.vo.OrderDetailVO;
import com.delivery.order.common.Result;
import com.delivery.order.entity.Orders;
import com.delivery.order.service.OrdersService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrdersController {
    private final OrdersService ordersService;
    public OrdersController(OrdersService ordersService){
        this.ordersService=ordersService;
    }

    @PostMapping("/create")
    public Result<OrderDetailVO> create(@RequestBody OrderCreateDTO dto){
        return Result.success(ordersService.createOrder(dto));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id){
        return Result.success(ordersService.getOrderDetail(id));
    }

    @GetMapping("/list")
    public Result<List<Orders>> list(){
        return Result.success(ordersService.list());
    }
}
