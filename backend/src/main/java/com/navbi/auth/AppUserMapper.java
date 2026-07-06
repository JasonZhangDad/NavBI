package com.navbi.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {

    @Update("""
            UPDATE app_user
            SET downloads_used_today = CASE
                    WHEN download_limit_reset_date < CURRENT_DATE THEN 1
                    ELSE downloads_used_today + 1
                END,
                download_limit_reset_date = CURRENT_DATE
            WHERE email = #{email}
              AND enabled = TRUE
              AND (
                    (download_limit_reset_date < CURRENT_DATE AND daily_download_limit > 0)
                    OR
                    (download_limit_reset_date >= CURRENT_DATE AND downloads_used_today < daily_download_limit)
                  )
            """)
    int consumeClientDownload(@Param("email") String email);
}
