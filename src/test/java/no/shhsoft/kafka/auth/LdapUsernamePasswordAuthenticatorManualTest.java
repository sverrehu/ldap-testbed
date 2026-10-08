package no.shhsoft.kafka.auth;

import no.shhsoft.ldap.LdapConnectionSpec;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public final class LdapUsernamePasswordAuthenticatorManualTest {

    private static final String PROPERTIES_FILE = System.getProperty("user.home") + "/.ldap-testbed.properties";

    private void doit(final LdapConnectionSpec connectionSpec, final String userDn, final char []userPassword, final String serviceUser, final char[] servicePassword) {
        final LdapUsernamePasswordAuthenticator authenticator = new LdapUsernamePasswordAuthenticator(connectionSpec, "%s", "userPrincipalName=%s", serviceUser, new String(servicePassword), 0);
        for (int q = 0; q < 3; q++) {
            final boolean isAuthenticated = authenticator.authenticate(userDn, userPassword);
            final Set<String> groups = UserToGroupsCache.getInstance().getGroupsForUser(userDn);
            System.out.println("isAuthenticated: " + isAuthenticated);
            for (final String group : groups) {
                System.out.println("  Group: " + group);
            }
        }
    }

    public static void main(final String[] args) {
        final Properties props = new Properties();
        try {
            props.load(new FileReader(PROPERTIES_FILE, StandardCharsets.ISO_8859_1));
            final String host = Objects.requireNonNull(props.getProperty("host"));
            final int port = Integer.valueOf(Objects.requireNonNull(props.getProperty("port")));
            final boolean useTls = true;//port == 636;
            final String baseDn = Objects.requireNonNull(props.getProperty("baseDn"));
            final LdapConnectionSpec connectionSpec = new LdapConnectionSpec(host, port, useTls, baseDn);
            final String userDn = Objects.requireNonNull(props.getProperty("userDn"));
            final String userPassword = Objects.requireNonNull(props.getProperty("password"));
            final String serviceUser = Objects.requireNonNull(props.getProperty("serviceUser"));
            final String servicePassword = Objects.requireNonNull(props.getProperty("servicePassword"));
//            new SystemUserGroupsManualTest().doit(connectionSpec, userDn, userDn, userPassword.toCharArray());
            new LdapUsernamePasswordAuthenticatorManualTest().doit(connectionSpec, userDn, userPassword.toCharArray(), serviceUser, servicePassword.toCharArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + PROPERTIES_FILE);
        }
    }

}
