/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package faf.pad1.broker;

import faf.pad1.common.Message;
import faf.pad1.common.Network;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
    
    private Main() {
        messageQueue = new LinkedBlockingQueue();
        subscribers = new ConcurrentHashMap<String, String[]>() {};
        executor = Executors.newCachedThreadPool();
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                executor.shutdown();
            }
        });
    }
    
    public static void main(String[] args) throws InterruptedException {
        
        new Main().run();
        
    }

    private void run() throws InterruptedException {
                
        final Network net = new Network("localhost", 3000);
        
        executor.submit(() -> {
            String s;
            while(true) {
                try {
                    messageQueue.put(net.read());
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        
        while(true) {
            Message msg = Message.parse(messageQueue.take());
            System.out.println(msg.serialize());
            
            if ("subscribe".equals(msg.type)) {
                String[] subscriptionData = msg.payload.split(" ");
                String hostInfo = subscriptionData[0];
                subscribers.putIfAbsent(hostInfo, subscriptionData[1].split(","));
                System.out.println(hostInfo + " subscribed to " + subscriptionData[1]);
            } else {
                subscribers.entrySet().stream()
                    .filter((pair) -> (Arrays.asList(pair.getValue()).contains(msg.type)))
                    .forEach((pair) -> dispatchMessageTo(pair.getKey(), msg));
            }
        }
    }
    
    private void dispatchMessageTo(final String hostString, final Message msg) {
        final String[] hostInfo = hostString.split(":");
        executor.submit(() -> {
            Network net = new Network(hostInfo[0], Integer.parseInt(hostInfo[1]));
            try {
                net.write(msg.serialize());
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
    final BlockingQueue<String> messageQueue;
    final ConcurrentHashMap<String, String[]> subscribers;
    final ExecutorService executor;  
}
