package mpinspector.mplearner.mqtt;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;


public class SslUtil {
	static KeyStore keyStore;
    static String keyPassword;
   /**
    * SSL Socket Factory A SSL socket factory is created and passed into this
    * class which decorates it to enable TLS 1.2 when sockets are created.
    */
   //private final SSLSocketFactory sslSocketFactory;
    
    
    
	/**************************************
	 * ssl lib a crt file and a key without keyAlgorithm
	 ******************************************/
    public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
 		    String tlsversion) throws Exception {
    	

    	 return getSocketFactory(caCrtFile,crtFile,keyFile,tlsversion,null);
    
    }

 
   public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
		   String tlsversion,String keyAlgorithm) throws Exception {
	   if (crtFile == null || keyFile == null) {
           System.out.println("Certificate or private key file missing");
           return null;
	   }
   	
   	
	   	final PrivateKey privateKey = loadPrivateKeyFromFile(keyFile, keyAlgorithm);
	   	final List<Certificate> certificates = loadCertificatesFromFile(crtFile);
	   	if (certificates == null || privateKey == null) return null;
   	

        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);

            // randomly generated key password for the key in the KeyStore
            keyPassword = new BigInteger(128, new SecureRandom()).toString(32);

            Certificate[] certChains = new Certificate[certificates.size()];
            certChains = certificates.toArray(certChains);
            keyStore.setKeyEntry("alias", privateKey, keyPassword.toCharArray(), certChains);
        } catch (IOException e) {
            System.out.println("Failed to create key store");
            
        }
   	
   		
   	 SSLContext context = SSLContext.getInstance(tlsversion);
   	 KeyManagerFactory managerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        managerFactory.init(keyStore, keyPassword.toCharArray());
        context.init(managerFactory.getKeyManagers(), null, null);
   	

       return context.getSocketFactory();
   }
   
   
   private static PrivateKey loadPrivateKeyFromFile(final String filename, final String algorithm) {
       PrivateKey privateKey = null;

       File file = new File(filename);
       if (!file.exists()) {
           System.out.println("Private key file not found: " + filename);
           return null;
       }
       try (DataInputStream stream = new DataInputStream(new FileInputStream(file))) {
           privateKey = PrivateKeyReader.getPrivateKey(stream, algorithm);
       } catch (IOException | GeneralSecurityException e) {
           System.out.println("Failed to load private key from file " + filename);
       }

       return privateKey;
   }
   
   @SuppressWarnings("unchecked")
	private static List<Certificate> loadCertificatesFromFile(final String filename) {
       File file = new File(filename);
       if (!file.exists()) {
           System.out.println("Certificate file: " + filename + " is not found.");
           return null;
       }

       try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
           final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
           return (List<Certificate>) certFactory.generateCertificates(stream);
       } catch (IOException | CertificateException e) {
           System.out.println("Failed to load certificate file " + filename);
       }
       return null;
   }


}
