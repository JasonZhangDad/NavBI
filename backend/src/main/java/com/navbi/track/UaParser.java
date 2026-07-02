package com.navbi.track;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.stereotype.Component;

@Component
public class UaParser {

    private static final String UNKNOWN = "未知";

    public UaInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new UaInfo(UNKNOWN, UNKNOWN, UNKNOWN);
        }
        UserAgent agent = UserAgent.parseUserAgentString(userAgent);
        OperatingSystem os = agent.getOperatingSystem();
        Browser browser = agent.getBrowser();

        String device = switch (os.getDeviceType()) {
            case COMPUTER -> "PC";
            case MOBILE -> "手机";
            case TABLET -> "平板";
            default -> "其他";
        };
        String osName = os == OperatingSystem.UNKNOWN ? UNKNOWN : os.getGroup().getName();
        String browserName = browser == Browser.UNKNOWN ? UNKNOWN : browser.getGroup().getName();
        return new UaInfo(device, osName, browserName);
    }
}
