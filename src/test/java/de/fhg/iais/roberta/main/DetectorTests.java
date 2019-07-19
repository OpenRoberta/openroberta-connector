package de.fhg.iais.roberta.main;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.arduino.Arduino;
import de.fhg.iais.roberta.connection.arduino.ArduinoDetector;
import de.fhg.iais.roberta.connection.arduino.ArduinoType;
import de.fhg.iais.roberta.connection.ev3.Ev3;
import de.fhg.iais.roberta.connection.ev3.Ev3Detector;
import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;
import de.fhg.iais.roberta.testUtils.TestListenable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DetectorTests {

    @Test
    void getDetectedRobots_ShouldOnlyReturnDetectedRobots_WhenRun() {
        IDetector arduDDetector = new TestArduinoDetectedDetector();
        IDetector ev3DDetector = new TestEv3DetectedDetector();
        IDetector nDDetector = new TestNoRobotDetectedDetector();

        RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(arduDDetector, ev3DDetector));
        Set<Robot> detectedRobots = new HashSet<>();
        while ( !robotDetectorHelper.allDetectorsRanOnce() ) {
            detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
        }
        detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
        assertThat(detectedRobots, notNullValue());
        assertThat(detectedRobots, containsInAnyOrder(isA(Arduino.class), isA(Ev3.class)));

        robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(arduDDetector, nDDetector));
        detectedRobots = new HashSet<>();
        while ( !robotDetectorHelper.allDetectorsRanOnce() ) {
            detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
        }
        detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
        assertThat(detectedRobots, notNullValue());
        assertThat(detectedRobots, iterableWithSize(1));
        assertThat(detectedRobots, contains(isA(Arduino.class)));

        robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(nDDetector, ev3DDetector));
        detectedRobots = new HashSet<>();
        while ( !robotDetectorHelper.allDetectorsRanOnce() ) {
            detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
        }
        detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
        assertThat(detectedRobots, notNullValue());
        assertThat(detectedRobots, iterableWithSize(1));
        assertThat(detectedRobots, contains(isA(Ev3.class)));

        robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(nDDetector, nDDetector));
        detectedRobots = new HashSet<>();
        while ( !robotDetectorHelper.allDetectorsRanOnce() ) {
            detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
        }
        detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
        assertThat(detectedRobots, notNullValue());
        assertThat(detectedRobots, empty());
    }

    @Test
    void getSelectedRobot_ShouldReturnSelectedRobot_WhenRobotIsSelected() {
        RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(new TestArduinoDetectedDetector(), new TestEv3DetectedDetector()));
        IOraListenable<Robot> robotTestListenable = new TestListenable<>();

        robotTestListenable.registerListener(robotDetectorHelper);

        robotTestListenable.fire(new Ev3("EV3"));
        Robot selectedRobot = robotDetectorHelper.getSelectedRobot();

        assertThat(selectedRobot, isA(Ev3.class));
        assertThat(selectedRobot.getName(), is("EV3"));

        robotTestListenable.fire(new Arduino(ArduinoType.UNO, "1234"));
        selectedRobot = robotDetectorHelper.getSelectedRobot();

        assertThat(selectedRobot, isA(Arduino.class));
    }

    private static class TestEv3DetectedDetector implements IDetector {
        @Override
        public List<Robot> detectRobots() {
            return Collections.singletonList(new Ev3("EV3"));
        }

        @Override
        public Class<? extends Robot> getRobotClass() {
            return Ev3.class;
        }
    }

    private static class TestArduinoDetectedDetector implements IDetector {
        @Override
        public List<Robot> detectRobots() {
            return Collections.singletonList(new Arduino(ArduinoType.UNO, "1234"));
        }

        @Override
        public Class<? extends Robot> getRobotClass() {
            return Arduino.class;
        }
    }
    private static class TestNoRobotDetectedDetector implements IDetector {
        @Override
        public List<Robot> detectRobots() {
            return Collections.emptyList();
        }

        @Override
        public Class<? extends Robot> getRobotClass() {
            return Arduino.class;
        }
    }
}
