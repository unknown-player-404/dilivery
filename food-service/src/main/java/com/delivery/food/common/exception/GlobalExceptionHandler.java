package com.delivery.food.common.exception;

import com.delivery.food.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 全局异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handle(Exception e){
        return Result.fail(e.getMessage());
    }
}
