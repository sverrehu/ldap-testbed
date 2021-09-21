package no.shhsoft.kafka.auth;

import org.apache.kafka.common.security.auth.KafkaPrincipal;

import java.util.Set;

public final class LdapGroupsPrincipal
extends KafkaPrincipal {

    private final Set<String> groups;

    public LdapGroupsPrincipal(final String principalType, final String name) {
        this(principalType, name, false);
    }

    public LdapGroupsPrincipal(final String principalType, final String name, final boolean tokenAuthenticated) {
        super(principalType, name, tokenAuthenticated);
        groups = UserToGroupsCache.getInstance().getGroupsForUser(name);
    }

    public boolean isInGroup(final String group) {
        return groups != null && groups.contains(group);
    }

}
