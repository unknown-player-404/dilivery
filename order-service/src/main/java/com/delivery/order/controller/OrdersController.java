package com.delivery.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
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

    /**
     * 创建订单：挂上 @SentinelResource 后，
     * 在控制台给资源名 createOrder 配限流规则，超出的请求会进入 blockHandler。
     */
    @PostMapping("/create")
    @SentinelResource(value = "createOrder",blockHandler = "createOrderBlockHandler")
    public Result<OrderDetailVO> create(@RequestBody OrderCreateDTO dto){
        return Result.success(ordersService.createOrder(dto));
    }

    /**
     *限流兜底方法：签名和原方法一致，末尾多一个BlockException(Sentinel)
     */
    public Result<OrderDetailVO> createOrderBlockHandler(OrderCreateDTO dto, BlockException e){
        return Result.fail("当前下单人数过多，请稍后再试(Sentinel限流已生效)");
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
