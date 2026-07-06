package com.navbi.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class AppUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String passwordHash;
    private String role;
    private Boolean enabled;
    private Integer dailyDownloadLimit;
    private Integer downloadsUsedToday;
    private LocalDate downloadLimitResetDate;
    private String registerIp;
    private String country;
    private String province;
    private String city;
    private LocalDateTime createdAt;
}
