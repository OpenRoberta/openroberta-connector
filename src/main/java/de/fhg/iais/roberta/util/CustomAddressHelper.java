package de.fhg.iais.roberta.util;


import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class CustomAddressHelper {
    private static final Logger LOG = LoggerFactory.getLogger(CustomAddressHelper.class);

    private static final int MAX_ADDRESS_ENTRIES = 5;
    private static final String CUSTOM_ADDRESSES_FILENAME =
            SystemUtils.getUserHome().getPath() + File.separator + "OpenRobertaConnector" + File.separator + "customaddresses.txt";
    private static final String ADDRESS_DELIMITER = " "; // colon may be used in ipv6 addresses

    private Deque<Pair<String, String>> addresses = new ArrayDeque<>();

    public CustomAddressHelper() {
        this.addresses.addAll(loadFromFile());
    }

    public Deque<Pair<String, String>> get() {
        return addresses;
    }

    private static List<Pair<String, String>> loadFromFile() {
        try {
            Collection<Pair<String, String>> addresses = new ArrayList<>();

            List<String> readAddresses = Files.readAllLines(new File(CUSTOM_ADDRESSES_FILENAME).toPath(), StandardCharsets.UTF_8);

            for ( String address : readAddresses ) {
                Pair<String, String> ipPort = extractIpAndPort(address);
                if ( ipPort != null ) {
                    addresses.add(ipPort);
                }
            }

            return addresses.stream().limit(MAX_ADDRESS_ENTRIES).collect(Collectors.toList());
        } catch ( NoSuchFileException e ) {
            LOG.info("No {} file found. Creating one when closing the program.", CUSTOM_ADDRESSES_FILENAME);
        } catch ( IOException e ) {
            LOG.warn("Something went wrong while reading the custom addresses: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private static Pair<String, String> extractIpAndPort(String address) {
        String[] s = address.split(ADDRESS_DELIMITER);

        if ( s.length == 1 ) {
            return new Pair<>(s[0], "");
        } else if ( s.length == 2 ) {
            String sPort = s[1];

            if ( validatePort(sPort) ) {
                return new Pair<>(s[0], sPort);
            }
        }
        return null;
    }

    public void add(Pair<String, String> address) {
        this.addresses.addFirst(address);
        this.addresses = this.addresses.stream().distinct().limit(MAX_ADDRESS_ENTRIES).collect(Collectors.toCollection(ArrayDeque::new));
    }

    public void save() {
        // space as delimiter, colon may be used in ipv6
        List<String>
            collect =
            this.addresses.stream()
                .map(address -> address.getFirst() + ADDRESS_DELIMITER + address.getSecond())
                .limit(MAX_ADDRESS_ENTRIES)
                .collect(Collectors.toList());
        try {
            Files.write(new File(CUSTOM_ADDRESSES_FILENAME).toPath(), collect, StandardCharsets.UTF_8);
        } catch ( IOException e ) {
            LOG.error("Something went wrong while writing the custom addresses: {}", e.getMessage());
        }
    }

    public static boolean validatePort(String port) {
        try {
            int p = Integer.valueOf(port);

            if ( (p >= 0) && (p <= 65535) ) {
                return true;
            }
        } catch ( NumberFormatException e ) {
            LOG.error("The given port is invalid: {}", port);
        }

        return false;
    }
}
