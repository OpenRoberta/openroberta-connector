package de.fhg.iais.roberta.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.arduino.Arduino;
import de.fhg.iais.roberta.connection.arduino.ArduinoConnector;
import de.fhg.iais.roberta.connection.arduino.ArduinoDetector;
import de.fhg.iais.roberta.connection.ev3.Ev3;
import de.fhg.iais.roberta.connection.ev3.Ev3Connector;
import de.fhg.iais.roberta.connection.ev3.Ev3Detector;
import de.fhg.iais.roberta.connection.nao.Nao;
import de.fhg.iais.roberta.connection.nao.NaoConnector;
import de.fhg.iais.roberta.connection.nao.NaoDetector;
import de.fhg.iais.roberta.ui.main.MainController;
import de.fhg.iais.roberta.util.PropertyHelper;

class OpenRobertaConnector {
    private static final Logger LOG = LoggerFactory.getLogger(OpenRobertaConnector.class);

    private static final long TIMEOUT = 1000L;
    private static final long HELP_THRESHOLD = 20000L;

    private final MainController controller;

    private final Ev3Detector ev3Detector = new Ev3Detector();
    private final ArduinoDetector arduinoDetector = new ArduinoDetector();
    private final NaoDetector naoDetector = new NaoDetector();
    private final RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(this.arduinoDetector, this.ev3Detector, this.naoDetector));

    OpenRobertaConnector() {
        ResourceBundle messages = ResourceBundle.getBundle(PropertyHelper.getInstance().getProperty("messagesBundle"), Locale.getDefault());
        LOG.info("Using locale {}", (messages.getLocale().getLanguage().isEmpty()) ? "default en" : messages.getLocale());

        this.controller = new MainController(messages);
        this.controller.registerListener(this.robotDetectorHelper); // register the detector helper as a listener to selection events of the controller
    }

    void run() {
        long previousTime = System.currentTimeMillis();
        long helpTimer = 0L;
        boolean showHelp = true;

        Map<Integer, String> errors = this.arduinoDetector.getReadIdFileErrors();
        if ( !errors.isEmpty() ) {
            this.controller.showConfigErrorPopup(errors);
        }

        // Main loop, repeats until the program is closed
        while ( !Thread.currentThread().isInterrupted() ) {
            Set<Robot> robots = new HashSet<>();
            Robot selectedRobot = null;
            this.robotDetectorHelper.reset();

            while ( (selectedRobot == null) ) {
                if ( Thread.currentThread().isInterrupted() ) {
                    break;
                }
                // Returns null if no robot is selected
                selectedRobot = this.robotDetectorHelper.getSelectedRobot();

                // Update frontend with currently detected robots
                robots.addAll(this.robotDetectorHelper.getDetectedRobots());
                this.controller.setRobotList(robots);

                // Connect to robot if only one was found
                if ( this.robotDetectorHelper.allDetectorsRanOnce() && (robots.size() == 1) ) {
                    selectedRobot = (Robot) robots.toArray()[0];
                }

                // Repeat until a robot is available or one was selected
                try {
                    Thread.sleep(TIMEOUT);
                    helpTimer += (System.currentTimeMillis() - previousTime);

                    if ( (helpTimer > HELP_THRESHOLD) && showHelp ) {
                        this.controller.showHelp();
                        showHelp = false;
                    }
                } catch ( InterruptedException e ) {
                    LOG.error("Thread was interrupted while waiting for a robot selection: {}", e.getMessage());
                }

                previousTime = System.currentTimeMillis();
            }

            // Create the appropriate connector depending on the robot
            IConnector connector;

            if ( selectedRobot instanceof Ev3 ) {
                connector = new Ev3Connector();
            } else if ( selectedRobot instanceof Arduino ) {
                connector = new ArduinoConnector((Arduino) selectedRobot);
            } else if ( selectedRobot instanceof Nao ) {
                connector = new NaoConnector((Nao) selectedRobot);
            } else {
                throw new UnsupportedOperationException("Selected robot not supported!");
            }

            this.controller.setConnector(connector);
            connector.run(); // Blocking until the connector is finished

            showHelp = false;
        }
    }
}
