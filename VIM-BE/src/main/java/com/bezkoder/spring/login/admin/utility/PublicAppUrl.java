package com.bezkoder.spring.login.admin.utility;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Public browser address of this application. Prefers the current request host
 * so links stay correct when the same build runs on another server.
 */
@Component
public class PublicAppUrl {

    @Value("${app.base.url:http://localhost:4200/velocity}")
    private String configuredBaseUrl;

    public String signInUrl() {
        return signInUrl(null);
    }

    public String signInUrl(String preferredBase) {
        String base = normalizeSpaBase(preferredBase);
        if (base == null) {
            base = currentRequestBase();
        }
        if (base == null) {
            base = normalizeSpaBase(configuredBaseUrl);
        }
        if (base == null) {
            base = "http://localhost:4200/velocity";
        }
        return base + "/auth/signin";
    }

    /** Scheme, host, port, and {@code /velocity} from the active HTTP request. */
    public String currentRequestBase() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes)) {
                return null;
            }
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            if (request == null) {
                return null;
            }
            String scheme = firstHeader(request, "X-Forwarded-Proto");
            if (scheme == null || scheme.isEmpty()) {
                scheme = request.getScheme();
            }
            String forwardedHost = firstHeader(request, "X-Forwarded-Host");
            String host = forwardedHost;
            if (host == null || host.isEmpty()) {
                host = request.getServerName();
                int port = request.getServerPort();
                if (!isDefaultPort(scheme, port)) {
                    host = host + ":" + port;
                }
            }
            String context = request.getContextPath() == null ? "" : request.getContextPath();
            return normalizeSpaBase(scheme + "://" + host + context);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeSpaBase(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        value = value.replaceAll("/+$", "");
        value = value.replaceAll("(?i)/auth/(login|signin)$", "");
        value = value.replaceAll("/+$", "");
        if (!value.endsWith("/velocity")) {
            value = value + "/velocity";
        }
        return value;
    }

    private String firstHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null) {
            return null;
        }
        String first = value.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }

    private boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
    }
}
