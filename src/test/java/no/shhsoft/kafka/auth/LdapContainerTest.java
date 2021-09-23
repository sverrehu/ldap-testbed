package no.shhsoft.kafka.auth;

import no.shhsoft.kafka.auth.LdapUsernamePasswordAuthenticator;
import no.shhsoft.kafka.auth.UserToGroupsCache;
import no.shhsoft.kafka.auth.container.LdapContainer;
import no.shhsoft.ldap.LdapConnectionSpec;
import no.shhsoft.security.UsernamePasswordAuthenticator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Just to check that we get what we want from the OpenLDAP container.
 * If this test fails, other tests will fail too.
 */
public final class LdapContainerTest {

    private static LdapContainer container;

    @BeforeClass
    public static void beforeClass() {
        container = new LdapContainer();
        container.start();
    }

    @Before
    public void before() {
        UserToGroupsCache.getInstance().clear();
    }

    @Test
    public void shouldAuthenticateAndFetchGroup() {
        final LdapConnectionSpec spec = new LdapConnectionSpec(container.getLdapHost(), container.getLdapPort(), false, container.getLdapBaseDn());
        final UsernamePasswordAuthenticator authenticator = new LdapUsernamePasswordAuthenticator(spec, LdapContainer.USERNAME_TO_DN_FORMAT, LdapContainer.USERNAME_TO_UNIQUE_SEARCH_FORMAT);
        Assert.assertTrue(authenticator.authenticate(LdapContainer.PRODUCER_WITH_GROUP_ALLOW_USER_PASS, LdapContainer.PRODUCER_WITH_GROUP_ALLOW_USER_PASS.toCharArray()));
        Assert.assertEquals(1, UserToGroupsCache.getInstance().getGroupsForUser(LdapContainer.PRODUCER_WITH_GROUP_ALLOW_USER_PASS).size());
    }

}