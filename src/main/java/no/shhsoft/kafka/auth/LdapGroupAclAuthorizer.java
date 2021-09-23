package no.shhsoft.kafka.auth;

import kafka.security.authorizer.AclAuthorizer;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.server.authorizer.Action;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import org.apache.kafka.server.authorizer.AuthorizationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public final class LdapGroupAclAuthorizer
extends AclAuthorizer {

    private static final Logger LOG = LoggerFactory.getLogger(LdapGroupAclAuthorizer.class);
    private static final String GROUP_TYPE = "Group";
    private static final String GROUP_TYPE_AND_COLON = GROUP_TYPE + ":";

    @Override
    public List<AuthorizationResult> authorize(final AuthorizableRequestContext requestContext, final List<Action> actions) {
        LOG.info("*** authorize principal: " + requestContext.principal() + ", protocol: " + requestContext.securityProtocol().name());
        final List<AuthorizationResult> results = super.authorize(requestContext, actions);
        if (isOverridableContext(requestContext)) {
            overrideResultsByGroup(requestContext, results, actions);
        }
        return results;
    }

    private static boolean isOverridableContext(final AuthorizableRequestContext context) {
        return isPrincipalToOverride(context.principal()) && isSecurityProtocolToOverride(context.securityProtocol());
    }

    private static boolean isPrincipalToOverride(final KafkaPrincipal principal) {
        return principal.getPrincipalType().equals(KafkaPrincipal.USER_TYPE);
    }

    private static boolean isSecurityProtocolToOverride(final SecurityProtocol protocol) {
        return protocol == SecurityProtocol.SASL_SSL || protocol == SecurityProtocol.SASL_PLAINTEXT;
    }

    private void overrideResultsByGroup(final AuthorizableRequestContext requestContext, final List<AuthorizationResult> results, final List<Action> actions) {
        final KafkaPrincipal principal = requestContext.principal();
        final Set<String> groupsForUser = UserToGroupsCache.getInstance().getGroupsForUser(principal.getName());
        if (groupsForUser == null || groupsForUser.isEmpty()) {
            /* Nothing to do. We are only concerned with group matching. */
            return;
        }
        for (int q = results.size() - 1; q >= 0; q--) {
            if (results.get(q) == AuthorizationResult.DENIED) {
                LOG.info("*** was DENIED, checking if we should override because of group membership...");
                final AuthorizationResult alternativeResult = authorize(groupsForUser, actions.get(q));
                if (alternativeResult == AuthorizationResult.ALLOWED) {
                    results.set(q, AuthorizationResult.ALLOWED);
                    LOG.info("*** Overriding DENIED result due to matching group membership");
                }
            }
        }
    }

    private AuthorizationResult authorize(final Set<String> groups, final Action action) {
        final ResourcePattern resourcePattern = action.resourcePattern();
        final ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(resourcePattern.resourceType(), resourcePattern.name(), resourcePattern.patternType());
        final AccessControlEntryFilter accessControlEntryFilter = new AccessControlEntryFilter(null, null, action.operation(), AclPermissionType.ANY);
        final AclBindingFilter aclBindingFilter = new AclBindingFilter(resourcePatternFilter, accessControlEntryFilter);
        final Iterable<AclBinding> acls = acls(aclBindingFilter);
        boolean hasSeenAllow = false;
        for (final AclBinding aclBinding : acls) {
            if (isGroupMatch(groups, aclBinding)) {
                final AclPermissionType permissionType = aclBinding.entry().permissionType();
                if (permissionType == AclPermissionType.DENY) {
                    /* There is a deny on a group to which the principal is a member. This wins. */
                    return AuthorizationResult.DENIED;
                }
                if (permissionType == AclPermissionType.ALLOW) {
                    hasSeenAllow = true;
                }
                LOG.info("*** " + aclBinding.entry().principal());
            }
        }
        return hasSeenAllow ? AuthorizationResult.ALLOWED : AuthorizationResult.DENIED;
    }

    private boolean isGroupMatch(final Set<String> groups, final AclBinding aclBinding) {
        final String aclPrincipal = aclBinding.entry().principal();
        if (!aclPrincipal.startsWith(GROUP_TYPE_AND_COLON)) {
            return false;
        }
        final String groupName = aclPrincipal.substring(GROUP_TYPE_AND_COLON.length()).trim();
        LOG.info("*** Checking match for ACL group \"" + groupName + "\"");
        return groups.contains(groupName);
    }

}
