package no.shhsoft.ldap;

import no.shhsoft.utils.ExceptionUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapUtilsIntegrationTest {

    private static final boolean USE_LDAPS = false;
    private static final String LDAP_DOMAIN = "shhsoft.no";
    private static final String LDAP_BASE_DN = "dc=shhsoft,dc=no";
    private static final String LDAP_ADMIN_DN = "cn=admin," + LDAP_BASE_DN;
    private static final String LDAP_ADMIN_PASSWORD = "admin";
    private static final String EXISTING_RDN = "cn=sverre,ou=People";
    private static final String NON_EXISTING_SUBTREE = "ou=non-existing";
    private static final String NON_EXISTING_ID = "non-existing";
    private static final String NON_EXISTING_RDN = "cn=" + NON_EXISTING_ID + "," + NON_EXISTING_SUBTREE;

    /* osixia will autogenerate
     * dn: {{ LDAP_BASE_DN }}
     * dn: cn=admin,{{ LDAP_BASE_DN }} */
    @ClassRule
    public static GenericContainer<?> ldapContainer = new GenericContainer<>(DockerImageName.parse("osixia/openldap:1.4.0"))
        .withClasspathResourceMapping("/ldap/bootstrap.ldif", "/container/service/slapd/assets/config/bootstrap/ldif/50-bootstrap.ldif", BindMode.READ_ONLY)
        .withEnv("LDAP_DOMAIN", LDAP_DOMAIN)
        .withEnv("LDAP_BASE_DN", LDAP_BASE_DN)
        .withEnv("LDAP_ADMIN_PASSWORD", LDAP_ADMIN_PASSWORD)
        .withEnv("LDAP_TLS_VERIFY_CLIENT", "never")
        .withExposedPorts(389, 636)
        .withCommand("--copy-service");

    private LdapContext getLdap() {
        final LdapConnector ldapConnector = new LdapConnector(ldapContainer.getHost(), ldapContainer.getMappedPort(USE_LDAPS ? 636 : 389), USE_LDAPS, LDAP_ADMIN_DN, LDAP_ADMIN_PASSWORD, LDAP_BASE_DN);
        return ldapConnector.getUnpooledContext();
    }

    private void removeNonStuff(final LdapContext ldap) {
        try {
            LdapUtils.remove(ldap, NON_EXISTING_RDN);
        } catch (final UncheckedNamingException e) {
            assertTrue(ExceptionUtils.containsCause(e, NameNotFoundException.class));
        }
        try {
            LdapUtils.remove(ldap, NON_EXISTING_SUBTREE);
        } catch (final UncheckedNamingException e) {
            assertTrue(ExceptionUtils.containsCause(e, NameNotFoundException.class));
        }
        assertNull(LdapUtils.findByRdn(ldap, NON_EXISTING_RDN));
    }

    private String getSingleValue(final Attributes attributes, final String name) {
        try {
            final Attribute attribute = attributes.get(name);
            if (attribute != null) {
                return attribute.get().toString();
            }
        } catch (final NamingException e) {
            fail("Error fetching attribute value: " + e.getMessage());
        }
        return null;
    }

    @Test
    public void testStore() {
        final LdapContext ldap = getLdap();
        removeNonStuff(ldap);
        try {
            Attributes attributes = LdapUtils.findByRdn(ldap, EXISTING_RDN);
            attributes.remove("cn");
            attributes.put("cn", NON_EXISTING_ID);
            attributes.remove("sn");
            attributes.put("sn", "sn");
            attributes.remove("description");
            attributes.put("description", "description");
            LdapUtils.store(ldap, NON_EXISTING_RDN, attributes, true);

            attributes = LdapUtils.findByRdn(ldap, NON_EXISTING_RDN);
            assertEquals("sn", getSingleValue(attributes, "sn"));
            assertEquals("description", getSingleValue(attributes, "description"));
            attributes.remove("description");
            attributes.put("description", "description2");
            LdapUtils.store(ldap, NON_EXISTING_RDN, attributes, true);

            attributes = LdapUtils.findByRdn(ldap, NON_EXISTING_RDN);
            assertEquals("sn", getSingleValue(attributes, "sn"));
            assertEquals("description2", getSingleValue(attributes, "description"));
            attributes.remove("description");
            attributes.put("description", "description2");
            LdapUtils.store(ldap, NON_EXISTING_RDN, attributes, true);
        } finally {
            removeNonStuff(ldap);
        }
    }

}
