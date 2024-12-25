import com.google.gson.Gson;
import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;




public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
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
            System.out.println("Info Hash: "+bytesToHex(torrent.infoHash));
            System.out.println("Piece Length: "+torrent.plength);
            System.out.println("Piece Hashes: ");
            byte[]piecesBytes =torrent.Hash;
            for (int i = 0; i < piecesBytes.length; i += 20) {
                byte[] hashBytes = new byte[20];
                System.arraycopy(piecesBytes, i, hashBytes, 0, 20); // Extract each 20-byte chunk
                System.out.println(bytesToHex(hashBytes)); // Convert to hex
            }

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

    private static String bytesToHex(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b:bytes){
          sb.append(String.format("%02x",b));
        }
              return sb.toString();  
        }

        
}



 