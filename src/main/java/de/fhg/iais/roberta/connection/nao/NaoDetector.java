package de.fhg.iais.roberta.connection.nao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;

/**
 * Detector class for NAO robots. Searches for NAOs in all network connections.
 */
public class NaoDetector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(NaoDetector.class);

    private static final String NAO_SERVICE_TYPE = "_naoqi._tcp.local.";
    private static final long TIMEOUT = 1000L;

    public NaoDetector() {

    }

    @Override
    public List<IRobot> detectRobots() {
        Collection<IRobot> detectedRobots = new HashSet<>();

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

                    IRobot robot = detectNaoOnAddress(address, currentNif);
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
     * Uses JmDNS to find NAOs on the address and network interface.
     *
     * @param address the network address where JmDNS should look for the robot
     * @param nif     the network interface the address is associated to
     * @return a detected robot or null, if none was found
     * @throws IOException          if something went wrong with the JmDNS creation
     * @throws UnknownHostException if the network interface could not be added to the found address
     */
    private static IRobot detectNaoOnAddress(InetAddress address, NetworkInterface nif) throws IOException, UnknownHostException {
        try (JmDNS jmDNS = JmDNS.create(address, address.getHostName())) {
            ServiceInfo[] list = jmDNS.list(NAO_SERVICE_TYPE, TIMEOUT);

            for ( ServiceInfo info : list ) {
                InetAddress[] naoAddresses = info.getInetAddresses();

                if ( naoAddresses.length > 0 ) {
                    InetAddress naoAddress = naoAddresses[0];
                    String name = info.getName();

                    // Add network interface to the IPv6 address, as JmDNS omits that information
                    if ( naoAddress instanceof Inet6Address ) {
                        naoAddress = Inet6Address.getByAddress(naoAddress.getHostName(), naoAddress.getAddress(), nif);
                    }

                    LOG.info("Found NAO {} with IP Address: {}", name, naoAddress);
                    return new Nao(name, naoAddress);
                }
            }
        }
        return null;
    }
}
