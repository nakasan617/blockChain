import java.io.StringWriter;
import java.io.StringReader;

/* CDE: The encryption needed for signing the hash: */

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.NoSuchAlgorithmException;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;
import java.security.spec.*;
// Ah, heck:
import java.security.*;

// Produces a 64-bye string representing 256 bits of the hash output. 4 bits per character
import java.security.MessageDigest; // To produce the SHA-256 hash.

/* CDE Some other uitilities: */

import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.text.*;
import java.util.Base64;
import java.util.Arrays;


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

        WriteJSON(publicKey);
        String stringKey = ReadJSON();
        byte[] bytePubkey = Base64.getDecoder().decode(stringKey);
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(bytePubkey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey RestoredKey = keyFactory.generatePublic(pubSpec);
        System.out.println("public key: " + publicKey);

        
    }
    
    public void WriteJSON(PublicKey publicKey)
    {
        byte[] bytePubKey = publicKey.getEncoded();
        String stringKey = Base64.getEncoder().encodeToString(bytePubKey);

        Gson gson = new GsonBuilder().create();
        try(FileWriter writer = new FileWriter("record.json"))
        {
            System.out.println("writing now");
            gson.toJson(stringKey, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String ReadJSON()
    {
        Gson gson = new Gson();
        String rv = "";
        try (Reader reader = new FileReader("record.json"))
        {
            rv = gson.fromJson(reader, String.class);
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        return rv;
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
