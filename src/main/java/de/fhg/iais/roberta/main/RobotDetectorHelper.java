package de.fhg.iais.roberta.main;

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

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.util.IOraListener;

/**
 * Helper class for robot detection.
 * Creates callables for each detector which return the respective detected robots.
 */
public class RobotDetectorHelper implements IOraListener<IRobot> {
    private static final Logger LOG = LoggerFactory.getLogger(RobotDetectorHelper.class);

    private static final int POOL_SIZE = 4;

    private final ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);
    private final Map<IDetector, Future<List<IRobot>>> futures = new HashMap<>();
    private final Map<IDetector, Boolean> ranOnce = new HashMap<>();

    private IRobot selectedRobot = null;

    /**
     * Constructor for the robot detector helper.
     * Starts the detectors and registers the corresponding futures in a map.
     *
     * @param detectors a list of detectors that should be used to find robots
     */
    public RobotDetectorHelper(List<IDetector> detectors) {
        for ( IDetector detector : detectors ) {
            this.futures.put(detector, this.executorService.submit(detector::detectRobots));
            this.ranOnce.put(detector, false);
        }
    }

    /**
     * Returns a list of currently detected robots.
     * Checks the detectors, finished detectors are restarted.
     *
     * @return a list of currently detected robots
     */
    public List<IRobot> getDetectedRobots() {
        List<IRobot> robots = new ArrayList<>();

        // Check each detector future
        for ( Map.Entry<IDetector, Future<List<IRobot>>> entry : this.futures.entrySet() ) {
            IDetector detector = entry.getKey();
            Future<List<IRobot>> future = entry.getValue();

            // If the future is done add the results to the list and start the detector again
            if ( future.isDone() ) {
                try {
                    robots.addAll(future.get());
                } catch ( InterruptedException e ) {
                    LOG.info("Future was interrupted: {}", e.getMessage());
                } catch ( ExecutionException e ) {
                    LOG.info("Exception during callable: {}", e.getMessage());
                }

                // Enqueue another search process
                this.futures.put(detector, this.executorService.submit(detector::detectRobots));

                // Register that detector ran once
                this.ranOnce.put(detector, true);
            }
        }
        return robots;
    }

    /**
     * Returns whether all detectors ran at least once.
     *
     * @return whether each detector ran at least once
     */
    public boolean allDetectorsRanOnce() {
        return !this.ranOnce.values().contains(false);
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
        for ( Map.Entry<IDetector, Boolean> entry : this.ranOnce.entrySet() ) {
            entry.setValue(false);
        }
    }
}
