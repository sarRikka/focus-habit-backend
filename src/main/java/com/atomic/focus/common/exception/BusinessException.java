package com.atomic.focus.common.exception;

import com.atomic.focus.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常，由全局异常处理器统一封装为标准 R 响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
