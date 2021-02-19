package de.fhg.iais.roberta.main;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.util.IOraListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Helper class for robot detection.
 * Creates callables for each detector which return the respective detected robots.
 */
public class RobotDetectorHelper implements IOraListener<IRobot> {
    private static final Logger LOG = LoggerFactory.getLogger(RobotDetectorHelper.class);

    private static final int POOL_SIZE = 4;

    private final ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);
    private final Map<Class, IDetector> detectors = new HashMap<>(5);
    private Future<List<IRobot>> future = null;

    private IRobot selectedRobot = null;

    /**
     * Constructor for the robot detector helper.
     * Starts the detectors and registers the corresponding futures in a map.
     */
    public RobotDetectorHelper() {

    }

    /**
     * Returns a list of currently detected robots.
     * Checks the detectors, finished detectors are restarted.
     *
     * @return a list of currently detected robots
     */
    public List<IRobot> getDetectedRobots(IDetector detector) {
        List<IRobot> robots = new ArrayList<>(5);

        if (this.future == null) {
            this.future = this.executorService.submit(detector::detectRobots);
        }
        if (this.future.isDone()) {
            try {
                List<IRobot> rob = this.future.get();
                if (rob != null) {
                    robots.addAll(rob);
                }
            } catch (InterruptedException e) {
                LOG.info("Future was interrupted: {}", e.getMessage());
            } catch (ExecutionException e) {
                LOG.info("Exception during callable: {}", e.getMessage());
            }
            this.future = null;
        }

        return robots;
    }


    /**
     * Returns the currently selected robot.
     *
     * @return the currently selected robot, null if none is selected
     */
    public IRobot getSelectedRobot() {
        return this.selectedRobot;
    }

    @Override
    public void update(IRobot object) {
        this.selectedRobot = object;
    }

    /**
     * Resets the selected robot and whether each detector ran at least once.
     */
    public void reset() {
        this.selectedRobot = null;
        this.future = null;
    }
}
