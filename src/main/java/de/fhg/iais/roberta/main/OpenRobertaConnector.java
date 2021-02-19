package de.fhg.iais.roberta.main;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.RndisDetector;
import de.fhg.iais.roberta.connection.wired.SerialRobotDetector;
import de.fhg.iais.roberta.connection.wireless.MdnsDetector;
import de.fhg.iais.roberta.connection.wireless.RaspberrypiDetector;
import de.fhg.iais.roberta.ui.main.MainController;
import de.fhg.iais.roberta.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

class OpenRobertaConnector {
    private static final Logger LOG = LoggerFactory.getLogger(OpenRobertaConnector.class);
    private static final long TIMEOUT = 1000L;
    private static final long HELP_THRESHOLD = Long.parseLong(PropertyHelper.getInstance().getProperty("timeToHelp")) * 1000L;
    private final MainController controller;

    private final IDetector wiredRobotDetector = new SerialRobotDetector();
    private final IDetector rndisDetector = new RndisDetector();
    private final IDetector naoDetector = new MdnsDetector();
    private final IDetector raspberrypiDetector = new RaspberrypiDetector();
    private final RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper();
    private final Map<Class<? extends IDetector>, IDetector> detectors = new HashMap<Class<? extends IDetector>, IDetector>() {
        {
            put(SerialRobotDetector.class, new SerialRobotDetector());
            put(RndisDetector.class, new RndisDetector());
            put(MdnsDetector.class, new MdnsDetector());
            put(RaspberrypiDetector.class, new RaspberrypiDetector());
        }
    };

    OpenRobertaConnector() {
        ResourceBundle messages = ResourceBundle.getBundle(PropertyHelper.getInstance().getProperty("messagesBundle"), Locale.getDefault());
        LOG.info("Using locale {}", (messages.getLocale().getLanguage().isEmpty()) ? "default en" : messages.getLocale());

        this.controller = new MainController(messages);
        this.controller.registerListener(this.robotDetectorHelper); // register the detector helper as a listener to selection events of the controller
    }

    public static void main(String args[]) {
        new OpenRobertaConnector().run();
    }

    void run() {
        long previousTime = System.currentTimeMillis();
        long helpTimer = 0L;
        boolean showHelp = true;

        // Main loop, repeats until the program is closed
        while (!Thread.currentThread().isInterrupted()) {
            if (!this.controller.isScan()) {
                continue;
            }
            Set<IRobot> robots = new HashSet<>(5);
            IRobot selectedRobot = null;
            this.robotDetectorHelper.reset();
            while ((selectedRobot == null) && this.controller.isScan()) {
                Class<? extends IDetector> robotType = this.controller.getRobotType();
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                // Returns null if no robot is selected
                selectedRobot = this.robotDetectorHelper.getSelectedRobot();
                IDetector robotDetector = this.detectors.get(robotType);

                // Update frontend with currently detected robots
                robots.addAll(this.robotDetectorHelper.getDetectedRobots(robotDetector));
                this.controller.setRobotList(robots);

                // Connect to robot if only one was found
                if ((robots.size() == 1)) {
                    selectedRobot = (IRobot) robots.toArray()[0];
                }

                // Repeat until a robot is available or one was selected
                try {
                    Thread.sleep(TIMEOUT);
                    helpTimer += (System.currentTimeMillis() - previousTime);

                    if ((helpTimer > HELP_THRESHOLD) && showHelp) {
                        this.controller.showHelp();
                        showHelp = false;
                    }
                } catch (InterruptedException e) {
                    LOG.error("Thread was interrupted while waiting for a robot selection: {}", e.getMessage());
                }

                previousTime = System.currentTimeMillis();
            }

            if (selectedRobot != null) {
                // Create the appropriate connector depending on the robot
                IConnector<?> connector = selectedRobot.createConnector();

                this.controller.setConnector(connector);
                connector.run(); // Blocking until the connector is finished
                showHelp = false;
            }


        }
    }
}
