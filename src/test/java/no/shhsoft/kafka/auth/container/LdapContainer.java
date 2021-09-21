package no.shhsoft.kafka.auth.container;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class LdapContainer
extends GenericContainer<LdapContainer> {

    static final String LDAP_DOMAIN = "example.com";
    static final String LDAP_BASE_DN = "dc=example,dc=com";
    static final char[] LDAP_ADMIN_PASSWORD = "admin".toCharArray();
    public static final String PRODUCER1_USER_PASS = "producer1";
    public static final String NON_PRODUCER_USER_PASS = "nonproducer";

    public LdapContainer() {
        this(DockerImageName.parse("osixia/openldap:1.4.0"));
    }

    public LdapContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        withClasspathResourceMapping("/ldap/openldap-bootstrap.ldif", "/container/service/slapd/assets/config/bootstrap/ldif/50-openldap-bootstrap.ldif", BindMode.READ_ONLY);
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
