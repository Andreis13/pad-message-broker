
package faf.pad1.common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Network implements IIO {
    public Network(String host, int p) {
        hostname = host;
        port = p;
    }

    @Override
    public String read() throws IOException {
        byte[] buf = new byte[65536];
        DatagramSocket ds = new DatagramSocket(port);
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        ds.receive(packet);
        ds.close();
        return new String(buf, 0, packet.getLength(), StandardCharsets.UTF_8);
    }

    @Override
    public void write(String s) throws IOException {
        DatagramSocket ds = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(s.getBytes(), s.getBytes().length);
        packet.setAddress(InetAddress.getByName(hostname));
        packet.setPort(port);
        ds.send(packet);
        ds.close();
    }

    private final String hostname;
    private final int port;
}
