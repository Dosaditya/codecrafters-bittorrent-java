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
        if (!validatePieceHash(torrent.p.get(index), piece)) {
            throw new RuntimeException("Piece hash validation failed: " + index);
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

    public static byte[] downloadPieceHelper(TCPService tcpService, int pieceLength, int index) throws Exception {
        // send an interested message to the peer
        byte[] interestedMessage = new byte[]{0, 0, 0, 1, INTERESTED_MESSAGE_ID};
        tcpService.sendMessage(interestedMessage);
        byte[] unchokeMessage = tcpService.waitForMessage();
        if (unchokeMessage[0] != UNCHOKE_MESSAGE_ID) {
            throw new RuntimeException("Expected unchoke message (1) from peer, but received different message: " + unchokeMessage[0]);
        }
        System.out.println("Received unchoke message");
        int blocks = (int) Math.ceil((double) pieceLength / BLOCK_SIZE);
        int offset = 0;
        byte[] piece = new byte[pieceLength];
        for (int blockIndex = 0; blockIndex < blocks; blockIndex++) {
            int blockLength = Math.min(BLOCK_SIZE, pieceLength - offset);
            byte[] requestPayload = TCPService.createRequestPayload(index, offset, blockLength);
            tcpService.sendMessage(REQUEST_MESSAGE_ID, requestPayload);
            byte[] pieceMessage = tcpService.waitForMessage();
            if (pieceMessage[0] != PIECE_MESSAGE_ID) {
                throw new RuntimeException("Expected piece message (7) from peer,  but received different message: " + pieceMessage[0]);
            }
            System.out.println("Received piece message for block: " + blockIndex + " out of " + blocks);
            System.arraycopy(pieceMessage, 9, piece, offset, blockLength); 
            offset += blockLength;
        }
        return piece;
    }

    private static boolean validatePieceHash(String expectedPieceHash, byte[] piece) {
        String actualPieceHash = Util.calculateSHA1(piece);
        if (!expectedPieceHash.equals(actualPieceHash)) {
            System.out.println("Hash validation failed. Expected hash: " + expectedPieceHash + ", Actual hash: " + actualPieceHash);
        }
        return expectedPieceHash.equals(actualPieceHash);
    }

    
}