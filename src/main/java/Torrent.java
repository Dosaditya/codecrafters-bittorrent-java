import com.google.gson.Gson;
import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;
import java.util.Map;




public class Torrent {
	public String announce; 
	public long length;
    public long plength;
    public byte[] infoHash;
    public String pieceHash;
    public byte[] Hash;
    public List<String> p;

	
    public Torrent(byte[] bytes) throws NoSuchAlgorithmException{
		
        try{
        Bencode bencode = new Bencode(false);
        Bencode bencode2 = new Bencode(true);
        

		Map<String, Object> root = bencode.decode(bytes, Type.DICTIONARY);
		Map<String, Object> info = (Map<String, Object>) root.get("info");

        
		Map<String, Object> info2 = (Map<String, Object>) bencode2.decode(bytes, Type.DICTIONARY).get("info");
        Hash = ((ByteBuffer) info2.get("pieces")).array();
        

        //System.out.println(info.get("pieces").getClass());

		announce = (String) root.get("announce");
		length = (long) info.get("length");
        plength=(long) info.get("piece length");
        pieceHash=(String)info.get("pieces");
        

        MessageDigest digest2 = MessageDigest.getInstance("SHA-1");
        infoHash = digest2.digest(bencode2.encode(
        (Map<String, Object>)bencode2.decode(bytes, Type.DICTIONARY)
            .get("info")));

        }
        catch (Exception e) {
            throw new RuntimeException("Error decoding Bencoded data: " + e.getMessage(), e);
        }

        byte[]piecesBytes =torrent.Hash; 
            for (int i = 0; i < piecesBytes.length; i += 20) {
                byte[] hashBytes = new byte[20];
                System.arraycopy(piecesBytes, i, hashBytes, 0, 20); // Extract each 20-byte chunk
                p.add(Util.bytesToHex(hashBytes)); // Convert to hex
            }
 
	}
} 