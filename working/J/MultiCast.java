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

/*
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
*/

import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.Reader;

/*
 * Thanks: http://www.javacodex.com/Concurrency/PriorityBlockingQueue-Example
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class Ports 
{
    public static int publicKeyPortBase = 4710;
    public static int Port;

    public void setPorts()
    {
        Port = publicKeyPortBase + MultiCast.pnum; 
    }
}

class publicKeyReceivingWorker extends Thread 
{
    Socket sock;
    publicKeyReceivingWorker (Socket s) {sock = s;}

    public void run() 
    {
        try 
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String pnum = in.readLine();
            String StringKey = in.readLine();
            sock.close();
            System.out.println("from Process " + pnum + ":\n" + StringKey);
        } catch (IOException x) {x.printStackTrace();}

    }
}

class publicKeyReceivingServer implements Runnable 
{
    public void run() 
    {
        int q_len = 6;
        Socket sock;
        System.out.println("Public Key Receiving Server has started");
        System.out.println("ready to listen at Port " + Ports.Port);
        try {
            ServerSocket servsock = new ServerSocket(Ports.Port, q_len);
            while(true)
            {
                sock = servsock.accept();
                new publicKeyReceivingWorker (sock).start();
            }
        } catch (IOException ioe) {System.out.println(ioe);}
    }
}

public class MultiCast 
{
    static String serverName = "localhost";
    static int numProcesses = 3;
    public static int pnum;
    //int UnverifiedBlockPort;
    //int BlockChainPort;
    //int port;

    public static void main(String argv[]) 
    {
        MultiCast m = new MultiCast(argv);
        try {
            m.run(argv);
        } catch (Exception x) {}
    }

    public MultiCast(String argv[])
    {
        System.out.println("In the constructor...");
        pnum = 0;
    }

    public void run(String argv[]) throws Exception
    {
        System.out.println("Running now\n");
        DemonstrateUtilities(argv);

        KeyPair keyPair = generateKeyPair(999);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();    
 
        new Thread(new publicKeyReceivingServer()).start();
        sendPublicKey(publicKey);
    }

    public void DemonstrateUtilities(String args []) throws Exception
    {
        System.out.println("=======>  In DemonstrateUtilities <======");
        /*
        int pnum;
        int UnverifiedBlockPort;
        int BlockChainPort;
        */

        if (args.length > 2) System.out.println("Special functionality present\n");

        if (args.length < 1) pnum = 0;
        else if (args[0].equals("0")) pnum = 0;
        else if (args[0].equals("1")) pnum = 1;
        else if (args[0].equals("2")) pnum = 2;
        else pnum = 0;

        new Ports().setPorts();

        //UnverifiedBlockPort = 4710 + pnum;
        //BlockChainPort = 4810 + pnum;
        //System.out.println("Hello from Process " + pnum + "\nPorts: " + UnverifiedBlockPort + " " + BlockChainPort + "\n");
        System.out.println("Hello from Process " + pnum + "\nPort: " + Ports.Port + "\n");
    }

    public void sendPublicKey(PublicKey publicKey)
    {
        Socket sock;
        PrintStream toServer;

        byte[] bytePubKey = publicKey.getEncoded();
        String stringKey = Base64.getEncoder().encodeToString(bytePubKey);

        try{Thread.sleep(10000);} catch (Exception x) {}

        for(int i = 0; i < numProcesses; i++)
        {
            System.out.println("From Process " + pnum + " to " + i);
            try {
                sock = new Socket(serverName, Ports.publicKeyPortBase + i); 
                toServer = new PrintStream(sock.getOutputStream());
                //toServer.println("Hello multicast message from Process " + pnum);
                toServer.println(pnum);
                toServer.flush();
                toServer.println(stringKey);
                toServer.flush();
                sock.close();
            } catch (Exception x) {
                System.out.println("did not work when trying to send to Process " + i);
            }
        }
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
