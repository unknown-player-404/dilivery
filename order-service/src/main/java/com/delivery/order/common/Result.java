package com.delivery.order.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    /**
     * 构造
     */
    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    /**
     * 成功
     */
    public static <T> Result<T> success(){
        return new Result<>(200,"成功",null);
    }
    public static <T> Result<T> success(T data){
        return new Result<>(200,"成功",data);
    }
    /**
     * 失败
     */
    public static <T> Result<T> fail(String message){
        return new Result<>(500,message,null);
    }

}
