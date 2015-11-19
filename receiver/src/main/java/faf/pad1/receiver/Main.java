/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package faf.pad1.receiver;

import faf.pad1.common.Message;
import faf.pad1.common.Network;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrew
 */
public class Main {
    public static void main(String[] args) throws SocketException, InterruptedException, ExecutionException, IOException {

        new Main(args).run();

    }

    private Main(String[] args) {
        brokerHost = args[0];
        brokerPort = Integer.parseInt(args[1]);

        host = "0.0.0.0";
        port = Integer.parseInt(args[2]);

        msgTypes = Arrays.copyOfRange(args, 3, args.length);

        messageQueue = new LinkedBlockingQueue();
        executor = Executors.newCachedThreadPool();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                executor.shutdown();
            }
        });
    }

    private void run() throws IOException, InterruptedException {
        subscribe();

        final Network net = new Network(host, port);

        executor.submit(() -> {
            while(true) {
                try {
                    messageQueue.put(net.read());
                } catch (InterruptedException | IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        while(true) {
            Message msg = Message.parse(messageQueue.take());

            System.out.print(msg.type);
            System.out.print(": ");
            System.out.println(msg.payload);
        }

    }

    private void subscribe() throws IOException {
        Network net = new Network(brokerHost, brokerPort);
        String payload = host + ":" + Integer.toString(port) + " ";
        for(int i=0; i<msgTypes.length; ++i) {
            if (i != 0) {
                payload += ",";
            }
            payload += msgTypes[i];
        }
        Message subscription = new Message("subscribe", payload);
        net.write(subscription.serialize());
    }

    final private String brokerHost;
    final private int brokerPort;

    final private String host;
    final private int port;

    final private String[] msgTypes;

    final BlockingQueue<String> messageQueue;
    final ExecutorService executor;

}
