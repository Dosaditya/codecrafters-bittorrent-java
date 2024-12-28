import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.util.*;
import java.nio.ByteBuffer;
import java.net.URLEncoder;

public class HttpRequest {

    public final HttpClient client;

    public static void request(Torrent torrent) {

        String url = torrent.announce;
        String infoHash = Util.bytesToHex(torrent.infoHash);

        byte[] peerIdBytes = Util.getRandomBytes(10);
        String peerId = Util.bytesToHex(peerIdBytes);

        
            try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .addHeader("info_hash", URLEncoder.encode(infoHash, StandardCharsets.ISO_8859_1))
                    .addHeader("peer_id", URLEncoder.encode(peerId, StandardCharsets.ISO_8859_1))
                    .addHeader("port", URLEncoder.encode("6881", StandardCharsets.ISO_8859_1))
                    .addHeader("uploaded", URLEncoder.encode("0", StandardCharsets.ISO_8859_1))
                    .addHeader("downloaded", URLEncoder.encode(infoHash, StandardCharsets.ISO_8859_1))
                    .addHeader("left", URLEncoder.encode(String.valueOf(torrent.length), StandardCharsets.ISO_8859_1))
                    .addHeader("compact", URLEncoder.encode("1", StandardCharsets.ISO_8859_1))
                    .build();

                    HttpResponse<byte[]> response=client.send(request, HttpResponse.BodyHandlers.ofByteArray());

             
                

                    //  = response.body().bytes();
                    Bencode bencode = new Bencode(true);
                    Map<String, Object> decodedResponse = bencode.decode(response.body(), Type.DICTIONARY);
                    System.out.println(decodedResponse);
                    System.out.println("eg");

                    byte[] peersBytes = ((ByteBuffer) decodedResponse.get("peers")).array();

                    List<String> peerList = new ArrayList<>();
                    for (int i = 0; i < peersBytes.length; i += 6) {
                        String ip = String.format("%d.%d.%d.%d", peersBytes[i] & 0xff, peersBytes[i + 1] & 0xff,
                                peersBytes[i + 2] & 0xff, peersBytes[i + 3] & 0xff);
                        int port = ((peersBytes[i + 4] & 0xff) << 8) | (peersBytes[i + 5] & 0xff);
                        peerList.add(ip + ":" + port);
                    }
                    Util.print(peerList);

                

            
        }
         catch (Exception e) {
            e.printStackTrace();
        }

    }
}