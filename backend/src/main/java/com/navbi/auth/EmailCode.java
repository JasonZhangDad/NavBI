package com.navbi.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("email_code")
public class EmailCode {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String code;
    private Boolean used;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
