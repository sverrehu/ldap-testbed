package no.shhsoft.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * For running misc. tests manually. Requires a file $HOME/.ldap-testbed.properties
 * for connection details:
 *   baseDn=dc=example,dc=com
 *   host=ldap.example.com
 *   port=636
 *   userDn=user@example.com
 *   password=MySecretPassword
 */
public final class LocalManualTest {

    private static final Logger LOG = LoggerFactory.getLogger(LocalManualTest.class);
    private static final String PROPERTIES_FILE = System.getProperty("user.home") + "/.ldap-testbed.properties";
    private static final String GROUP_MEMBER_OF_FIELD = "memberOf";
    private final String usernameToUniqueSearchFormat = "userPrincipalName=%s";
    private LdapConnectionSpec ldapConnectionSpec;

    private void doit(final LdapConnectionSpec connectionSpec, final String userDn, final char[] password) {
        this.ldapConnectionSpec = connectionSpec;
        final LdapContext context = LdapUtils.connect(connectionSpec, userDn, password);
        if (context == null) {
            throw new RuntimeException("No LdapContext");
        }
        final Set<String> groups = findGroups(context, userDn);
        System.out.println("Groups for " + userDn + ":" + (groups.isEmpty() ? " None" : ""));
        for (final String group : groups) {
            System.out.println("  Group: " + group);
        }
    }

    private Set<String> findGroups(final LdapContext ldap, final String username) {
        final Set<String> set = new HashSet<>();
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
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

    public static void main(final String[] args) {
        final Properties props = new Properties();
        try {
            props.load(new FileReader(PROPERTIES_FILE));
            final String host = Objects.requireNonNull(props.getProperty("host"));
            final int port = Integer.valueOf(Objects.requireNonNull(props.getProperty("port")));
            final boolean useTls = port == 636;
            final String baseDn = Objects.requireNonNull(props.getProperty("baseDn"));
            final LdapConnectionSpec connectionSpec = new LdapConnectionSpec(host, port, useTls, baseDn);
            final String userDn = Objects.requireNonNull(props.getProperty("userDn"));
            final String password = Objects.requireNonNull(props.getProperty("password"));
            new LocalManualTest().doit(connectionSpec, userDn, password.toCharArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + PROPERTIES_FILE);
        }
    }

}
