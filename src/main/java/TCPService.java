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

    public byte[] waitForMessage() {
        try {
            // Read the length of the incoming message (assuming the length is sent as the first 4 bytes)
            byte[] lengthBuffer = new byte[4];
            int bytesRead = in.read(lengthBuffer); 
            if (bytesRead != 4) {
                throw new IOException("Failed to read message length");
            }
            int messageLength = ByteBuffer.wrap(lengthBuffer).getInt();
            // Allocate a buffer of the appropriate size
            byte[] messageBuffer = new byte[messageLength];
            
            bytesRead = in.readNBytes(messageBuffer, 0, messageLength);
            if (bytesRead != messageLength) {
                throw new IOException("Failed to read the complete message");
            }
            return messageBuffer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] createRequestPayload(int index, int begin, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(index);
        buffer.putInt(begin);
        buffer.putInt(length);
        return buffer.array();
    }

    @Override
    public void close() throws IOException {
        in.close();
        out.close();
    } 

} 