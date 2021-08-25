package no.shhsoft.ldap;

import no.shhsoft.security.UsernamePasswordAuthenticator;
import no.shhsoft.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;
import java.util.Objects;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapUsernamePasswordAuthenticator
implements UsernamePasswordAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(LdapUsernamePasswordAuthenticator.class);

    private final LdapConnectionSpec ldapConnectionSpec;
    private final String usernameToDnFormat;

    public LdapUsernamePasswordAuthenticator(final LdapConnectionSpec ldapConnectionSpec, final String usernameToDnFormat) {
        this.ldapConnectionSpec = Objects.requireNonNull(ldapConnectionSpec);
        this.usernameToDnFormat = Objects.requireNonNull(usernameToDnFormat);
    }

    @Override
    public boolean authenticate(final String username, final char[] password) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        final String userDn = String.format(usernameToDnFormat, LdapUtils.escape(username));
        return authenticateByDn(userDn, password);
    }

    public boolean authenticateByDn(final String userDn, final char[] password) {
        final LdapContext context = LdapUtils.connect(ldapConnectionSpec, userDn, password);
        if (context == null) {
            return false;
        }
        try {
            context.close();
        } catch (final NamingException e) {
            LOG.warn("Ignoring exception when closing LDAP context.", e);
        }
        return true;
    }

}
