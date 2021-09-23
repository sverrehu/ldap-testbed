package no.shhsoft.kafka.auth;

import no.shhsoft.ldap.LdapConnectionSpec;
import no.shhsoft.ldap.LdapUtils;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
final class LdapUsernamePasswordAuthenticator
implements UsernamePasswordAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(LdapUsernamePasswordAuthenticator.class);
    private static final String GROUP_MEMBER_OF_FIELD = "memberOf";

    private final LdapConnectionSpec ldapConnectionSpec;
    private final String usernameToDnFormat;
    private final String usernameToUniqueSearchFormat;

    LdapUsernamePasswordAuthenticator(final LdapConnectionSpec ldapConnectionSpec, final String usernameToDnFormat, final String usernameToUniqueSearchFormat) {
        this.ldapConnectionSpec = Objects.requireNonNull(ldapConnectionSpec);
        this.usernameToDnFormat = Objects.requireNonNull(usernameToDnFormat);
        this.usernameToUniqueSearchFormat = usernameToUniqueSearchFormat;
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

    private boolean authenticateByDn(final String userDn, final char[] password, final String originalUsername) {
        final LdapContext context = LdapUtils.connect(ldapConnectionSpec, userDn, password);
        if (context == null) {
            return false;
        }
        if (!StringUtils.isBlank(usernameToUniqueSearchFormat) && originalUsername != null) {
            populateGroups(context, originalUsername);
        }
        try {
            context.close();
        } catch (final NamingException e) {
            LOG.warn("Ignoring exception when closing LDAP context.", e);
        }
        return true;
    }

    private void populateGroups(final LdapContext context, final String username) {
        UserToGroupsCache.getInstance().fetchGroupsForUserIfNeeded(username, s -> {
            if (!s.equals(username)) {
                LOG.warn("Expected \"" + username + "\", but got \"" + s + "\"");
            }
            return findGroups(context, username);
        });
    }

    private Set<String> findGroups(final LdapContext ldap, final String username) {
        final Set<String> set = new HashSet<>();
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(new String[] { GROUP_MEMBER_OF_FIELD });
        final String filter = "(" + String.format(usernameToUniqueSearchFormat, LdapUtils.escape(username)) + ")";
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
            LOG.warn("Unable to fetch groups for \"" + username + "\". Will return no groups.", e);
            return Collections.emptySet();
        }
    }

}