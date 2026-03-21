package com.api.monitor.config;

import java.util.List;
import java.util.Locale;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves {@code index.html} for client-side routes when no matching file exists under
 * {@code classpath:/static/}. Excludes API, OAuth, webhooks, heartbeat pings, dev consoles, etc.
 */
public class SpaResourceResolver implements ResourceResolver {

    @Override
    public Resource resolveResource(
            HttpServletRequest request,
            String requestPath,
            List<? extends Resource> locations,
            ResourceResolverChain chain) {
        Resource resource = chain.resolveResource(request, requestPath, locations);
        if (resource != null) {
            return resource;
        }
        String normalized = normalizePath(requestPath);
        if (!shouldFallback(normalized)) {
            return null;
        }
        return chain.resolveResource(request, "index.html", locations);
    }

    @Override
    public String resolveUrlPath(
            String resourcePath,
            List<? extends Resource> locations,
            ResourceResolverChain chain) {
        return chain.resolveUrlPath(resourcePath, locations);
    }

    private static String normalizePath(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) {
            return "/";
        }
        String p = requestPath.startsWith("/") ? requestPath : "/" + requestPath;
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    /**
     * When false, do not serve {@code index.html} (let other handlers / 404 apply).
     */
    static boolean shouldFallback(String path) {
        String p = path.toLowerCase(Locale.ROOT);

        if (p.startsWith("/api/")) {
            return false;
        }
        if (p.startsWith("/oauth2/")) {
            return false;
        }
        // OAuth2 callback: /login/oauth2/code/{registrationId}
        if (p.startsWith("/login/oauth2")) {
            return false;
        }
        if (p.startsWith("/webhooks/")) {
            return false;
        }
        if (p.startsWith("/heartbeat/")) {
            return false;
        }
        if (p.startsWith("/h2-console")) {
            return false;
        }
        if (p.startsWith("/actuator/")) {
            return false;
        }
        if (p.startsWith("/debug-env")) {
            return false;
        }
        // Typical static folders (also resolved by chain first when files exist)
        if (p.startsWith("/css/") || p.startsWith("/js/") || p.startsWith("/fonts/") || p.startsWith("/assets/")) {
            return false;
        }

        int slash = p.lastIndexOf('/');
        String lastSegment = slash >= 0 ? p.substring(slash + 1) : p;
        if (lastSegment.contains(".") && !lastSegment.endsWith(".html")) {
            return false;
        }
        return true;
    }
}
