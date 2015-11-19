
package faf.pad1.common;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class File implements IIO {
    public File(String name) {
        filename = name;
    }
    
    @Override
    public String read() throws IOException {
        char[] buf;
        try (FileReader in = new FileReader(filename)) {
            buf = new char[2^16];
            in.read(buf);
            in.close();
        }
        return new String(buf);
    }

    @Override
    public void write(String s) throws IOException {
        PrintWriter out = new PrintWriter(filename);
        out.write(s);
        out.close();
    }
    
    private final String filename;
}
