import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class TestDecrypt {
    public static void main(String[] args) throws Exception {
        byte[] AES_KEY = "A1h@L1a$hEEt2026".getBytes();
        String encrypted = "InAZmp01d/nNaWv8wcg31Q==";
        
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        byte[] decrypted = cipher.doFinal(decoded);
        String result = new String(decrypted, "UTF-8");
        
        System.out.println("Decrypted password for pwalson: [" + result + "]");
        System.out.println("Length: " + result.length());
    }
}
