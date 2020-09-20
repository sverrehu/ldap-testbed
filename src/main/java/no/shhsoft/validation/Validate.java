package no.shhsoft.validation;

import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class Validate {

    private Validate() {
    }

    public static <T> T notNull(final T o, final String message) {
        if (o == null) {
            throw new IllegalArgumentException(message);
        }
        return o;
    }

    public static <T> T notNull(final T o) {
        notNull(o, "Object must not be null");
        return o;
    }

    public static void isTrue(final boolean b, final String message) {
        if (!b) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isTrue(final boolean b) {
        isTrue(b, "Expression must not be false");
    }

    public static <T> T[] noNullElements(final T[] array, final String message) {
        Validate.notNull(array);
        for (int q = array.length; q >= 0; q--) {
            if (array[q] == null) {
                throw new IllegalArgumentException(message + " (index: " + q + ")");
            }
        }
        return array;
    }

    public static <T> T[] noNullElements(final T[] array) {
        return noNullElements(array, "Array must not contain null elements");
    }

    public static <T> Collection<T> noNullElements(final Collection<T> collection, final String message) {
        Validate.notNull(collection);
        int idx = 0;
        for (final Object element : collection) {
            if (element == null) {
                throw new IllegalArgumentException(message + " (index: " + idx + ")");
            }
            ++idx;
        }
        return collection;
    }

    public static <T> Collection<T> noNullElements(final Collection<T> collection) {
        return noNullElements(collection, "Collection must not contain null elements");
    }

    public static <T> T[] notEmpty(final T[] array, final String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    public static <T> T[] notEmpty(final T[] array) {
        return notEmpty(array, "Array can not be null or empty");
    }

    public static <T> Collection<T> notEmpty(final Collection<T> collection, final String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    public static <T> Collection<T> notEmpty(final Collection<T> collection) {
        return notEmpty(collection, "Collection can not be null or empty");
    }

    public static String notEmpty(final String s, final String message) {
        if (s == null || s.length() == 0) {
            throw new IllegalArgumentException(message);
        }
        return s;
    }

    public static String notEmpty(final String s) {
        return notEmpty(s, "String can not be null or empty");
    }

    public static <T, U> Map<T, U> notEmpty(final Map<T, U> map, final String message) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    public static <T, U> Map<T, U> notEmpty(final Map<T, U> map) {
        return notEmpty(map, "Map can not be null or empty");
    }

    public static <T> Collection<T> sizeZeroOrOne(final Collection<T> collection) {
        if (collection.size() > 1) {
            throw new IllegalArgumentException("Collection must be empty, or contain a single element. Size: " + collection.size());
        }
        return collection;
    }

    public static int zeroOrOne(final int n) {
        if (n < 0 || n > 1) {
            throw new IllegalArgumentException("Number must be 0 or 1, but was " + n);
        }
        return n;
    }
}
