package no.shhsoft.kafka.auth;

import kafka.security.authorizer.AclAuthorizer;
import org.apache.kafka.common.acl.*;
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

/**
 * Please note! There is no support for DENY by group. This Authorizer will handle ALLOW ACLs only.
 */
public final class LdapGroupAclAuthorizer
extends AclAuthorizer {

    private static final Logger LOG = LoggerFactory.getLogger(LdapGroupAclAuthorizer.class);

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
        return protocol.equals(SecurityProtocol.SASL_SSL) || protocol.equals(SecurityProtocol.SASL_PLAINTEXT);
    }

    private void overrideResultsByGroup(final AuthorizableRequestContext requestContext, final List<AuthorizationResult> results, final List<Action> actions) {
        final KafkaPrincipal principal = requestContext.principal();
        for (int q = results.size() - 1; q >= 0; q--) {
            if (results.get(q).equals(AuthorizationResult.DENIED)) {
                LOG.info("*** was DENIED, overriding");
                final AuthorizationResult alternativeResult = authorize(principal, actions.get(q));
                if (alternativeResult.equals(AuthorizationResult.ALLOWED)) {
                    results.set(q, AuthorizationResult.ALLOWED);
                    LOG.info("Overriding DENIED result due to group membership");
                }
            }
        }
    }

    private AuthorizationResult authorize(final KafkaPrincipal principal, final Action action) {
        final ResourcePattern resourcePattern = action.resourcePattern();
        final ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(resourcePattern.resourceType(), resourcePattern.name(), resourcePattern.patternType());
        final AccessControlEntryFilter accessControlEntryFilter = new AccessControlEntryFilter(null, null, AclOperation.ANY, AclPermissionType.ALLOW);
        final AclBindingFilter aclBindingFilter = new AclBindingFilter(resourcePatternFilter, accessControlEntryFilter);
        final Iterable<AclBinding> acls = acls(aclBindingFilter);
        for (final AclBinding aclBinding : acls) {
            LOG.info("*** " + aclBinding.entry().principal());
        }
        return AuthorizationResult.DENIED;
    }

}
