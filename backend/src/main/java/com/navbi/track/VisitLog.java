package com.navbi.track;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visit_log")
public class VisitLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ip;
    private String country;
    private String province;
    private String city;
    private String device;
    private String os;
    private String browser;
    private String url;
    private String referer;
    private String userAgent;
    private String sessionId;
    private LocalDateTime createdAt;
}
