package mpinspector.mplearner.coap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import com.alibaba.fastjson.JSONObject;

public class CoAPAliEncryption {
	 // identify MAC algorithm: HmacMD5 HmacSHA1, need to be same with signature
    private static final String HMAC_ALGORITHM = "hmacsha1";
    
    
    // AES algorithm
    private static final String IV = "543yhjy97ae7fyfg";   //IV offered by IoT platform document
    private static final String TRANSFORM = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "AES";
	
 // token option
    private static final int COAP2_OPTION_TOKEN = 2088;
    // seq option
    private static final int COAP2_OPTION_SEQ = 2089;

    // encryption algorithm sha256
    private static final String SHA_256 = "SHA-256";

    private static final int DIGITAL_16 = 16;
    private static final int DIGITAL_48 = 48;

    
    /**
     * authentication content
     * 
     * @param productKey 
     * @param deviceName 
     * @param deviceSecret 
     * @return authentication request
     */
    static String authBody(String productKey, String deviceName, String deviceSecret) {

        // authentication request
        JSONObject body = new JSONObject();
        body.put("productKey", productKey);
        body.put("deviceName", deviceName);
        body.put("clientId", productKey + "." + deviceName);
        body.put("timestamp", String.valueOf(System.currentTimeMillis()));
        body.put("signmethod", HMAC_ALGORITHM);
        body.put("seq", DIGITAL_16);
        body.put("sign", sign(body, deviceSecret));

        //System.out.println("----- auth body -----");
        //System.out.println(body.toJSONString());

        return body.toJSONString();
    }

    /**
     * device signature
     * 
     * @param params 
     * @param deviceSecret 
     * @return signature string, heximal
     */
    private static String sign(JSONObject params, String deviceSecret) {

        // the request paramters sorted by dictionary order
        Set<String> keys = getSortedKeys(params);

        // sign signmethod、version、resources除外
        keys.remove("sign");
        keys.remove("signmethod");
        keys.remove("version");
        keys.remove("resources");

        //  build plain signature 
        StringBuffer content = new StringBuffer();
        for (String key : keys) {
            content.append(key);
            content.append(params.getString(key));
        }

        // encrypt signature
        String sign = encrypt(content.toString(), deviceSecret);
      //  System.out.println("sign content=" + content);
      //  System.out.println("sign result=" + sign);

        return sign;
    }

    /**
     * sorted list of json objects
     * 
     * @param json JSON objects that need to be sorted
     * @return sorted list of keys
     */
    private static Set<String> getSortedKeys(JSONObject json) {
        SortedMap<String, String> map = new TreeMap<String, String>();
        for (String key : json.keySet()) {
            String vlaue = json.getString(key);
            map.put(key, vlaue);
        }
        return map.keySet();
    }

    /**
     *  HMAC_ALGORITHM encrypt
     * 
     * @param content plain text
     * @param secret key
     * @return encrypted text
     */
    private static String encrypt(String content, String secret) {
        try {
            byte[] text = content.getBytes(StandardCharsets.UTF_8);
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(key, HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(secretKey.getAlgorithm());
            mac.init(secretKey);
            return Hex.encodeHexString(mac.doFinal(text));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * SHA-256
     * 
     * @param str the packet to be encrypted
     */
    static String encod(String str) {
        MessageDigest messageDigest;
        String encdeStr = "";
        try {
            messageDigest = MessageDigest.getInstance(SHA_256);
            byte[] hash = messageDigest.digest(str.getBytes(StandardCharsets.UTF_8));
            encdeStr = Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            System.out.println(String.format("Exception@encod: str=%s;", str));
            e.printStackTrace();
            return null;
        }
        return encdeStr;
    }



    /**
     * key length = 16 bits
     */
    static byte[] encrypt(byte[] content, byte[] key) {
        return encrypt(content, key, IV);
    }

    /**
     * key length = 16 bits
     */
    static byte[] decrypt(byte[] content, byte[] key) {
        return decrypt(content, key, IV);
    }

    /**
     * aes 128 cbc key length = 16 bits
     */
    private static byte[] encrypt(byte[] content, byte[] key, String ivContent) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            IvParameterSpec iv = new IvParameterSpec(ivContent.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            return cipher.doFinal(content);
        } catch (Exception ex) {
            System.out.println(
                    String.format("AES encrypt error, %s, %s, %s", content, Hex.encodeHex(key), ex.getMessage()));
            return null;
        }
    }

    /**
     * aes 128 cbc key length = 16 bits
     */
    private static byte[] decrypt(byte[] content, byte[] key, String ivContent) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            IvParameterSpec iv = new IvParameterSpec(ivContent.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            return cipher.doFinal(content);
        } catch (Exception ex) {
            System.out.println(String.format("AES decrypt error, %s, %s, %s", Hex.encodeHex(content),
                    Hex.encodeHex(key), ex.getMessage()));
            return null;
        }
    }

	public static String getproductkey(String host) {
		// TODO Auto-generated method stub
		String tmp = host.split(".")[0];
		String productkey = tmp.split("//")[1];
		return productkey;
	}
	
	
	
	

}
