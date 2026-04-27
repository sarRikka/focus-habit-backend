package com.atomic.focus.modules.data.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class SyncPushResultVO {

    private List<Item> results;

    @JsonProperty("server_ts")
    private OffsetDateTime serverTs;

    @Data
    public static class Item {
        @JsonProperty("client_op_id")
        private String clientOpId;
        private Boolean ok;
        private Object data;
        private Map<String, Object> error;
    }
}
