package de.fhg.iais.roberta.connection.ev3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.util.PropertyHelper;

public class Ev3Detector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(Ev3Detector.class);
    private static final String brickIp = PropertyHelper.getInstance().getProperty("brickIp");

    public Ev3Detector() {

    }

    @Override
    public List<IRobot> detectRobots() {
        List<IRobot> detectedRobots = new ArrayList<>(1); // only 1 ev3 supported at a time

        Ev3Communicator ev3comm = new Ev3Communicator(brickIp);
        try {
            String name = ev3comm.getName();
            if ( ev3comm.isRunning() ) {
                LOG.info("EV3 is executing a program");
            } else {
                detectedRobots.add(new Ev3(name));
            }
        } catch ( IOException e ) {
            LOG.info("EV3 could not be found.");
        }
        ev3comm.shutdown();

        return detectedRobots;
    }
}
