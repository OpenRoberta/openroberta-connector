package de.fhg.iais.roberta.testUtils;

import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;

import java.util.ArrayList;
import java.util.Collection;

public class TestListenable<T> implements IOraListenable<T> {
    private final Collection<IOraListener<T>> listeners = new ArrayList<>();

    @Override
    public void registerListener(IOraListener<T> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(IOraListener<T> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(T object) {
        for ( IOraListener<T> listener : this.listeners ) {
            listener.update(object);
        }
    }
}
