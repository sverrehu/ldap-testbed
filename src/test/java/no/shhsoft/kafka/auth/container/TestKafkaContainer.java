package no.shhsoft.kafka.auth.container;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.images.builder.Transferable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestKafkaContainer
extends SaslPlaintextKafkaContainer {

    private static final String COMBO_JAR_ARTIFACT_NAME = "ldap-testbed";
    private final String pathToComboJar;

    public TestKafkaContainer() {
        pathToComboJar = findPathToComboJar(new File("../"));
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo, final boolean reused) {
        copyComboJar();
        super.containerIsStarting(containerInfo, reused);
    }

    private void copyComboJar() {
        try {
            final byte[] comboJarBytes = Files.readAllBytes(Path.of(pathToComboJar));
            System.out.println("*** Copying our jar to the container");
            copyFileToContainer(Transferable.of(comboJarBytes), "/usr/share/java/kafka/our-test.jar");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String findPathToComboJar(final File dir) {
        final File[] files = dir.listFiles();
        if (files == null) {
            throw new RuntimeException("Nothing found? This should not happen.");
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                final String path = findPathToComboJar(file);
                if (path != null) {
                    return path;
                }
            } else {
                if (file.getName().matches(COMBO_JAR_ARTIFACT_NAME + "-.*\\.jar")) {
                    return file.getPath();
                }
            }
        }
        return null;
    }

}
