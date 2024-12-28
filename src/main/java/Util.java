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

  public static byte[] hexStringToByteArray(String hexString) {
    byte[] byteArray = new byte[hexString.length() / 2];
    for (int i = 0; i < byteArray.length; i++) {
        int index = i * 2;
        int j = Integer.parseInt(hexString.substring(index, index + 2), 16);
        byteArray[i] = (byte) j;
    }
    return byteArray;
  }




}