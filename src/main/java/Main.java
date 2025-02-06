import com.google.gson.Gson;
import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.IOException;
import java.net.Socket;




public class Main {
    private static final Gson gson = new Gson();
    

    public static void main(String[] args) {

        List<String> peerList;
        String peerIPAndPort;
        String torrentFilePath;
        String pieceStoragePath;


        
        String command = args[0]; 
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            try {
              
                // Decode the Bencoded value;
                Object decoded = decodeBencode(bencodedValue);
                // Convert the decoded value to JSON and print it
                System.out.println(gson.toJson(decoded));
              }
             catch (RuntimeException e) {
                // Print the error message if decoding fails n
                System.out.println(e.getMessage());
            }
        }  
        else if("info".equals(command)){
            try{
            String filePath = args[1];
			Torrent torrent = new Torrent(Files.readAllBytes(Path.of(filePath)));
            System.out.println("Tracker URL: " + torrent.announce);
			System.out.println("Length: " + torrent.length);
            System.out.println("Info Hash: "+Util.bytesToHex(torrent.infoHash));
            System.out.println("Piece Length: "+torrent.plength);
            System.out.println("Piece Hashes: ");
            List<String> list=torrent.p;
            for(int i=0;i<list.size();i++){
                System.out.println(list.get(i));
            }

            }
            catch (Exception e) {
                // Handle the exception
                System.err.println("An IOException occurred: " + e.getMessage());
            }
        }
        else if("peers".equals(command)){
            try{
                String filePath = args[1];
			    Torrent torrent = new Torrent(Files.readAllBytes(Path.of(filePath)));
                peerList = TorrentDownloader.getPeerList(torrent);
                for (String peer : peerList) {
                    System.out.println(peer);
                }
            
            
            }
            catch (Exception e) {
                // Handle the exception
                System.err.println("An IOException occurred: " + e.getMessage());
            }
        }
        else if("handshake".equals(command)){
            try{
                String filePath = args[1];
			    Torrent torrent = new Torrent(Files.readAllBytes(Path.of(filePath)));
                peerIPAndPort = args[2];
                String peerIP = peerIPAndPort.split(":")[0];
                int peerPort = Integer.parseInt(peerIPAndPort.split(":")[1]);
                try (Socket socket = new Socket(peerIP, peerPort)){
                    TCPService tcpService = new TCPService(socket);
                    TorrentDownloader.performHandshake(Util.bytesToHex(torrent.infoHash), tcpService, false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            
            
            }
            catch (Exception e) {
                // Handle the exception
                System.err.println("An IOException occurred: " + e.getMessage());
            }
        }
        else if("download_piece".equals(command)){
            try{
                pieceStoragePath = args[2];
                String filePath = args[3];
                Torrent torrent = new Torrent(Files.readAllBytes(Path.of(filePath)));
                int pieceIndex = Integer.parseInt(args[4]);//System.out.println( pieceIndex );
                byte[] piece = TorrentDownloader.downloadPiece(torrent, pieceIndex, false);
                Util.writePieceToFile(pieceStoragePath, piece);
            }
            catch (Exception e) {
                // Handle the exception
                System.err.println("An IOException occurred: " + e.getMessage());
            }

        }
         else {
            System.out.println("Unknown command: " + command);
        }
    }

    static Object decodeBencode(String bencodedData) {
        try {
            // Convert the Bencoded string to a byte array
            byte[] bencodedBytes = bencodedData.getBytes(StandardCharsets.UTF_8);

            // Initialize the Bencode decoder
            Bencode bencode = new Bencode(StandardCharsets.UTF_8);

            
            char firstChar = bencodedData.charAt(0);
            if (firstChar == 'i') {
                // Decode as integer
                return bencode.decode(bencodedBytes, Type.NUMBER);
            }
            else if (firstChar == 'l') {
                // Decode as list
                return bencode.decode(bencodedBytes, Type.LIST);
            } else if (firstChar == 'd') {
                // Decode as dictionary
                return bencode.decode(bencodedBytes, Type.DICTIONARY);
            } else if (Character.isDigit(firstChar)) {
                // Decode as string
                return bencode.decode(bencodedBytes, Type.STRING);
            } else {
                throw new RuntimeException("Unsupported Bencoded data type");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error decoding Bencoded data: " + e.getMessage(), e);
        }
    }

    

        
}



 