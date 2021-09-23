package no.shhsoft.ldap;

import no.shhsoft.utils.StringUtils;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * NOTE: Heavily trimmed version of Sverre's original utility.
 *
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapUtils {

    private static final Logger LOG = Logger.getLogger(LdapUtils.class.getName());
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private LdapUtils() {
    }

    public static String escape(final String s) {
        /* See RFC 2253, section 2.4 */
        final StringBuilder sb = new StringBuilder();
        final int len = s.length();
        for (int q = 0; q < len; q++) {
            final int c = s.charAt(q);
            boolean doEscape = false;
            if (q == 0 && (c == ' ' || c == '#')) {
                doEscape = true;
            } else if (q == len - 1 && c == ' ') {
                doEscape = true;
            } else if (",+\"\\<>;".indexOf(c) >= 0) {
                doEscape = true;
            } else if (c < 32 || c > 126) {
                /* The standard actually allows values outside this range, but since we are allowed
                 * to escape anything, we do it just to avoid potential problems. */
                /* Update 2007-04-24: only escape the low ones. */
                if (c < 32) {
                    doEscape = true;
                }
            }
            if (doEscape) {
                sb.append('\\');
                if (" #,+\"\\<>;".indexOf(c) >= 0) {
                    sb.append((char) c);
                } else {
                    if (c > 255) {
                        sb.append(HEX_CHARS[(c >> 12) & 0xf]);
                        sb.append(HEX_CHARS[(c >> 8) & 0xf]);
                        sb.append('\\');
                    }
                    sb.append(HEX_CHARS[(c >> 4) & 0xf]);
                    sb.append(HEX_CHARS[c & 0xf]);
                }
            } else {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }

    public static LdapContext connect(final LdapConnectionSpec ldapConnectionSpec, final String userDn, final char[] password) {
        if (StringUtils.isBlank(userDn) || password == null || password.length == 0) {
            return null;
        }
        final Hashtable<String, Object> env = new Hashtable<>();
        /* As per https://docs.oracle.com/javase/jndi/tutorial/ldap/connect/pool.html,
         * not using connection pooling, since we change the principal of the connection. */
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapConnectionSpec.getUrl());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userDn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.REFERRAL, "follow");
        try {
            return new InitialLdapContext(env, null);
        } catch (final AuthenticationException e) {
            LOG.info("Authentication failure for user \"" + userDn + "\": " + e.getMessage());
            return null;
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

}
