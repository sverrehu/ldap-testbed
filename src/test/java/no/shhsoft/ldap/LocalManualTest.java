package no.shhsoft.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
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
    private final String usernameToUniqueSearchFormat = "sAMAccountName=%s";

    private void doit(final LdapConnectionSpec connectionSpec, final String userDn, final char[] password) {
        LOG.info("Logging in as " + userDn);
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
            final Map<String, Map<String, List<String>>> attributes1 = getAttributes(ne);
            for (final Map.Entry<String, Map<String, List<String>>> entry : attributes1.entrySet()) {
                System.out.println(entry.getKey());
                for (final Map.Entry<String, List<String>> entry2 : entry.getValue().entrySet()) {
                    System.out.println("  " + entry2.getKey() + ": " + entry2.getValue().get(0));
                    if (entry2.getKey().equalsIgnoreCase(GROUP_MEMBER_OF_FIELD)) {
                        set.addAll(entry2.getValue());
                    }
                }
            }
            return set;
        } catch (final NamingException e) {
            LOG.warn("Unable to fetch groups for \"" + username + "\". Will return no groups.", e);
            return Collections.emptySet();
        }
    }

    private static Map<String, Map<String, List<String>>> getAttributes(final NamingEnumeration<SearchResult> ne) {
        final Map<String, Map<String, List<String>>> results = new LinkedHashMap<>();
        try {
            while (ne.hasMore()) {
                final SearchResult sr = ne.next();
                results.put(sr.getNameInNamespace(), getAttributes(sr.getAttributes()));
            }
        } catch (final PartialResultException e) {
            LOG.info("Ignoring PartialResultException: " + e.getMessage());
        } catch (final NamingException e) {
            throw new UncheckedNamingException(e);
        }
        return results;
    }

    private static Map<String, List<String>> getAttributes(final Attributes attributes) {
        if (attributes == null) {
            return Collections.emptyMap();
        }
        try {
            final Map<String, List<String>> attributeMap = new LinkedHashMap<>();
            final NamingEnumeration<? extends Attribute> ne = attributes.getAll();
            while (ne.hasMore()) {
                final ArrayList<String> valuesList = new ArrayList<>();
                final Attribute attribute = ne.next();
                final NamingEnumeration<?> values = attribute.getAll();
                while (values.hasMore()) {
                    valuesList.add(values.next().toString());
                }
                valuesList.sort(String::compareToIgnoreCase);
                attributeMap.put(attribute.getID(), valuesList);
            }
            return attributeMap;
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
            final boolean useTls = true;//port == 636;
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
