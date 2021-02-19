package de.fhg.iais.roberta.connection.wireless;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wireless.raspberrypi.Raspberrypi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Detector class for mDNS robots. Searches for mDNS in all network connections.
 */
public class RaspberrypiDetectorOld implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(RaspberrypiDetectorOld.class);


    private static final long TIMEOUT = 1000L;

    @Override
    public List<IRobot> detectRobots() {
        Collection<IRobot> detectedRobots = new HashSet<>(5);

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface currentNif = interfaces.nextElement();
                // Only regard active and non localhost networks
                if (!currentNif.isUp() || currentNif.isLoopback()) {
                    continue;
                }

                // Check all associated addresses of network interface
                Enumeration<InetAddress> addresses = currentNif.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    LOG.info("Looking in {} {}", currentNif.getName(), address);

                    Collection<IRobot> robot = detectRobotsOnAddress(address, currentNif);
                    if (!robot.isEmpty()) {
                        detectedRobots.addAll(robot);
                    }
                }
            }
        } catch (SocketException e) {
            LOG.error("Could not create or access socket: {}", e.getMessage());
        } catch (IOException | InterruptedException e) {
            LOG.error("Something went wrong: {}", e.getMessage());
        }

        return detectedRobots.stream().collect(Collectors.toList());
    }

    public Collection<IRobot> detectRobotsOnAddress(InetAddress address, NetworkInterface ni) throws InterruptedException, IOException {
        Collection<IRobot> allRobots = new HashSet<>(5);
        String ipPrefix = address.getHostAddress();
        ipPrefix = ipPrefix.substring(0, ipPrefix.lastIndexOf(".") + 1);
        if (ipPrefix.equals("")) {
            return allRobots;
        }
        ExecutorService executor = Executors.newFixedThreadPool(200);

        List<Future<String>> resultList = new ArrayList<>();
        for (int i = 1; i <= 254; i++) {
            String ip = ipPrefix + i;
            Ping ping = new Ping(ip);
            Future<String> result = executor.submit(ping);
            resultList.add(result);
        }

        executor.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS);

        for (int i = 0; i < resultList.size(); i++) {
            Future<String> result = resultList.get(i);
            String ip = null;
            try {
                ip = result.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            if (!ip.equals("")) {
                String mac = getClientMACAddress(ip).toLowerCase(Locale.ROOT);
                if (mac.startsWith("b8:27:eb") || mac.startsWith("dc:a6:32") || mac.startsWith("e4:5f:01")) {
                    InetAddress ipAddress = InetAddress.getByName(ip);
                    allRobots.add(new Raspberrypi("raspberry", mac, ipAddress, 22, "pi", "raspberry"));
                }
            }
        }

        executor.shutdown();
        return allRobots;
    }

    public String getClientMACAddress(String clientIp) {
        String str = "";

        Pattern pattern = Pattern.compile("\\w\\w:\\w\\w:\\w\\w:\\w\\w:\\w\\w:\\w\\w");

        try {
            Process p = Runtime.getRuntime().exec("arp -n " + clientIp);
            InputStreamReader ir = new InputStreamReader(p.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (int i = 1; i < 100; i++) {
                str = input.readLine();
                if (str != null) {
                    Matcher matcher = pattern.matcher(str);
                    if (matcher.find()) {
                        return matcher.group();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return "";
    }


    static class Ping implements Callable<String> {
        private final String ip;

        public Ping(String ip) {
            this.ip = ip;
        }

        @Override
        public String call() throws Exception {

            InetAddress inet = InetAddress.getByName(this.ip);
            if (inet.isReachable(1000)) {
                return this.ip;
            }
            return "";
        }
    }


}
