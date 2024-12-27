import java.util.*;


public class Util{
  
    public static String bytesToHex(byte[] bytes){
        StringBuilder sb = new StringBuilder();
          for (byte b:bytes){
            sb.append(String.format("%02x",b));
          }
        return sb.toString();  
    }

    public static byte[] getRandomBytes(int length) {
      byte[] bytes = new byte[length];
      new Random().nextBytes(bytes);
      return bytes;
  }

  public static void print(List<String> peerList){
     for(int i=0;i<peerList.size();i++){
         System.out.println(peerList.get(i));
     }
  }




}