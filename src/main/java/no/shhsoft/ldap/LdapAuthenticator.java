package no.shhsoft.ldap;

import no.shhsoft.utils.StringUtils;
import no.shhsoft.validation.Validate;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapAuthenticator {

    private static final Logger LOG = Logger.getLogger(LdapAuthenticator.class.getName());

    private final LdapConnector ldap;
    private final String userNameToDnFormat;

    public LdapAuthenticator(final LdapConnector ldap, final String userNameToDnFormat) {
        this.ldap = Validate.notNull(ldap);
        this.userNameToDnFormat = Validate.notNull(userNameToDnFormat);
    }

    public boolean authenticateByUserName(final String userName, final char[] password) {
        if (StringUtils.isBlank(userName)) {
            return false;
        }
        final String userDn = String.format(userNameToDnFormat, LdapUtils.escape(userName));
        return authenticateByDn(userDn, password);
    }

    public boolean authenticateByDn(final String userDn, final char[] password) {
        if (StringUtils.isBlank(userDn) || password == null || password.length == 0) {
            return false;
        }
        final Hashtable<String, Object> env = new Hashtable<>();
        /* As per https://docs.oracle.com/javase/jndi/tutorial/ldap/connect/pool.html,
         * not using connection pooling, since we change the principal of the connection. */
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldap.getUrl());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userDn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        LdapContext context = null;
        try {
            context = new InitialLdapContext(env, null);
            return true;
        } catch (final AuthenticationException e) {
            LOG.info("Authentication failure for user \"" + userDn + "\": " + e.getMessage());
            return false;
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (final NamingException e) {
                    LOG.log(Level.WARNING, "Got exception closing LDAP context. Ignoring.", e);
                }
            }
        }
    }

}
