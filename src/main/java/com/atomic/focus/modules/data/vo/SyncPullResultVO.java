package com.atomic.focus.modules.data.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class SyncPullResultVO {

    @JsonProperty("server_ts")
    private OffsetDateTime serverTs;

    private Bucket goals;
    private Bucket checkins;
    private Bucket rewards;
    private Bucket reviews;
    private Bucket scenes;

    private Object profile;
    private Object settings;

    @Data
    public static class Bucket {
        private List<?> upserts;
        private List<String> deletes;
    }
}
