package com.navbi.nav;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IconServiceTest {

    private final IconService iconService = new IconService();

    @Test
    void autoCandidatesChainOrder() {
        NavItem item = new NavItem();
        item.setId(1L);
        item.setUrl("https://chat.deepseek.com/x");
        List<String> candidates = iconService.autoCandidates(item);
        assertThat(candidates).containsExactly(
                "https://icons.duckduckgo.com/ip3/chat.deepseek.com.ico",
                "https://www.google.com/s2/favicons?domain=chat.deepseek.com&sz=64",
                "https://chat.deepseek.com/favicon.ico",
                "https://icons.duckduckgo.com/ip3/deepseek.com.ico",
                "https://www.google.com/s2/favicons?domain=deepseek.com&sz=64");
    }

    @Test
    void exactHostCandidatesComeBeforeRootDomainFallbacks() {
        NavItem item = new NavItem();
        item.setId(1L);
        item.setUrl("https://tongyi.aliyun.com");
        List<String> candidates = iconService.autoCandidates(item);
        assertThat(candidates).containsExactly(
                "https://icons.duckduckgo.com/ip3/tongyi.aliyun.com.ico",
                "https://www.google.com/s2/favicons?domain=tongyi.aliyun.com&sz=64",
                "https://tongyi.aliyun.com/favicon.ico",
                "https://icons.duckduckgo.com/ip3/aliyun.com.ico",
                "https://www.google.com/s2/favicons?domain=aliyun.com&sz=64");
    }

    @Test
    void manualIconIsNotInAutoCandidates() {
        NavItem item = new NavItem();
        item.setId(1L);
        item.setUrl("https://github.com");
        item.setIcon("https://cdn.example.com/gh.png");
        assertThat(iconService.autoCandidates(item))
                .doesNotContain("https://cdn.example.com/gh.png");
    }

    @Test
    void rootDomainHandlesTwoPartTld() {
        assertThat(IconService.rootDomain("www.jd.com")).isEqualTo("jd.com");
        assertThat(IconService.rootDomain("news.sina.com.cn")).isEqualTo("sina.com.cn");
        assertThat(IconService.rootDomain("github.com")).isEqualTo("github.com");
    }

    @Test
    void magicBytesAreRequiredEvenWithImageContentType() {
        // 回归：DDG 曾对 open.spotify.com 返回 Content-Type=image/png 的文本垃圾
        assertThat(IconService.looksLikeImage("version 1.2".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(IconService.looksLikeImage(new byte[]{1, 2, 3, 4})).isFalse();
        assertThat(IconService.looksLikeImage(
                "<html><body>404</body></html>".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(IconService.looksLikeImage(new byte[0])).isFalse();

        assertThat(IconService.looksLikeImage(new byte[]{(byte) 0x89, 'P', 'N', 'G'})).isTrue();
        assertThat(IconService.looksLikeImage(new byte[]{0, 0, 1, 0, 1})).isTrue();
        assertThat(IconService.looksLikeImage(new byte[]{(byte) 0xFF, (byte) 0xD8, 0, 0})).isTrue();
        assertThat(IconService.looksLikeImage("<svg xmlns=\"x\"/>".getBytes(StandardCharsets.UTF_8))).isTrue();
    }

    @Test
    void picksLargestValidIcon() {
        IconService.Icon small = new IconService.Icon(new byte[100], "image/png");
        IconService.Icon large = new IconService.Icon(new byte[5000], "image/x-icon");
        assertThat(IconService.pickBest(List.of(small, large))).contains(large);
        assertThat(IconService.pickBest(List.of())).isEmpty();
    }

    @Test
    void failedManualFetchIsNotCachedForever() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        IconService service = new IconService(url -> requests.incrementAndGet() == 1
                ? Optional.empty()
                : Optional.of(new IconService.Icon(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, "image/png")));
        NavItem item = new NavItem();
        item.setId(99L);
        item.setUrl("not-a-url");
        item.setIcon("https://cdn.example.com/favicon.png");

        assertThat(service.iconFor(item)).isEmpty();
        Optional<IconService.Icon> retry = service.iconFor(item);

        assertThat(retry).isPresent();
        assertThat(requests).hasValue(2);
    }
}
