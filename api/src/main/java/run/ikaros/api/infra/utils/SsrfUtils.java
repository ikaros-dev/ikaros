package run.ikaros.api.infra.utils;

import java.net.InetAddress;
import java.net.URI;

/**
 * Utility for Server-Side Request Forgery (SSRF) prevention.
 * Validates URLs to block requests to private/internal network addresses.
 */
public final class SsrfUtils {

    private SsrfUtils() {
    }

    /**
     * Check if a URL is safe from an SSRF perspective.
     * Rejects URLs that resolve to loopback, site-local (private), or link-local addresses.
     *
     * @param url the URL to check
     * @return true if the URL targets a public remote server, false otherwise
     */
    public static boolean isSafeUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            InetAddress address = InetAddress.getByName(host);
            return !address.isLoopbackAddress()
                && !address.isSiteLocalAddress()
                && !address.isLinkLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
