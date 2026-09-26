/*% if (feature.MV_Processes) { %*/
package es.udc.lbd.gema.lps.web.rest;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * What the proxy may fetch. The proxy exists so the browser can reach two kinds of servers:
 * the QGIS services of this stack (processing and results), and public http servers it cannot
 * call itself (mixed content). Anything else on the server's own network is refused: without
 * this, anyone could make the server call its own GeoServer administration (with the default
 * credentials in a header), the database port, or a cloud provider's metadata address.
 */
final class ProxyGuard {

  /** The only names of this stack's own services the proxy talks to (compose service names). */
  static final Set<String> INTERNAL_SERVICES = Set.of("py-qgis-wps", "py-qgis-server");

  /** Headers the browser may pass on: never credentials or cookies. */
  static final Set<String> FORWARDED_HEADERS =
      Set.of("content-type", "accept", "accept-language", "prefer");

  private ProxyGuard() {}

  /** @throws ResponseStatusException 403 when the address is not one the proxy may call */
  static void check(URL url) {
    String scheme = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
    if (!scheme.equals("http") && !scheme.equals("https")) {
      throw refuse("only http and https addresses");
    }
    String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
    if (host.isEmpty() || url.getUserInfo() != null) {
      throw refuse("an address without credentials and with a host");
    }
    if (INTERNAL_SERVICES.contains(host)) {
      return;
    }
    try {
      for (InetAddress address : InetAddress.getAllByName(host)) {
        if (isInternal(address)) {
          throw refuse("addresses outside this server's own network");
        }
      }
    } catch (UnknownHostException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unknown host");
    }
  }

  /** Loopback, this host, link-local (metadata services), private, shared and multicast ranges. */
  static boolean isInternal(InetAddress a) {
    if (a.isAnyLocalAddress()
        || a.isLoopbackAddress()
        || a.isLinkLocalAddress()
        || a.isSiteLocalAddress()
        || a.isMulticastAddress()) {
      return true;
    }
    byte[] b = a.getAddress();
    if (b.length == 4) {
      int first = b[0] & 0xff;
      int second = b[1] & 0xff;
      // 0.0.0.0/8, and 100.64.0.0/10 (carrier-grade NAT, some clouds' internal ranges)
      return first == 0 || (first == 100 && second >= 64 && second <= 127);
    }
    // IPv6 unique local addresses fc00::/7
    return b.length == 16 && (b[0] & 0xfe) == 0xfc;
  }

  private static ResponseStatusException refuse(String allowed) {
    return new ResponseStatusException(
        HttpStatus.FORBIDDEN, "The proxy only calls " + allowed + " of the map's services");
  }
}

/*% } %*/
