package de.fhg.iais.roberta.connection.wireless;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wireless.robotino.Robotino;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HostnameDetector implements IDetector {

    private static final Logger LOG = LoggerFactory.getLogger(HostnameDetector.class);

    private static final Map<String, Class<? extends AbstractWirelessRobot>> ROBOT_HOSTNAMES = new HashMap<>(1);

    static {
        ROBOT_HOSTNAMES.put("robotino.local", Robotino.class);
    }

    @Override
    public List<IRobot> detectRobots() {
        Collection<IRobot> detectedRobots = new HashSet<>(5);
        for (Map.Entry<String, Class<? extends AbstractWirelessRobot>> entry : ROBOT_HOSTNAMES.entrySet()) {
            LOG.info("Looking for {} in local network", entry.getKey());
            IRobot robot = detectLocalRobotsWithHostname(entry);
            if (robot != null) {
                detectedRobots.add(robot);
            }
        }
        return detectedRobots.stream().filter(distinctByKey(IRobot::getName)).collect(Collectors.toList());
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public AbstractWirelessRobot detectLocalRobotsWithHostname(Map.Entry<String, Class<? extends AbstractWirelessRobot>> entry) {
        String hostname = entry.getKey();
        try {
            InetAddress robotAddress = InetAddress.getByName(hostname);
            LOG.info("Found robot hostname {} with IP Address: {}", hostname, robotAddress);
            return entry.getValue().getConstructor(String.class, InetAddress.class).newInstance(hostname, robotAddress);
        } catch (UnknownHostException e) {
            LOG.info("can't find host {}", hostname);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 InstantiationException e) {
            LOG.error("Robot class not implemented: {}", e.getMessage());
        }
        return null;
    }
}
