package no.shhsoft.utils;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    public static boolean containsCause(final Throwable t, final Class<? extends Throwable> cause) {
        if (t == null || cause == null) {
            return false;
        }
        Throwable current = t;
        while (current != null) {
            if (cause.isAssignableFrom(current.getClass())) {
                return true;
            }
            if (current == current.getCause()) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }

}
