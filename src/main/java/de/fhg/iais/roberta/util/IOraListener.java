package de.fhg.iais.roberta.util;

/**
 * Simple listener interface in combination with {@link IOraListenable}.
 * Can mostly be handled by a lambda.
 * @param <T> the type of object that is listened for
 */
@FunctionalInterface
public interface IOraListener<T> {
    /**
     * Updates the listener with the fired object.
     * This is called when a {@link IOraListenable}, that has this listener registered, triggers the {@link IOraListenable#fire(Object)} method.
     * @param object the received object
     */
    void update(T object);
}
