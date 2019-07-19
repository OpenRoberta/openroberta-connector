package de.fhg.iais.roberta.connection;

import de.fhg.iais.roberta.main.Robot;

import java.util.List;

/**
 * The general interface for robot detectors. Every robot should implement a respective detector that looks for it on all platforms.
 */
public interface IDetector {
    /**
     * Checks whether robots targeted by this detector are available.
     * @return a list of the available robots
     */
    List<Robot> detectRobots();

    /**
     * Specifies which class of robot this connector supports.
     * @return the robot class
     */
    Class<? extends Robot> getRobotClass();
}
