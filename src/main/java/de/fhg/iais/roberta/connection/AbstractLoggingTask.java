package de.fhg.iais.roberta.connection;

import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

public abstract class AbstractLoggingTask implements Callable<Void>, IOraListenable<byte[]> {

    private static final long TIMEOUT = 100;

    private final Collection<IOraListener<byte[]>> listeners = new ArrayList<>();

    @Override
    public Void call() {
        while ( !Thread.currentThread().isInterrupted() ) {
            try {
                Thread.sleep(TIMEOUT);

                log();
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
            }
        }
        finish();
        return null;
    }

    protected abstract void log();

    protected abstract void finish();

    @Override
    public void registerListener(IOraListener<byte[]> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(IOraListener<byte[]> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(byte[] object) {
        for ( IOraListener<byte[]> listener : this.listeners ) {
            listener.update(object);
        }
    }
}
