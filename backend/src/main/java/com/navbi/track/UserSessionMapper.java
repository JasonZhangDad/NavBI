package com.navbi.track;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {

    @Update("UPDATE user_session SET end_time = CURRENT_TIMESTAMP, page_count = page_count + 1 "
            + "WHERE session_id = #{sessionId}")
    int touch(@Param("sessionId") String sessionId);
}
