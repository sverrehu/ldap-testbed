package no.shhsoft.kafka.auth.container;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class LdapContainer
extends GenericContainer<LdapContainer> {

    private static final String LDAP_DOMAIN = "example.com";
    private static final String LDAP_BASE_DN = "dc=example,dc=com";
    private static final char[] LDAP_ADMIN_PASSWORD = "admin".toCharArray();
    public static final String PRODUCER_WITH_USER_ALLOW_USER_PASS = "producer1";
    public static final String PRODUCER_WITH_GROUP_ALLOW_USER_PASS = "producer2";
    public static final String PRODUCER_WITH_GROUP_DENY_USER_PASS = "producer3";
    public static final String PRODUCERS_GROUP = "cn=producers,ou=Groups," + LDAP_BASE_DN;
    public static final String DENIED_PRODUCERS_GROUP = "cn=deniedproducers,ou=Groups," + LDAP_BASE_DN;
    public static final String NON_PRODUCER_USER_PASS = "nonproducer";
    public static final String USERNAME_TO_DN_FORMAT = "cn=%s,ou=People,dc=example,dc=com";
    public static final String USERNAME_TO_UNIQUE_SEARCH_FORMAT = "uid=%s";

    public LdapContainer() {
        this(DockerImageName.parse("osixia/openldap:1.4.0"));
    }

    @Override
    public void start() {
        super.start();
        /* It appears slapd (the OpenLDAP process) is not fully ready when the container has started.
         * Give it some slack to let it spin up. */
        try {
            Thread.sleep(1000L);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private LdapContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        withClasspathResourceMapping("/ldap/openldap-bootstrap.ldif", "/container/service/slapd/assets/config/bootstrap/ldif/50-openldap-bootstrap.ldif", BindMode.READ_ONLY);
        withClasspathResourceMapping("/ldap/openldap-access.ldif", "/container/service/slapd/assets/config/bootstrap/ldif/51-openldap-access.ldif", BindMode.READ_ONLY);
        withEnv("LDAP_DOMAIN", LDAP_DOMAIN);
        withEnv("LDAP_BASE_DN", LDAP_BASE_DN);
        withEnv("LDAP_ADMIN_PASSWORD", new String(LDAP_ADMIN_PASSWORD));
        withEnv("LDAP_TLS_VERIFY_CLIENT", "never");
        withEnv("LDAP_RFC2307BIS_SCHEMA", "true");
        withExposedPorts(389);
        withCommand("--copy-service");
    }

    public String getLdapBaseDn() {
        return LDAP_BASE_DN;
    }

    public String getLdapHost() {
        return getHost();
    }

    public int getLdapPort() {
        return getMappedPort(389);
    }

}
