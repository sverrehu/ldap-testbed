package no.shhsoft.kafka.auth.container;

import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class TtyConsumer
extends BaseConsumer<TtyConsumer> {

    private final String prefix;

    public TtyConsumer() {
        this("");
    }

    public TtyConsumer(final String prefix) {
        this.prefix = prefix == null || prefix.isEmpty() ? "" : prefix + ": ";
    }

    @Override
    public void accept(final OutputFrame outputFrame) {
        final String s = outputFrame.getUtf8String().replaceAll("((\\r?\\n)|(\\r))$", "");
        switch (outputFrame.getType()) {
            case END:
                break;
            case STDOUT:
                System.out.println(prefix + s);
                break;
            case STDERR:
                System.err.println(prefix + s);
                break;
            default:
                throw new IllegalArgumentException("Unexpected outputType " + outputFrame.getType());
        }
    }

}
