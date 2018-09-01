package com.github.manevolent.ts3j.protocol;

import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

public class TS3DNS {
    private static final int TS3_VOICE_DEFAULT_PORT = 9987;
    private static final int TS3_QUERY_DEFAULT_PORT = 10011;
    private static final int TS3_FILETRANSFER_DEFAULT_PORT = 30033;
    private static final int TSDNS_DEFAULT_PORT = 41144;

    private static final String DNS_PREFIX_TCP = "_tsdns._tcp.";
    private static final String DNS_PREFIX_UDP = "_ts3._udp.";

    private static List<InetSocketAddress> lookupSRV(String domain) throws IOException {
        org.xbill.DNS.Record[] records = new Lookup(domain, Type.SRV).run();

        if (records == null || records.length <= 0) throw new UnknownHostException("Host not found");

        return Arrays.stream(records)
                .filter(Objects::nonNull)
                .map(x -> (SRVRecord) x)
                .filter(x -> x.getTarget() != null)
                .sorted((a, b) -> {
                    // Loose sorting
                    int prioComp = Integer.compare(a.getPriority(), b.getPriority());
                    if (prioComp != 0) return prioComp;
                    else return Integer.compare(a.getWeight(), b.getWeight());
                })
                .map(x -> new InetSocketAddress(x.getTarget().toString(), x.getPort()))
                .collect(Collectors.toList());
    }

    public static List<InetSocketAddress> lookup(String domain) throws IOException {
        return lookupSRV(DNS_PREFIX_UDP + domain);
    }
}
