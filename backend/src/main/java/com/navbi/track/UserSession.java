package com.navbi.track;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_session")
public class UserSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String ip;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer pageCount;
}
