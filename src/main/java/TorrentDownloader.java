import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;


public class TorrentDownloader{


    private static final int PORT = 6881;

    private static final byte UNCHOKE_MESSAGE_ID = 1;
    private static final byte INTERESTED_MESSAGE_ID = 2;
    private static final byte BITFIELD_MESSAGE_ID = 5;
    private static final byte REQUEST_MESSAGE_ID = 6;
    private static final byte PIECE_MESSAGE_ID = 7;
    private static final int BLOCK_SIZE = 16384;

    private static List<String> getPeerListFromHTTPResponse(HttpResponse<byte[]> response) {
        Bencode bencode = new Bencode(true);
        Map<String, Object> decodedResponse = bencode.decode(response.body(), Type.DICTIONARY);
        byte[] peersBytes = ((ByteBuffer) decodedResponse.get("peers")).array();

        List<String> peerList = new ArrayList<>();
        for (int i = 0; i < peersBytes.length; i += 6) {
            String ip = String.format("%d.%d.%d.%d", peersBytes[i] & 0xff, peersBytes[i + 1] & 0xff,
                    peersBytes[i + 2] & 0xff, peersBytes[i + 3] & 0xff);
            int port = ((peersBytes[i + 4] & 0xff) << 8) | (peersBytes[i + 5] & 0xff);
            peerList.add(ip + ":" + port);
        }
        return peerList;
    }
     
    
    
    
    static List<String> getPeerList(Torrent torrent) throws URISyntaxException, IOException, InterruptedException {
        String url = torrent.announce;
        String infoHash = new String(Util.hexStringToByteArray(Util.bytesToHex(torrent.infoHash)),
        StandardCharsets.ISO_8859_1);
        byte[] peerIdBytes = Util.getRandomBytes(10);
        String peerId = Util.bytesToHex(peerIdBytes);

        HttpClientService httpClientService = new HttpClientService();
        String requestURL = httpClientService.newRequestURLBuilder(torrent.announce)
                .addParam("info_hash", infoHash)
                .addParam("peer_id", peerId)
                .addParam("port", String.valueOf(PORT))
                .addParam("uploaded", "0")
                .addParam("downloaded", "0")
                .addParam("left", String.valueOf(torrent.length))
                .addParam("compact", "1")
                .build();

        HttpResponse<byte[]> response = httpClientService.sendGetRequest(requestURL);
        return getPeerListFromHTTPResponse(response);
    }

    static void performHandshake(String infoHash, TCPService tcpService, boolean isMagnetHandshake) {
        byte[] handshakeMessage = createHandshakeMessage(infoHash, isMagnetHandshake);
        tcpService.sendMessage(handshakeMessage);
        byte[] handshakeResponse = tcpService.waitForHandshakeResponse();
        //validateHandshakeResponse(handshakeResponse, Utils.hexStringToByteArray(infoHash), isMagnetHandshake);
        byte[] peerIdBytes = Arrays.copyOfRange(handshakeResponse, handshakeResponse.length - 20, handshakeResponse.length);
        String peerId = Util.bytesToHex(peerIdBytes);
        System.out.println("Peer ID: " + peerId);
    }

    static byte[] createHandshakeMessage(String infoHash, boolean isMagnetHandshake) {
        // create a handshake message to send to the peer
        ByteArrayOutputStream handshakeMessage = new ByteArrayOutputStream();
        try {
            handshakeMessage.write(19);
            handshakeMessage.write("BitTorrent protocol".getBytes());
            byte[] reservedBytes = new byte[] {0,0,0,0,0,0,0,0};
            if (isMagnetHandshake) {
                reservedBytes[5] = 16;
            }
            handshakeMessage.write(reservedBytes);
            handshakeMessage.write(Util.hexStringToByteArray(infoHash));
            handshakeMessage.write("ABCDEFGHIJKLMNOPQRST".getBytes());
            byte[] handshakeMessageBytes = handshakeMessage.toByteArray();
            return handshakeMessageBytes;
        } catch (Exception e) {
            throw new RuntimeException("Error creating handshake message: " + e.getMessage());
        }
    }
    
    public static byte[] downloadPiece(Torrent torrent, int index, boolean isMagnetHandshake) {
        List<String> peerList = null;
        try {
            peerList = getPeerList(torrent);
        } catch (Exception e) {
            throw new RuntimeException("Error getting peer list: " + e.getMessage());
        }

        if (peerList == null || peerList.size() == 0) {
            throw new RuntimeException("No peers available to download from");
        }
        byte piece[] = null;
        for (String peer : peerList) {
            try {
                System.out.println("Downloading piece from peer: " + peer);
                piece = downloadPieceFromPeer(torrent, peer, index, isMagnetHandshake);
                break;
            } catch (Exception e) {
                System.out.println("Error downloading piece from peer: " + peer + ", " + e.getMessage());
            }
        }
        if (piece == null) {
            throw new RuntimeException("Failed to download piece: " + index);
        }
        
        return piece;
    }

    public static byte[] downloadPieceFromPeer(Torrent torrent, String peer, int index, boolean isMagnetHandshake) {
        try (Socket socket = new Socket(peer.split(":")[0], Integer.parseInt(peer.split(":")[1]))) {
            TCPService tcpService = new TCPService(socket);
            int pieceLength = (int) torrent.plength;
           
                performHandshake(Util.bytesToHex(torrent.infoHash), tcpService, false);
                return downloadPieceHelper(pieceLength, tcpService, index);
            
        } catch (Exception e) {
            throw new RuntimeException("Error downloading piece from peer: " + e.getMessage());
        } 
    }

    public static byte[] downloadPieceHelper(int pieceLength, TCPService tcpService, int index) throws Exception {
        byte[] bitfieldMessage = tcpService.waitForMessage();
        if (bitfieldMessage[0] != BITFIELD_MESSAGE_ID) {
            throw new RuntimeException("Expected bitfield message (5) from peer, but received different message: " + bitfieldMessage[0]);
        }
        System.out.println("Received bitfield message");
        byte[] piece = downloadPieceHelper(tcpService, pieceLength, index);
        return piece;
    }

}