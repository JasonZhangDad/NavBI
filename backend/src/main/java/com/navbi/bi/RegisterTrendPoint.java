package com.navbi.bi;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterTrendPoint {
    private LocalDateTime bucket;
    private long value;
}
