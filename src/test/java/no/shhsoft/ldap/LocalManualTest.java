package no.shhsoft.ldap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

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

    private static final String PROPERTIES_FILE = System.getProperty("user.home") + "/.ldap-testbed.properties";
    private static final String USER_DN_SEARCH_FIELD = "userPrincipalName";
    private static final String GROUP_MEMBER_OF_FIELD = "memberOf";

    private static void doit(final LdapConnectionSpec connectionSpec, final String userDn, final char[] password) {
        final LdapContext context = LdapUtils.connect(connectionSpec, userDn, password);
        if (context == null) {
            throw new RuntimeException("No LdapContext");
        }
        final Set<String> groups = findAdGroups(context, userDn);
        for (final String group : groups) {
            System.out.println("  Group: " + group);
        }
    }

    private static Set<String> findAdGroups(final LdapContext ldap, final String dn) {
        final Set<String> set = new HashSet<>();
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final String escapedDn = LdapUtils.escape(dn);
        final String filter = "(" + USER_DN_SEARCH_FIELD + "=" + escapedDn + ")";
        try {
            final NamingEnumeration<SearchResult> ne = ldap.search("", filter, sc);
            while (ne.hasMore()) {
                final SearchResult sr = ne.next();
                System.out.println("Found " + sr.getName());
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
            return set;
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
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
            doit(connectionSpec, userDn, password.toCharArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + PROPERTIES_FILE);
        }
    }

}
