
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.Reader;

class Key 
{
    PrivateKey privateKey;
    PublicKey publicKey;

    public Key()
    {
        System.out.println("in the constructor");
    }

    public static void main(String args[]) 
    {
        Key s = new Key();
        try {
            s.run(args);
        } catch (Exception x) {}
    }

    public void run(String args[]) throws Exception
    {
        System.out.println("Running now\n");
        KeyPair keyPair = generateKeyPair(999);

        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();    
        System.out.println("public key: " + publicKey);
        System.out.println("writing in a JSON format");

        WriteJSON(publicKey);
        ReadJSON();
    }
    
    public void WriteJSON(PublicKey publicKey)
    {
    }

    public void ReadJSON()
    {
    }
    
    public static KeyPair generateKeyPair(long seed) throws Exception 
    {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
        rng.setSeed(seed);
        keyGenerator.initialize(1024, rng);
        return (keyGenerator.generateKeyPair());
    }
}
