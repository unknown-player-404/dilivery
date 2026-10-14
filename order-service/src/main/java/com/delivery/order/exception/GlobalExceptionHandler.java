package com.delivery.order.exception;


import com.delivery.order.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result<Void> handle(Exception e){
        return Result.fail(e.getMessage());
    }
}
