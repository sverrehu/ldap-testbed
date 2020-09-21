package no.shhsoft.ldap;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapAuthenticatorIntegrationTest {

    @ClassRule
    public static final GenericContainer<?> ldapContainer = LdapContainerUtils.createContainer();


    @Test
    public void shouldAcceptValidUserDnAndPassword() {
        final LdapAuthenticator authenticator = getAuthenticator();
        assertTrue(authenticator.authenticateByDn(LdapContainerUtils.LDAP_ADMIN_DN, LdapContainerUtils.LDAP_ADMIN_PASSWORD));
        assertTrue(authenticator.authenticateByDn(LdapContainerUtils.EXISTING_RDN + "," + LdapContainerUtils.LDAP_BASE_DN, LdapContainerUtils.EXISTING_USER_PASSWORD));
    }

    @Test
    public void shouldAcceptValidUserNameAndPassword() {
        final LdapAuthenticator authenticator = getAuthenticator();
        assertTrue(authenticator.authenticateByUserName(LdapContainerUtils.EXISTING_USER_NAME, LdapContainerUtils.EXISTING_USER_PASSWORD));
    }

    @Test
    public void shouldDenyEmptyUserDnOrPassword() {
        final LdapAuthenticator authenticator = getAuthenticator();
        assertFalse(authenticator.authenticateByDn(LdapContainerUtils.LDAP_ADMIN_DN, null));
        assertFalse(authenticator.authenticateByDn(LdapContainerUtils.LDAP_ADMIN_DN, ""));
        assertFalse(authenticator.authenticateByDn(null, LdapContainerUtils.LDAP_ADMIN_PASSWORD));
        assertFalse(authenticator.authenticateByDn("", LdapContainerUtils.LDAP_ADMIN_PASSWORD));
        assertFalse(authenticator.authenticateByDn(null, null));
        assertFalse(authenticator.authenticateByDn("", ""));
    }

    private LdapAuthenticator getAuthenticator() {
        return new LdapAuthenticator(LdapContainerUtils.getLdapConnector(ldapContainer), LdapContainerUtils.USER_NAME_TO_DN_FORMAT);
    }

}
