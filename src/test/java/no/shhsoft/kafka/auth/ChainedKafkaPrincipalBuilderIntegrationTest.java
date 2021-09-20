package no.shhsoft.kafka.auth;

import no.shhsoft.kafka.auth.container.TestKafkaContainer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public final class ChainedKafkaPrincipalBuilderIntegrationTest {

    private static TestKafkaContainer container;

    @BeforeClass
    public static void beforeClass()
    throws IOException {
        container = new TestKafkaContainer();
        container.start();
    }

    @Test
    public void shouldFoo() {
    }

}
