package de.fhg.iais.roberta.connection.ev3;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.main.Robot;
import de.fhg.iais.roberta.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Ev3Detector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(Ev3Detector.class);
    private static final String brickIp = PropertyHelper.getInstance().getProperty("brickIp");

    @Override
    public List<Robot> detectRobots() {
        List<Robot> detectedRobots = new ArrayList<>(1); // only 1 ev3 supported at a time

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

    @Override
    public Class<? extends Robot> getRobotClass() {
        return Ev3.class;
    }
}
