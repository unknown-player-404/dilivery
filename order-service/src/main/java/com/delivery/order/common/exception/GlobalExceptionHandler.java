package com.delivery.order.common.exception;


import com.delivery.order.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result<Void> handle(Exception e){
        return Result.fail(e.getMessage());
    }
}
