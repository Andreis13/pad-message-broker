
package faf.pad1.sender;

import faf.pad1.common.Message;
import faf.pad1.common.Network;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException, FileNotFoundException, IOException {

        final ExecutorService es = Executors.newCachedThreadPool();
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                es.shutdown();
            }
        });
        
        final Network broker = new Network(args[0], Integer.parseInt(args[1]));
        
        while(true)
        {
            final Message msg = readMessageFromKeyboard();

            es.submit(() -> {
                try {
                    broker.write(msg.serialize());
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    private static Message readMessageFromKeyboard() {
        String type;
        String payload;
        
        Scanner s = new Scanner(System.in);
        
        System.out.println("Enter the message type:");
        type = s.next();
        
        System.out.println("Enter the message payload:");
        payload = s.next();
        
        return new Message(type, payload);
    }
}
