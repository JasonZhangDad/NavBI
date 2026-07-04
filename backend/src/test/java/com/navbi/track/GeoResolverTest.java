package com.navbi.track;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoResolverTest {

    private final GeoResolver resolver = new GeoResolver();

    @Test
    void resolvesChinesePublicIp() {
        GeoInfo geo = resolver.resolve("114.114.114.114");
        assertThat(geo.country()).isEqualTo("中国");
    }

    @Test
    void privateIpIsIntranet() {
        assertThat(resolver.resolve("127.0.0.1")).isEqualTo(GeoInfo.INTRANET);
        assertThat(resolver.resolve("192.168.1.10")).isEqualTo(GeoInfo.INTRANET);
        assertThat(resolver.resolve("172.20.0.3")).isEqualTo(GeoInfo.INTRANET);
    }

    @Test
    void garbageIpIsUnknown() {
        assertThat(resolver.resolve("not-an-ip")).isEqualTo(GeoInfo.UNKNOWN);
        assertThat(resolver.resolve(null)).isEqualTo(GeoInfo.UNKNOWN);
    }

    @Test
    void ipv6FallsBackToCloudflareCountryHeader() {
        GeoInfo geo = resolver.resolve("2409:8a28:1234::1", "CN");

        assertThat(geo.country()).isEqualTo("中国");
        assertThat(geo.province()).isEqualTo("未知");
        assertThat(geo.city()).isEqualTo("未知");
    }
}
