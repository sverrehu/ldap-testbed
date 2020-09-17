package no.shhsoft.ldap;

import javax.naming.ldap.LdapContext;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapConnector {

    private final String server;
    private final int port;
    private final boolean useTls;
    private final String username;
    private final String password;
    private final String baseDn;

    public LdapConnector(final String server, final int port, final boolean useTls, final String username, final String password, final String baseDn) {
        this.server = server;
        this.port = port;
        this.useTls = useTls;
        this.username = username;
        this.password = password;
        this.baseDn = baseDn;
    }

    public LdapConnector(final String server, final int port, final boolean useTls, final String baseDn) {
        this(server, port, useTls, null, null, baseDn);
    }

    public String getServer() {
        return server;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public String getUrl() {
        return (useTls ? "ldaps" : "ldap") + "://" + server + ":" + port + "/" + baseDn;
    }

    /**
     * Will use pooled contexts, so the caller must make sure to close the returned context after
     * use in order to return it to the pool.
     */
    public LdapContext getContext() {
        return LdapUtils.connect(getUrl(), username, password, true);
    }

    public LdapContext getUnpooledContext() {
        return LdapUtils.connect(getUrl(), username, password, false);
    }

    @Override
    public String toString() {
        return getUrl();
    }

}
