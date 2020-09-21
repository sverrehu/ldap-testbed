package no.shhsoft.ldap;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapConnector {

    private final String server;
    private final int port;
    private final boolean useTls;
    private final String baseDn;

    public LdapConnector(final String server, final int port, final boolean useTls, final String baseDn) {
        this.server = server;
        this.port = port;
        this.useTls = useTls;
        this.baseDn = baseDn;
    }

    public String getUrl() {
        return (useTls ? "ldaps" : "ldap") + "://" + server + ":" + port + "/" + baseDn;
    }

    @Override
    public String toString() {
        return getUrl();
    }

}
