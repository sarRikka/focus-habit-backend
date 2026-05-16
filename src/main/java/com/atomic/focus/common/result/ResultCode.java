package com.atomic.focus.common.result;

/**
 * 业务错误码定义（与接口文档第 14 章保持一致）。
 */
public enum ResultCode {

    SUCCESS(0, "ok"),

    UNAUTHORIZED(1001, "未登录或 Token 无效"),
    TOKEN_EXPIRED(1002, "Token 已过期，请刷新"),
    FORBIDDEN(1003, "无权限"),
    RATE_LIMIT(1004, "请求过于频繁"),

    PARAM_INVALID(2002, "参数校验失败"),
    NOT_FOUND(2003, "资源不存在"),
    CONFLICT(2004, "资源冲突"),

    LATE_OUT_OF_WINDOW(2010, "延迟打卡超出允许时间窗"),
    DUPLICATE_OP(2011, "重复操作（幂等忽略）"),
    /** PRD §7.5：`done`/`late` 提交时长未达到 ⌈有效每日目标/2⌉ */
    CHECKIN_MIN_DURATION_NOT_MET(2012, "打卡时长未达到当日最低有效时长"),
    PHASE_DATE_INVALID(2020, "阶段日期范围非法"),
    SCENE_OVERLAP(2030, "特殊场景重叠"),
    SCENE_PAUSE_LIMIT(2031, "暂停场景超过 3 天上限"),
    REWARD_NOT_REACHED(2040, "奖励触发条件未达成"),

    INTERNAL_ERROR(5001, "服务内部错误"),
    SERVICE_UNAVAILABLE(5002, "服务暂不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
