import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class TCPService implements Closeable{

    private InputStream in;
    private OutputStream out;

    public TCPService(Socket socket) {
        try {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendMessage(byte[] message) {
        try {
            out.write(message);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] waitForHandshakeResponse() {
        try {
            byte[] handshakeResponse = new byte[68];
            int bytesRead = in.read(handshakeResponse);
            if (bytesRead != 68) {
                throw new IOException("Failed to read handshake response");
            }
            return handshakeResponse;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
        out.close();
    } 

} 