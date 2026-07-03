package com.navbi.nav;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("nav_item")
public class NavItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String url;
    private String category;
    private String icon;
    private Integer sort;
    private Long clickCount;
    private Long uvCount;
    private Boolean enabled;
    private Boolean proxyEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
