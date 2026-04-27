package com.atomic.focus.modules.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class SyncPushDTO {

    private List<Operation> operations;

    @Data
    public static class Operation {
        @JsonProperty("client_op_id")
        private String clientOpId;

        @JsonProperty("client_ts")
        private OffsetDateTime clientTs;

        private String type;

        private Map<String, Object> payload;
    }
}
