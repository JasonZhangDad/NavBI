package com.navbi.common;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;

public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        String cloudflareIp = firstPublicIp(request.getHeader("CF-Connecting-IP"));
        if (cloudflareIp != null) {
            return cloudflareIp;
        }
        String trueClientIp = firstPublicIp(request.getHeader("True-Client-IP"));
        if (trueClientIp != null) {
            return trueClientIp;
        }
        String forwardedIp = firstPublicIp(request.getHeader("X-Forwarded-For"));
        if (forwardedIp != null) {
            return forwardedIp;
        }
        String realIp = request.getHeader("X-Real-IP");
        if (isUsable(realIp)) {
            return normalize(realIp);
        }
        return request.getRemoteAddr();
    }

    private static String firstPublicIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String firstUsable = null;
        for (String part : value.split(",")) {
            String ip = normalize(part);
            if (!isUsable(ip)) {
                continue;
            }
            if (firstUsable == null) {
                firstUsable = ip;
            }
            if (!isPrivate(ip)) {
                return ip;
            }
        }
        return firstUsable;
    }

    private static boolean isUsable(String value) {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value.trim());
    }

    private static String normalize(String value) {
        String ip = value == null ? "" : value.trim();
        if (ip.startsWith("[") && ip.contains("]")) {
            return ip.substring(1, ip.indexOf(']'));
        }
        int colon = ip.lastIndexOf(':');
        if (colon > 0 && ip.indexOf(':') == colon && ip.indexOf('.') > 0) {
            return ip.substring(0, colon);
        }
        return ip;
    }

    private static boolean isPrivate(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
