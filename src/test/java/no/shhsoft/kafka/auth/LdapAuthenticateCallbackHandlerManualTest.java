package no.shhsoft.kafka.auth;

import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

/**
 * Can only be run after the jar file has been built.
 *
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapAuthenticateCallbackHandlerManualTest {

    public static DockerComposeContainer<?> environment
      = new DockerComposeContainer<>(new File("src/test/resources/kafka/compose-kafka.yaml"))
            .withExposedService("openldap", 389);

    public static void main(final String[] args) {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
