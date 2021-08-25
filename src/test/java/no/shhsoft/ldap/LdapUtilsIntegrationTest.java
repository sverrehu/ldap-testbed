package no.shhsoft.ldap;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import javax.naming.ldap.LdapContext;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapUtilsIntegrationTest {

    @ClassRule
    public static final GenericContainer<?> LDAP_CONTAINER = LdapContainerUtils.createContainer();

    @Test
    public void shouldFoo() {
        final LdapContext context = getContext();
        final Set<String> groups = LdapUtils.findGroups(context, LdapContainerUtils.EXISTING_RDN);
        assertFalse(groups.isEmpty());
    }

    private LdapContext getContext() {
        return LdapUtils.connect(LdapContainerUtils.getLdapConnectionSpec(LDAP_CONTAINER), LdapContainerUtils.LDAP_ADMIN_DN, LdapContainerUtils.LDAP_ADMIN_PASSWORD);
    }

}
