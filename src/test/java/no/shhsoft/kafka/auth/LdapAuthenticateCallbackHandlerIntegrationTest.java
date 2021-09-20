package no.shhsoft.kafka.auth;

import no.shhsoft.kafka.auth.container.ContainerTestUtils;
import no.shhsoft.kafka.auth.container.LdapContainer;
import no.shhsoft.kafka.auth.container.TestKafkaContainer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

public class LdapAuthenticateCallbackHandlerIntegrationTest {

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
        container.withEnv("KAFKA_AUTHN_LDAP_USERNAME_TO_DN_FORMAT", "cn=%s,ou=People,dc=example,dc=com");
        container.withEnv("KAFKA_LISTENER_NAME_SASL__PLAINTEXT_PLAIN_SASL_SERVER_CALLBACK_HANDLER_CLASS", LdapAuthenticateCallbackHandler.class.getName());
        container.start();
        setupTestTopicsAndAcls();
    }

    private static void setupTestTopicsAndAcls() {
        final AdminClient adminClient = ContainerTestUtils.getSaslAdminClient(container, "kafka", "kafka");
        adminClient.createTopics(Collections.singleton(new NewTopic("testtopic", 1, (short) 1)));
        try {
            for (final String topicName : adminClient.listTopics().names().get()) {
                System.out.println("Topic: " + topicName);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldFoo() {
    }

}
