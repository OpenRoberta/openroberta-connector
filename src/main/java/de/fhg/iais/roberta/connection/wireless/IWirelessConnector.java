package de.fhg.iais.roberta.connection.wireless;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;

public interface IWirelessConnector<T extends IRobot> extends IConnector<T> {
    void setPassword(String password);
}
