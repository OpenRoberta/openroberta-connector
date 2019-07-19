package de.fhg.iais.roberta.util;

import java.util.Locale;
import java.util.Objects;

public class SerialDevice {
    public final String vendorId;
    public final String productId;
    public final String port; // optional port, ignored for equality
    public final String name; // just as a description, ignored for equality

    public SerialDevice(String vendorId, String productId, String port, String name) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.port = port;
        this.name = name;
    }

    @Override public boolean equals(Object obj) {
        if ( obj == this ) {
            return true;
        }
        if ( !(obj instanceof SerialDevice) ) {
            return false;
        }
        SerialDevice serialDevice = (SerialDevice) obj;
        return this.vendorId.equalsIgnoreCase(serialDevice.vendorId) && this.productId.equalsIgnoreCase(serialDevice.productId);
    }

    @Override public int hashCode() {
        return Objects.hash(this.vendorId.toLowerCase(Locale.ENGLISH), this.productId.toLowerCase(Locale.ENGLISH));
    }
}
