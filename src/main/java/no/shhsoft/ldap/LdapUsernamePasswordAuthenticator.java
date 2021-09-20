package no.shhsoft.ldap;

import no.shhsoft.kafka.auth.UserToGroupsCache;
import no.shhsoft.security.UsernamePasswordAuthenticator;
import no.shhsoft.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapUsernamePasswordAuthenticator
implements UsernamePasswordAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(LdapUsernamePasswordAuthenticator.class);
    private static final String USER_DN_SEARCH_FIELD = "userPrincipalName";
    private static final String GROUP_MEMBER_OF_FIELD = "memberOf";

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
        return authenticateByDn(userDn, password, username);
    }

    public boolean authenticateByDn(final String userDn, final char[] password) {
        return authenticateByDn(userDn, password, null);
    }

    public boolean authenticateByDn(final String userDn, final char[] password, final String originalUsername) {
        final LdapContext context = LdapUtils.connect(ldapConnectionSpec, userDn, password);
        if (context == null) {
            return false;
        }
        if (originalUsername != null) {
            populateGroups(context, userDn, originalUsername);
        }
        try {
            context.close();
        } catch (final NamingException e) {
            LOG.warn("Ignoring exception when closing LDAP context.", e);
        }
        return true;
    }

    private void populateGroups(final LdapContext context, final String userDn, final String originalUsername) {
        UserToGroupsCache.getInstance().fetchGroupsForUserIfNeeded(originalUsername, s -> {
            if (!s.equals(originalUsername)) {
                LOG.warn("Expected \"" + originalUsername + "\", but got \"" + s + "\"");
            }
            return findAdGroups(context, userDn);
        });
    }

    private static Set<String> findAdGroups(final LdapContext ldap, final String dn) {
        final Set<String> set = new HashSet<>();
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final String escapedDn = LdapUtils.escape(dn);
        final String filter = "(" + USER_DN_SEARCH_FIELD + "=" + escapedDn + ")";
        try {
            final NamingEnumeration<SearchResult> ne = ldap.search("", filter, sc);
            if (ne.hasMore()) {
                final SearchResult sr = ne.next();
                final Attributes attributes = sr.getAttributes();
                if (attributes != null) {
                    final Attribute attribute = attributes.get(GROUP_MEMBER_OF_FIELD);
                    if (attribute != null) {
                        final NamingEnumeration<?> allGroups = attribute.getAll();
                        while (allGroups.hasMore()) {
                            set.add(allGroups.next().toString());
                        }
                    }
                }
            }
            if (ne.hasMore()) {
                LOG.warn("Expected to find unique entry for \"" + filter + "\", but found several. Will not return any groups.");
                set.clear();
            }
            return set;
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
    }

}
