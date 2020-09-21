package no.shhsoft.ldap;

import no.shhsoft.utils.StringUtils;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;

/**
 * NOTE: Heavily trimmed version of Sverre's original utility.
 *
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapUtils {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private LdapUtils() {
    }

    public static LdapContext connect(final String url, final String userDn, final String password, final boolean usePooling) {
        final Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        if (usePooling) {
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            env.put("com.sun.jndi.ldap.connect.pool.timeout", "600000");
        }
        env.put(Context.PROVIDER_URL, url);
        if (!StringUtils.isBlank(userDn)) {
            if (StringUtils.isBlank(password)) {
                /* We need to stop this here, since some LDAP servers treat a blank password as an
                 * anonymous login, even if a userDn is provided. Stupid shit, particularly when the
                 * connect thing is the only way to perform LDAP authentication. */
                throw new IllegalArgumentException("Empty password not allowed.");
            }
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, userDn);
            env.put(Context.SECURITY_CREDENTIALS, password);
        } else {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        }
        try {
            return new InitialLdapContext(env, null);
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
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

}
