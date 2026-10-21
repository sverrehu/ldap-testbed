package no.shhsoft.ldap;

import javax.naming.ldap.LdapContext;

public interface LdapContextProvider {

    LdapContext getContext();

    void discardContext();

}
