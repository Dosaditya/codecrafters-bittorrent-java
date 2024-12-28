
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.nio.charset.StandardCharsets;
import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.util.*;
import java.nio.ByteBuffer;
import java.net.URLEncoder;

public class HttpRequest{
     
    

    public static void request(Torrent torrent){
        
        
        
            
            String url=torrent.announce;
            String infoHash = Util.bytesToHex(torrent.infoHash);
        
            byte[] peerIdBytes = Util.getRandomBytes(10);
            String peerId = Util.bytesToHex(peerIdBytes);    


            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                .url(url)
                .get() 
                .addHeader("info_hash",URLEncoder.encode(infoHash,StandardCharsets.UTF_8))
                .addHeader("peer_id",URLEncoder.encode(peerId,StandardCharsets.UTF_8))
                .addHeader("port", URLEncoder.encode("6881",StandardCharsets.UTF_8))
                .addHeader("uploaded",URLEncoder.encode("0",StandardCharsets.UTF_8))
                .addHeader("downloaded",URLEncoder.encode(infoHash,StandardCharsets.UTF_8))
                .addHeader("left",URLEncoder.encode(String.valueOf(torrent.length),StandardCharsets.UTF_8))
                .addHeader("compact",URLEncoder.encode("1",StandardCharsets.UTF_8))
                .build();

            
            try (Response response = client.newCall(request).execute()){
                if(response.isSuccessful()==false){
                    System.out.println("Somethings Wrong");
                }
                else{

                    byte[] responseBytes = response.body().bytes();
                    Bencode bencode=new Bencode(true);
                    Map<String ,Object> decodedResponse=bencode.decode(responseBytes,Type.DICTIONARY);
                    byte[] peersBytes=((ByteBuffer)decodedResponse.get("peers")).array();

                    List<String> peerList = new ArrayList<>();
                        for (int i = 0; i < peersBytes.length; i += 6) {
                            String ip = String.format("%d.%d.%d.%d", peersBytes[i] & 0xff, peersBytes[i + 1] & 0xff,
                                peersBytes[i + 2] & 0xff, peersBytes[i + 3] & 0xff);
                            int port = ((peersBytes[i + 4] & 0xff) << 8) | (peersBytes[i + 5] & 0xff);
                            peerList.add(ip + ":" + port);
                        }
                    Util.print(peerList);   



                }
            

        }
            catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    
