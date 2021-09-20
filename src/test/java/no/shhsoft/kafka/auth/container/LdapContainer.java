package no.shhsoft.kafka.auth.container;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class LdapContainer
extends GenericContainer<LdapContainer> {

    static final String LDAP_DOMAIN = "example.com";
    static final String LDAP_BASE_DN = "dc=example,dc=com";
    static final String LDAP_ADMIN_DN = "cn=admin," + LDAP_BASE_DN;
    static final char[] LDAP_ADMIN_PASSWORD = "admin".toCharArray();
    static final String EXISTING_USERNAME = "sverrehu";
    static final String EXISTING_RDN = "cn=" + EXISTING_USERNAME + ",ou=People";
    static final char[] EXISTING_USER_PASSWORD = "secret".toCharArray();
    static final String USERNAME_TO_DN_FORMAT = "cn=%s,ou=People," + LDAP_BASE_DN;

    public LdapContainer() {
        this(DockerImageName.parse("osixia/openldap:1.4.0"));
    }

    public LdapContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        withClasspathResourceMapping("/ldap/openldap-bootstrap.ldif", "/container/service/slapd/assets/config/bootstrap/ldif/50-bootstrap.ldif", BindMode.READ_ONLY);
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
