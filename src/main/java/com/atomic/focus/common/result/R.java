package com.atomic.focus.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.UUID;

/**
 * 统一返回信封。
 *
 * @param <T> 业务数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class R<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;

    public R() {
        this.traceId = UUID.randomUUID().toString();
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> R<T> ok(T data, String message) {
        R<T> r = ok(data);
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(ResultCode code) {
        return fail(code.getCode(), code.getMessage());
    }

    public static <T> R<T> fail(ResultCode code, String message) {
        return fail(code.getCode(), message);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> R<T> warn(ResultCode code, T data) {
        R<T> r = new R<>();
        r.code = code.getCode();
        r.message = code.getMessage();
        r.data = data;
        return r;
    }
}
