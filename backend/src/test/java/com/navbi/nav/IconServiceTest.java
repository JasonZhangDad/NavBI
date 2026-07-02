package com.navbi.nav;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IconServiceTest {

    private final IconService iconService = new IconService();

    @Test
    void candidatesChainOrder() {
        NavItem item = new NavItem();
        item.setId(1L);
        item.setUrl("https://chat.deepseek.com/x");
        List<String> candidates = iconService.candidates(item);
        assertThat(candidates).containsExactly(
                "https://icons.duckduckgo.com/ip3/chat.deepseek.com.ico",
                "https://icons.duckduckgo.com/ip3/deepseek.com.ico",
                "https://chat.deepseek.com/favicon.ico");
    }

    @Test
    void manualHttpIconComesFirst() {
        NavItem item = new NavItem();
        item.setId(1L);
        item.setUrl("https://github.com");
        item.setIcon("https://cdn.example.com/gh.png");
        assertThat(iconService.candidates(item).get(0)).isEqualTo("https://cdn.example.com/gh.png");
    }

    @Test
    void rootDomainHandlesTwoPartTld() {
        assertThat(IconService.rootDomain("www.jd.com")).isEqualTo("jd.com");
        assertThat(IconService.rootDomain("news.sina.com.cn")).isEqualTo("sina.com.cn");
        assertThat(IconService.rootDomain("github.com")).isEqualTo("github.com");
    }

    @Test
    void magicBytesDetection() {
        assertThat(IconService.looksLikeImage("image/png", new byte[]{1, 2, 3, 4})).isTrue();
        assertThat(IconService.looksLikeImage("", new byte[]{(byte) 0x89, 'P', 'N', 'G'})).isTrue();
        assertThat(IconService.looksLikeImage("", new byte[]{0, 0, 1, 0, 1})).isTrue();
        assertThat(IconService.looksLikeImage("text/html",
                "<html><body>404</body></html>".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(IconService.looksLikeImage("", "<svg xmlns=\"x\"/>".getBytes(StandardCharsets.UTF_8))).isTrue();
        assertThat(IconService.looksLikeImage("", new byte[0])).isFalse();
    }
}
