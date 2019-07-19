package de.fhg.iais.roberta.util;

import java.util.Objects;

public class Pair<F, S> {
    private final F first;
    private final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return this.first;
    }

    public S getSecond() {
        return this.second;
    }

    @Override public boolean equals(Object obj) {
        if ( !(obj instanceof Pair) ) {
            return false;
        }
        Pair<?, ?> p = (Pair<?, ?>) obj;
        return Objects.equals(p.first, this.first) && Objects.equals(p.second, this.second);
    }

    @Override public int hashCode() {
        return ((this.first == null) ? 0 : this.first.hashCode()) ^ ((this.second == null) ? 0 : this.second.hashCode());
    }

    @Override public String toString() {
        return "Pair{" + "first=" + this.first + ", second=" + this.second + '}';
    }
}

