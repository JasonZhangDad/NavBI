package com.navbi.bi;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrendPoint {
    private LocalDateTime bucket;
    private long pv;
    private long uv;
}
