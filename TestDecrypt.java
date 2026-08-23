import java.util.Base64;

public class TestDecrypt {
    private static final String[] ENC_FRAG = {
        "VEhITE8=", "eWxsMSI=", "PWQtIz4=", "OSQzJCI=", "PSo7NzY=",
        "KzoxK3E=", "BQkLSS8=", "DgguAQI=", "ARAkGAE=", "A1QyGB4=",
        "we7t9+Y=", "2eX88aQ=", "x/Xy4/k=", "4/K4+vY=", "9/Cxwfo=",
        "xNHEisk=", "xc/Jwt8=", "1sCdx8s=", "zg=="
    };

    private static int deriveKey(int index) {
        int k = 0x3C + (index * 7);
        return (k & 0xFF);
    }

    private static String decryptUrl() {
        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < ENC_FRAG.length; i++) {
            String b64 = ENC_FRAG[i];
            byte[] enc = Base64.getDecoder().decode(b64);
            int key = deriveKey(i);
            for (byte b : enc) {
                sb.append((char) ((b & 0xFF) ^ key));
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String result = decryptUrl();
        System.out.println("Decrypted: " + result);
        System.out.println("Length: " + result.length());
        System.out.println("Match: " + result.equals("https://raw.githubusercontent.com/IceCloudPlux/IceCloudPlux-Website/main/_data/licenses.txt"));
    }
}