package de.fhg.iais.roberta.connection.wireless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wireless.nao.Nao;

/**
 * Detector class for mDNS robots. Searches for mDNS in all network connections.
 */
public class mDnsDetector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(mDnsDetector.class);

    private static final Map<String, Class<? extends AbstractWirelessRobot>> SERVICE_TYPES = new HashMap<>(1);
    static {
        SERVICE_TYPES.put("_naoqi._tcp.local.", Nao.class);
    }

    private static final long TIMEOUT = 1000L;

    @Override
    public List<IRobot> detectRobots() {
        Collection<IRobot> detectedRobots = new HashSet<>(5);

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while ( interfaces.hasMoreElements() ) {
                NetworkInterface currentNif = interfaces.nextElement();
                // Only regard active and non localhost networks
                if ( !currentNif.isUp() || currentNif.isLoopback() ) {
                    continue;
                }

                // Check all associated addresses of network interface
                Enumeration<InetAddress> addresses = currentNif.getInetAddresses();
                while ( addresses.hasMoreElements() ) {
                    InetAddress address = addresses.nextElement();
                    LOG.info("Looking in {} {}", currentNif.getName(), address);

                    IRobot robot = detectRobotsOnAddress(address, currentNif);
                    if ( robot != null ) {
                        detectedRobots.add(robot);
                    }
                }
            }
        } catch ( UnknownHostException e ) {
            LOG.error("Could not add network interface to IPv6 address: {}", e.getMessage());
        } catch ( SocketException e ) {
            LOG.error("Could not create or access socket: {}", e.getMessage());
        } catch ( IOException e ) {
            LOG.error("Something went wrong: {}", e.getMessage());
        }

        // Remove duplicates, filters the NAOs by name
        return detectedRobots.stream().filter(distinctByKey(IRobot::getName)).collect(Collectors.toList());
    }

    /**
     * Helper method to create a distinct list by a specific property.
     * https://stackoverflow.com/questions/23699371/java-8-distinct-by-property
     *
     * @param keyExtractor the function to be used for distinction
     * @param <T>          the class of the function
     * @return the predicate to be used e.g. by a filter
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * Uses JmDNS to find mDNS robots on the address and network interface.
     *
     * @param address the network address where JmDNS should look for the robot
     * @param nif     the network interface the address is associated to
     * @return a detected robot or null, if none was found
     * @throws IOException          if something went wrong with the JmDNS creation
     * @throws UnknownHostException if the network interface could not be added to the found address
     */
    private static AbstractWirelessRobot detectRobotsOnAddress(InetAddress address, NetworkInterface nif) throws IOException, UnknownHostException {
        try (JmDNS jmDNS = JmDNS.create(address, address.getHostName())) {
            for ( Entry<String, Class<? extends AbstractWirelessRobot>> entry : SERVICE_TYPES.entrySet() ) {
                ServiceInfo[] list = jmDNS.list(entry.getKey(), TIMEOUT);

                for ( ServiceInfo info : list ) {
                    InetAddress[] adresses = info.getInetAddresses();

                    if ( adresses.length > 0 ) {
                        InetAddress robotAddress = adresses[0];
                        String name = info.getName();

                        // Add network interface to the IPv6 address, as JmDNS omits that information
                        if ( robotAddress instanceof Inet6Address ) {
                            robotAddress = Inet6Address.getByAddress(robotAddress.getHostName(), robotAddress.getAddress(), nif);
                        }

                        LOG.info("Found mDNS robot {} with IP Address: {}", name, robotAddress);
                        return entry.getValue().getConstructor(String.class, InetAddress.class).newInstance(name, robotAddress);
                    }
                }
            }
        } catch ( NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e ) {
            LOG.error("Robot class not implemented: {}", e.getMessage());
        }
        return null;
    }
}
