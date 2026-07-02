package com.navbi.track;

public record GeoInfo(String country, String province, String city) {

    public static final GeoInfo UNKNOWN = new GeoInfo("未知", "未知", "未知");
    public static final GeoInfo INTRANET = new GeoInfo("内网", "内网", "内网");
}
