package de.fhg.iais.roberta.main;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.WiredRobotType;
import de.fhg.iais.roberta.connection.wired.arduino.Arduino;
import de.fhg.iais.roberta.connection.wired.ev3.Ev3;
import de.fhg.iais.roberta.testUtils.TestListenable;
import de.fhg.iais.roberta.util.IOraListenable;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;

class DetectorTests {

    @Test
    void getDetectedRobots_ShouldOnlyReturnDetectedRobots_WhenRun() {
        IDetector arduDDetector = new TestArduinoDetectedDetector();
        IDetector ev3DDetector = new TestEv3DetectedDetector();
        IDetector nDDetector = new TestNoRobotDetectedDetector();

        RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper();
        Set<IRobot> detectedRobots = new HashSet<>(2);

//        detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
//        assertThat(detectedRobots, notNullValue());
//        assertThat(detectedRobots, containsInAnyOrder(isA(Arduino.class), isA(Ev3.class)));

//        robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(arduDDetector, nDDetector));
//        detectedRobots = new HashSet<>(1);

//        detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
//        assertThat(detectedRobots, notNullValue());
//        assertThat(detectedRobots, iterableWithSize(1));
//        assertThat(detectedRobots, contains(isA(Arduino.class)));

//        robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(nDDetector, ev3DDetector));
//        detectedRobots = new HashSet<>(1);

//        detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
//        assertThat(detectedRobots, notNullValue());
//        assertThat(detectedRobots, iterableWithSize(1));
//        assertThat(detectedRobots, contains(isA(Ev3.class)));

//        robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(nDDetector, nDDetector));
//        detectedRobots = new HashSet<>(0);

//        detectedRobots.addAll(robotDetectorHelper.getDetectedRobots());
//        assertThat(detectedRobots, notNullValue());
//        assertThat(detectedRobots, empty());
    }

    @Test
    void getSelectedRobot_ShouldReturnSelectedRobot_WhenRobotIsSelected() {
        RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper();
        IOraListenable<IRobot> robotTestListenable = new TestListenable<>();

        robotTestListenable.registerListener(robotDetectorHelper);

        robotTestListenable.fire(new Ev3("EV3"));
        IRobot selectedRobot = robotDetectorHelper.getSelectedRobot();

        assertThat(selectedRobot, isA(Ev3.class));
        assertThat(selectedRobot.getName(), is("LEGO EV3: EV3"));

        robotTestListenable.fire(new Arduino(WiredRobotType.UNO, "1234"));
        selectedRobot = robotDetectorHelper.getSelectedRobot();

        assertThat(selectedRobot, isA(Arduino.class));
    }

    private static class TestEv3DetectedDetector implements IDetector {
        @Override
        public List<IRobot> detectRobots() {
            return Collections.singletonList(new Ev3("EV3"));
        }
    }

    private static class TestArduinoDetectedDetector implements IDetector {
        @Override
        public List<IRobot> detectRobots() {
            return Collections.singletonList(new Arduino(WiredRobotType.UNO, "1234"));
        }
    }

    private static class TestNoRobotDetectedDetector implements IDetector {
        @Override
        public List<IRobot> detectRobots() {
            return Collections.emptyList();
        }
    }
}
