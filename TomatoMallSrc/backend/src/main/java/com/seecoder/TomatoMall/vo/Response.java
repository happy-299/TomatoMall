package com.seecoder.TomatoMall.vo;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Response<T> implements Serializable {

    private String code;

    private String msg;

    private T data;

    // 处理无msg的情况
    public static <T> Response<T> buildSuccess(T result) {
        return buildSuccess(result, null);
    }

    public static <T> Response<T> buildSuccess(T result, String msg) {
        return new Response<T>("200", msg, result);
    }

    public static <T> Response<T> buildFailure(String msg) {
        return new Response<T>("400", msg, null);
    }
}
