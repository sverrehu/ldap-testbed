package no.shhsoft.ldap;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapConnectionSpec {

    public static final int DEFAULT_TIMEOUT_MS = 5000;
    private final String server;
    private final int port;
    private final boolean useTls;
    private final String baseDn;
    private final int timeoutMs;

    public LdapConnectionSpec(final String server, final int port, final boolean useTls, final String baseDn) {
        this(server, port, useTls, baseDn, DEFAULT_TIMEOUT_MS);
    }

    public LdapConnectionSpec(final String server, final int port, final boolean useTls, final String baseDn, final int timeoutMs) {
        this.server = server;
        this.port = port;
        this.useTls = useTls;
        this.baseDn = baseDn;
        this.timeoutMs = timeoutMs;
    }

    public String getUrl() {
        return (useTls ? "ldaps" : "ldap") + "://" + server + ":" + port + "/" + baseDn;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

}
