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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class Ping implements Callable<String> {
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

/**
 * Detector class for mDNS robots. Searches for mDNS in all network connections.
 */
public class RaspberrypiDetector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(RaspberrypiDetector.class);


    private static final long TIMEOUT = 1000L;

    @Override
    public List<IRobot> detectRobots() {
        Collection<IRobot> detectedRobots = new HashSet<>(5);


//        Collection<IRobot> robot = detectRobotsOnAddress();
//        if (!robot.isEmpty()) {
//            detectedRobots.addAll(robot);
//        }
//
//
//        return detectedRobots.stream().collect(Collectors.toList());
        return null;
    }

    public Collection<IRobot> detectRobotsOnAddress(String address) throws InterruptedException, IOException {
        Collection<IRobot> allRobots = new HashSet<>(5);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        Ping ping = new Ping(address);
        Future<String> result = executor.submit(ping);

        executor.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS);
        String ip = "";
        try {
            ip = result.get();
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


}
