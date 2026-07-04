package com.navbi.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpUtilTest {

    @Test
    void cloudflareConnectingIpHasPriority() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "2409:8a28:1234::1");
        request.addHeader("X-Forwarded-For", "172.18.0.1, 1.1.1.1");
        request.addHeader("X-Real-IP", "172.18.0.1");
        request.setRemoteAddr("172.18.0.2");

        assertThat(ClientIpUtil.resolve(request)).isEqualTo("2409:8a28:1234::1");
    }

    @Test
    void skipsPrivateForwardedAddresses() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "172.18.0.1, 203.0.113.5");
        request.setRemoteAddr("172.18.0.2");

        assertThat(ClientIpUtil.resolve(request)).isEqualTo("203.0.113.5");
    }
}
