package no.shhsoft.ldap;

import no.shhsoft.kafka.auth.UserToGroupsCache;
import no.shhsoft.kafka.auth.container.LdapContainer;
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
//System.out.println(spec.getUrl());
//        try {
//            Thread.sleep(Long.MAX_VALUE);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        final LdapUsernamePasswordAuthenticator authenticator = new LdapUsernamePasswordAuthenticator(spec, LdapContainer.USERNAME_TO_DN_FORMAT, LdapContainer.USERNAME_TO_UNIQUE_SEARCH_FORMAT);
        Assert.assertTrue(authenticator.authenticate(LdapContainer.PRODUCER2_USER_PASS, LdapContainer.PRODUCER2_USER_PASS.toCharArray()));
        Assert.assertEquals(1, UserToGroupsCache.getInstance().getGroupsForUser(LdapContainer.PRODUCER2_USER_PASS).size());
        System.out.println(UserToGroupsCache.getInstance().getGroupsForUser(LdapContainer.PRODUCER2_USER_PASS).iterator().next());
    }

}
