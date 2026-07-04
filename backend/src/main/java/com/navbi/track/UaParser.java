package com.navbi.track;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UaParser {

    private static final String UNKNOWN = "未知";

    public UaInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new UaInfo(UNKNOWN, UNKNOWN, UNKNOWN);
        }
        String device = parseDevice(userAgent);
        String osName = parseOs(userAgent);
        String browserName = parseBrowser(userAgent);
        if (!UNKNOWN.equals(device) && !UNKNOWN.equals(osName) && !UNKNOWN.equals(browserName)) {
            return new UaInfo(device, osName, browserName);
        }

        UserAgent agent = UserAgent.parseUserAgentString(userAgent);
        OperatingSystem os = agent.getOperatingSystem();
        Browser browser = agent.getBrowser();

        String fallbackDevice = switch (os.getDeviceType()) {
            case COMPUTER -> "PC";
            case MOBILE -> "手机";
            case TABLET -> "平板";
            default -> "其他";
        };
        String fallbackOs = os == OperatingSystem.UNKNOWN ? UNKNOWN : os.getGroup().getName();
        String fallbackBrowser = browser == Browser.UNKNOWN ? UNKNOWN : browser.getGroup().getName();
        return new UaInfo(
                UNKNOWN.equals(device) ? fallbackDevice : device,
                UNKNOWN.equals(osName) ? fallbackOs : osName,
                UNKNOWN.equals(browserName) ? fallbackBrowser : browserName);
    }

    private static String parseDevice(String userAgent) {
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("ipad") || ua.contains("tablet") || (ua.contains("android") && !ua.contains("mobile"))) {
            return "平板";
        }
        if (ua.contains("iphone") || ua.contains("ipod") || ua.contains("android")
                || ua.contains("mobile") || ua.contains("windows phone")) {
            return "手机";
        }
        return "PC";
    }

    private static String parseOs(String userAgent) {
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            return "iOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("windows nt") || ua.contains("windows phone")) {
            return "Windows";
        }
        if (ua.contains("mac os x") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return UNKNOWN;
    }

    private static String parseBrowser(String userAgent) {
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("micromessenger")) return "微信";
        if (ua.contains("mqqbrowser")) return "QQ浏览器";
        if (ua.contains("samsungbrowser")) return "Samsung Browser";
        if (ua.contains("ucbrowser")) return "UC浏览器";
        if (ua.contains("edgios") || ua.contains("edga") || ua.contains("edg/")) return "Edge";
        if (ua.contains("firefox") || ua.contains("fxios")) return "Firefox";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        if (ua.contains("crios") || ua.contains("chrome/")) return "Chrome";
        if (ua.contains("safari/") && !ua.contains("chrome/") && !ua.contains("crios")) return "Safari";
        return UNKNOWN;
    }
}
