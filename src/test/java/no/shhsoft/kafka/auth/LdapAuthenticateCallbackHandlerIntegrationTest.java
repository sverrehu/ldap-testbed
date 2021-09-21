package no.shhsoft.kafka.auth;

import no.shhsoft.kafka.auth.container.ContainerTestUtils;
import no.shhsoft.kafka.auth.container.LdapContainer;
import no.shhsoft.kafka.auth.container.TestKafkaContainer;
import no.shhsoft.ldap.LdapConnectionSpec;
import no.shhsoft.ldap.LdapUsernamePasswordAuthenticator;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

public class LdapAuthenticateCallbackHandlerIntegrationTest {

    private static final String USERNAME_TO_DN_FORMAT = "cn=%s,ou=People,dc=example,dc=com";
    private static TestKafkaContainer container;
    private static LdapContainer ldapContainer;

    @BeforeClass
    public static void beforeClass() {
        ldapContainer = new LdapContainer();
        ldapContainer.start();
        container = new TestKafkaContainer();
        container.withEnv("KAFKA_AUTHN_LDAP_BASE_DN", ldapContainer.getLdapBaseDn());
        container.withEnv("KAFKA_AUTHN_LDAP_HOST", ldapContainer.getLdapHost());
        container.withEnv("KAFKA_AUTHN_LDAP_PORT", String.valueOf(ldapContainer.getLdapPort()));
        container.withEnv("KAFKA_AUTHN_LDAP_USERNAME_TO_DN_FORMAT", USERNAME_TO_DN_FORMAT);
        container.withEnv("KAFKA_LISTENER_NAME_SASL__PLAINTEXT_PLAIN_SASL_SERVER_CALLBACK_HANDLER_CLASS", LdapAuthenticateCallbackHandler.class.getName());
        container.start();
        setupTestTopicsAndAcls();
    }

    private static void setupTestTopicsAndAcls() {
        assertLdapAuthenticationWorks();
        container.addTopic("testtopic");
        container.addProducer("testtopic", "User:foo");
        final AdminClient adminClient = container.getSuperAdminClient();
        try {
            for (final String topicName : adminClient.listTopics().names().get()) {
                System.out.println("Topic: " + topicName);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertLdapAuthenticationWorks() {
        final LdapConnectionSpec ldapConnectionSpec = new LdapConnectionSpec(ldapContainer.getLdapHost(), ldapContainer.getLdapPort(), false, ldapContainer.getLdapBaseDn());
        final LdapUsernamePasswordAuthenticator ldapUsernamePasswordAuthenticator = new LdapUsernamePasswordAuthenticator(ldapConnectionSpec, USERNAME_TO_DN_FORMAT, false);
        Assert.assertTrue(ldapUsernamePasswordAuthenticator.authenticate("kafka", "kafka".toCharArray()));
    }

    @Test
    public void shouldFoo() {
    }


}
