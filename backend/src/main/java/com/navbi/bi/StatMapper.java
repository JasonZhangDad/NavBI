package com.navbi.bi;

import com.navbi.track.VisitLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StatMapper {

    @Select("SELECT COUNT(*) FROM visit_log WHERE created_at >= #{from} AND created_at < #{to}")
    long countPvBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(DISTINCT session_id) FROM visit_log WHERE created_at >= #{from} AND created_at < #{to}")
    long countUvBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(DISTINCT ip) FROM visit_log WHERE created_at >= #{from} AND created_at < #{to}")
    long countIpBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*) FROM app_user WHERE created_at >= #{from} AND created_at < #{to}")
    long countUsersBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*) FROM visit_log")
    long totalPv();

    @Select("SELECT COUNT(DISTINCT session_id) FROM visit_log")
    long totalUv();

    @Select("SELECT COUNT(DISTINCT ip) FROM visit_log")
    long distinctIpCount();

    @Select("SELECT DATE_TRUNC('hour', created_at) AS bucket, COUNT(*) AS pv, "
            + "COUNT(DISTINCT session_id) AS uv FROM visit_log WHERE created_at >= #{from} "
            + "GROUP BY DATE_TRUNC('hour', created_at) ORDER BY bucket")
    List<TrendPoint> trendByHour(@Param("from") LocalDateTime from);

    @Select("SELECT DATE_TRUNC('day', created_at) AS bucket, COUNT(*) AS pv, "
            + "COUNT(DISTINCT session_id) AS uv FROM visit_log WHERE created_at >= #{from} "
            + "GROUP BY DATE_TRUNC('day', created_at) ORDER BY bucket")
    List<TrendPoint> trendByDay(@Param("from") LocalDateTime from);

    @Select("SELECT DATE_TRUNC('day', created_at) AS bucket, COUNT(*) AS \"value\" "
            + "FROM app_user WHERE created_at >= #{from} "
            + "GROUP BY DATE_TRUNC('day', created_at) ORDER BY bucket")
    List<RegisterTrendPoint> registerTrendByDay(@Param("from") LocalDateTime from);

    @Select("SELECT url AS name, COUNT(*) AS \"value\" FROM visit_log WHERE url IS NOT NULL "
            + "GROUP BY url ORDER BY COUNT(*) DESC LIMIT 10")
    List<NameValue> topPages();

    @Select("SELECT device AS name, COUNT(*) AS \"value\" FROM visit_log WHERE device IS NOT NULL "
            + "GROUP BY device ORDER BY COUNT(*) DESC")
    List<NameValue> deviceShare();

    @Select("SELECT browser AS name, COUNT(*) AS \"value\" FROM visit_log WHERE browser IS NOT NULL "
            + "GROUP BY browser ORDER BY COUNT(*) DESC")
    List<NameValue> browserShare();

    @Select("SELECT country AS name, COUNT(*) AS \"value\" FROM visit_log WHERE country IS NOT NULL "
            + "GROUP BY country ORDER BY COUNT(*) DESC LIMIT 20")
    List<NameValue> geoByCountry();

    @Select("SELECT province AS name, COUNT(*) AS \"value\" FROM visit_log "
            + "WHERE country = '中国' AND province IS NOT NULL "
            + "GROUP BY province ORDER BY COUNT(*) DESC LIMIT 20")
    List<NameValue> geoByProvince();

    @Select("SELECT CASE WHEN province IS NOT NULL AND province <> '' AND province <> '未知' THEN province "
            + "WHEN country IS NOT NULL AND country <> '' THEN country ELSE '未知' END AS name, "
            + "COUNT(*) AS \"value\" FROM app_user GROUP BY CASE "
            + "WHEN province IS NOT NULL AND province <> '' AND province <> '未知' THEN province "
            + "WHEN country IS NOT NULL AND country <> '' THEN country ELSE '未知' END "
            + "ORDER BY COUNT(*) DESC LIMIT 20")
    List<NameValue> registerGeo();

    @Select("SELECT * FROM visit_log ORDER BY created_at DESC, id DESC LIMIT #{size} OFFSET #{offset}")
    List<VisitLog> pageLogs(@Param("size") int size, @Param("offset") long offset);
}
