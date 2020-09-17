package no.shhsoft.ldap;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapUtilsTest {

    @Test
    public void testIsBelow() {
        assertTrue(LdapUtils.isBelow("cn=a,ou=b,ou=c,o=d", "o=d"));
        assertTrue(LdapUtils.isBelow("cn=a,ou=b,ou=c,o=d", "ou=c, o=d"));
        assertTrue(LdapUtils.isBelow("cn=a,ou=b,ou=c,o=d", "ou=b, ou=c, o=d"));
        assertTrue(LdapUtils.isBelow("cn=a,ou=b,ou=c,o=d", "cn=a, ou=b, ou=c, o=d"));

        assertFalse(LdapUtils.isBelow("cn=a,ou=b,ou=c,o=d", "o=e"));
        assertFalse(LdapUtils.isBelow("cn=a,ou=b,ou=c,o=d", "ou=e, o=d"));
        assertFalse(LdapUtils.isBelow("cn=a,ou=b,ou=c,o=d", "ou=e, ou=c, o=d"));
        assertFalse(LdapUtils.isBelow("cn=a,ou=b,ou=c,o=d", "cn=e, ou=b, ou=c, o=d"));
    }

    @Test
    public void testEscape1() {
        assertEquals("foo", LdapUtils.escape("foo"));
    }

    @Test
    public void testEscape2() {
        assertEquals("\\#foo", LdapUtils.escape("#foo"));
    }

    @Test
    public void testEscape3() {
        assertEquals("\\ foo\\ ", LdapUtils.escape(" foo "));
    }

    @Test
    public void testEscape4() {
        assertEquals("foo#", LdapUtils.escape("foo#"));
    }

    @Test
    public void testEscape5() {
        assertEquals("f\\, oo", LdapUtils.escape("f, oo"));
    }

    @Test
    public void testEscape6() {
        assertEquals("foo\\+\\\"\\<\\>\\;", LdapUtils.escape("foo+\"<>;"));
    }

    @Ignore("As of 2007-04-24, no longer escaping high unprintables.")
    @Test
    public void xtestEscape7() {
        assertEquals("foo\\7f", LdapUtils.escape("foo\u007f"));
    }

    @Ignore("As of 2007-04-24, no longer escaping high unprintables.")
    @Test
    public void xtestEscape8() {
        assertEquals("foo\\c4\\8d", LdapUtils.escape("foo\uc48d"));
    }

}
