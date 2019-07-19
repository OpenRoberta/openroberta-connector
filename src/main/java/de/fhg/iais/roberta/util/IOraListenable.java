package de.fhg.iais.roberta.util;

/**
 * Simple listenable interface in combination with {@link IOraListener}.
 * Listenables need to hold a collection of listeners that are updated when an event is fired.
 * @param <T> the type of object that can be listened to
 */
public interface IOraListenable<T> {
    /**
     * Registers a {@link IOraListener} with the listenable. Adds the listener to the collection.
     * @param listener the listener that should be added
     */
    void registerListener(IOraListener<T> listener);

    /**
     * Unregisters a {@link IOraListener} from the listenable. Removes the listener from the collection.
     * @param listener the listener that should be removed
     */
    void unregisterListener(IOraListener<T> listener);

    /**
     * Fires an event to the {@link IOraListener}s. All registered listeners {@link IOraListener#update(Object)} methods should be called.
     * @param object the object that should be sent to the listeners
     */
    void fire(T object);
}
