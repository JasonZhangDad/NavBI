package com.navbi.apilog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("api_log")
public class ApiLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String method;
    private String path;
    private String ip;
    private String username;
    private Integer status;
    private Integer costMs;
    private String userAgent;
    private LocalDateTime createdAt;
}
