package com.navbi.track;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UaParserTest {

    private final UaParser parser = new UaParser();

    @Test
    void parsesDesktopChrome() {
        UaInfo info = parser.parse("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
        assertThat(info.device()).isEqualTo("PC");
        assertThat(info.os()).contains("Windows");
        assertThat(info.browser()).isEqualTo("Chrome");
    }

    @Test
    void parsesIphoneSafari() {
        UaInfo info = parser.parse("Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) "
                + "AppleWebKit/605.1.15 (Version/17.5 Mobile/15E148 Safari/604.1");
        assertThat(info.device()).isEqualTo("手机");
        assertThat(info.os()).contains("iOS");
        assertThat(info.browser()).isEqualTo("Safari");
    }

    @Test
    void parsesAndroidChromeAsMobile() {
        UaInfo info = parser.parse("Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36");
        assertThat(info.device()).isEqualTo("手机");
        assertThat(info.os()).isEqualTo("Android");
        assertThat(info.browser()).isEqualTo("Chrome");
    }

    @Test
    void parsesWechatBrowserBeforeChrome() {
        UaInfo info = parser.parse("Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) "
                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.50");
        assertThat(info.device()).isEqualTo("手机");
        assertThat(info.os()).isEqualTo("iOS");
        assertThat(info.browser()).isEqualTo("微信");
    }

    @Test
    void blankUaReturnsUnknown() {
        UaInfo info = parser.parse(null);
        assertThat(info.device()).isEqualTo("未知");
        assertThat(info.os()).isEqualTo("未知");
        assertThat(info.browser()).isEqualTo("未知");
    }
}
