package no.shhsoft.kafka.auth;

import no.shhsoft.ldap.LdapConnectionSpec;
import no.shhsoft.ldap.LdapUtils;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public final class SystemUserGroupsManualTest {

    private static final String PROPERTIES_FILE = System.getProperty("user.home") + "/.ldap-testbed.properties";
    private static final String GROUP_MEMBER_OF_FIELD = "memberOf";
    private final String usernameToUniqueSearchFormat = "userPrincipalName=%s";

    private void doit(final LdapConnectionSpec connectionSpec, final String userDn, final String serviceUser, final char[] servicePassword) {
        final SystemUserGroupsFetcher groupsFetcher = new SystemUserGroupsFetcher(connectionSpec, serviceUser, servicePassword, GROUP_MEMBER_OF_FIELD, usernameToUniqueSearchFormat);
        final Set<String> groups = groupsFetcher.fetchGroups(userDn);
        System.out.println("Groups for " + userDn + ":" + (groups.isEmpty() ? " None" : ""));
        for (final String group : groups) {
            System.out.println("  Group: " + group);
        }
    }

    public static void main(final String[] args) {
        final Properties props = new Properties();
        try {
            props.load(new FileReader(PROPERTIES_FILE, StandardCharsets.ISO_8859_1));
            final String host = Objects.requireNonNull(props.getProperty("host"));
            final int port = Integer.valueOf(Objects.requireNonNull(props.getProperty("port")));
            final boolean useTls = port == 636;
            final String baseDn = Objects.requireNonNull(props.getProperty("baseDn"));
            final LdapConnectionSpec connectionSpec = new LdapConnectionSpec(host, port, useTls, baseDn);
            final String userDn = Objects.requireNonNull(props.getProperty("userDn"));
            final String userPassword = Objects.requireNonNull(props.getProperty("password"));
            final String serviceUser = Objects.requireNonNull(props.getProperty("serviceUser"));
            final String servicePassword = Objects.requireNonNull(props.getProperty("servicePassword"));
            System.out.println("Password \"" + servicePassword + "\"");
//            new SystemUserGroupsManualTest().doit(connectionSpec, userDn, userDn, userPassword.toCharArray());
            new SystemUserGroupsManualTest().doit(connectionSpec, userDn, serviceUser, servicePassword.toCharArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + PROPERTIES_FILE);
        }
    }

}
