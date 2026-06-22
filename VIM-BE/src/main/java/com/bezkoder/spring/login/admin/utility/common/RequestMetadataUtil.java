package com.bezkoder.spring.login.admin.utility.common;

import javax.servlet.http.HttpServletRequest;

public final class RequestMetadataUtil {

    private RequestMetadataUtil() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty() && !"unknown".equalsIgnoreCase(forwarded.trim())) {
            String[] ips = forwarded.split(",");
            if (ips.length > 0 && ips[0] != null) {
                return ips[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty() && !"unknown".equalsIgnoreCase(realIp.trim())) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "";
    }

    public static String resolveDevice(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "";
    }
}
