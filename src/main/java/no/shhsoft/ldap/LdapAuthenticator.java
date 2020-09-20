package no.shhsoft.ldap;

import no.shhsoft.utils.StringUtils;

import javax.naming.ldap.LdapContext;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapAuthenticator {

    private final LdapConnector ldap;

    public LdapAuthenticator(final LdapConnector ldap) {
        this.ldap = ldap;
    }

    public boolean authenticate(final String userName, final String password) {
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(password)) {
            return false;
        }
        final LdapContext context = ldap.getUnpooledContext();
        try {
            context.bi
        } finally {
            LdapUtils.close(context);
        }
    }

}
