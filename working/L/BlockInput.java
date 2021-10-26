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
//
// // Produces a 64-bye string representing 256 bits of the hash output. 4 bits per character
import java.security.MessageDigest; // To produce the SHA-256 hash.

import java.util.Date;
import java.util.UUID;
//import java.util.Scanner;
import java.util.Arrays;
import java.util.Random;
import java.text.*;
import java.util.Base64;

import java.io.StringWriter;
import java.io.StringReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    public static int unverifiedBlockPortBase = 4710;
    public static int publicKeyPortBase = 4820;
    public static int blockChainPortBase = 4930;

    public static int unverifiedBlockPort;
    public static int publicKeyPort;
    public static int blockChainPort;

    public void setPorts()
    {
        unverifiedBlockPort = unverifiedBlockPortBase + BlockChain.pnum;
        publicKeyPort = publicKeyPortBase + BlockChain.pnum; 
        blockChainPort = blockChainPortBase + BlockChain.pnum;
    }
}

class BlockChainReceivingWorker extends Thread 
{
    Socket sock;
    BlockChainReceivingWorker (Socket s) {sock = s;}

    public void run() 
    {
        String stringBlockChain = "";
        String line;
        try 
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            //String pnum = in.readLine();
            line = in.readLine();
            while (line != null)
            {
                stringBlockChain += line + "\n";
                line = in.readLine();
            }
            sock.close();
            BlockChain blockChain = new Gson().fromJson(stringBlockChain, BlockChain.class); 
            //System.out.println("stringBlockChain: " + blockChain);
            //checkBlockChainReception(blockChain);             
            blockChain.printBlockChain();
            
        } catch (IOException x) {x.printStackTrace();}

    }

}

class BlockChainReceivingServer implements Runnable 
{
    public void run() 
    {
        int q_len = 6;
        Socket sock;
        System.out.println("BlockChain Receiving Server has started");
        System.out.println("ready to listen at Port " + Ports.blockChainPort);
        try {
            ServerSocket servsock = new ServerSocket(Ports.blockChainPort, q_len);
            while(true)
            {
                sock = servsock.accept();
                new BlockChainReceivingWorker (sock).start();
            }
        } catch (IOException ioe) {System.out.println(ioe);}
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
        System.out.println("ready to listen at Port " + Ports.publicKeyPort);
        try {
            ServerSocket servsock = new ServerSocket(Ports.publicKeyPort, q_len);
            while(true)
            {
                sock = servsock.accept();
                new publicKeyReceivingWorker (sock).start();
            }
        } catch (IOException ioe) {System.out.println(ioe);}
    }
}

class PublicKeySender
{
    static int numProcesses = 3;
    static String serverName = "localhost";
    int pnum;
    PrivateKey privateKey;
    PublicKey publicKey;

    PublicKeySender(int _pnum) throws Exception
    {
       
        KeyPair keyPair = generateKeyPair(999);
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        new Thread(new publicKeyReceivingServer()).start();

        pnum = _pnum;
    }

    public void run()
    {
        sendPublicKey(publicKey);    
    }

    public void sendPublicKey(PublicKey publicKey)
    {
        Socket sock;
        PrintStream toServer;

        byte[] bytePubKey = publicKey.getEncoded();
        String stringKey = Base64.getEncoder().encodeToString(bytePubKey);

        try{Thread.sleep(4000);} catch (Exception x) {}

        for(int i = 0; i < numProcesses; i++)
        {
            //System.out.println("From Process " + pnum + " to " + i);
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


class BlockRecord{
  //String BlockID;
  public String data;
  public String PreviousHash; // We'll copy from previous block
  public String RandomSeed; // Our guess. Ultimately our winning guess.
  public String WinningHash;
  UUID uuid; // Just to show how JSON marshals this binary data.
  public String TimeStampString;

  public BlockRecord(String ph)
  {
      data = "This is a data";
      PreviousHash = ph;
      RandomSeed = null;
      WinningHash =  null;

      Date date = new Date();
      TimeStampString = String.format("%1$s %2$tF.%2$tT", "", date);
      uuid = UUID.randomUUID();
      //System.out.println("uuid: " + uuid.toString() + "\nTime Stamp: " + TimeStampString);
  }

  //public String getBlockID() {return BlockID;}
  //public void setBlockID(String BID){this.BlockID = BID;}

  public String getPreviousHash() {return this.PreviousHash;}
  public void setPreviousHash (String PH){this.PreviousHash = PH;}
  
  public UUID getUUID() {return uuid;} // Later will show how JSON marshals as a string. Compare to BlockID.
  public void setUUID (UUID ud){this.uuid = ud;}

  public String getRandomSeed() {return RandomSeed;}
  public void setRandomSeed (String RS){this.RandomSeed = RS;}
  
  public String getWinningHash() {return WinningHash;}
  public void setWinningHash (String WH){this.WinningHash = WH;}
  
}

class Block {
    BlockRecord br;
    public Block next;

    public Block(String ph)
    {
        br = new BlockRecord(ph);
        next = null;
    }

    public String createHV()
    {
        try {
            doWork(br);
        } catch (Exception e) {}
        return br.WinningHash;
    }

    public int verifyBlock()
    {
        String checkStr;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String tmp = br.data + br.PreviousHash + br.RandomSeed;
            md.update(tmp.getBytes());
            byte byteData[] = md.digest();

            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < byteData.length; i++)
            {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            checkStr = sb.toString();
        } 
        catch (NoSuchAlgorithmException x)
        {
            System.out.println("no such algorithm exception caught");
            return 2;
        }
        
        if(checkStr.equalsIgnoreCase(br.WinningHash))
        {
            System.out.println("a block verified");
            return 0;
        }
        else
        {
            System.out.println("a block not verified!");
            System.out.println(br.WinningHash);
            System.out.println(checkStr);
            
            return 1;
        }
    }

    public static String ByteArrayToString(byte[] ba){
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for(int i=0; i < ba.length; i++){
            hex.append(String.format("%02X", ba[i]));
        }
        return hex.toString();
    }

    public static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }
 
    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
     
    public static void doWork(BlockRecord br) throws Exception 
    {
        String randString;
        String concatString = "";  
        String stringOut = ""; 

        //Scanner ourInput = new Scanner(System.in);
        System.out.println("data: " + br.data);
        String stringIn = br.data + br.PreviousHash; 

        randString = randomAlphaNumeric(8);
        int workNumber = 0;     
        workNumber = Integer.parseInt("0000",16); 
        workNumber = Integer.parseInt("FFFF",16); 

        try {

            while(true){ // Limit how long we try for this example.
                randString = randomAlphaNumeric(8); // Get a new random AlphaNumeric seed string
                concatString = stringIn + randString; // Concatenate with our input string (which represents Blockdata)
                MessageDigest MD = MessageDigest.getInstance("SHA-256");
                byte[] bytesHash = MD.digest(concatString.getBytes("UTF-8")); // Get the hash value
            
                stringOut = ByteArrayToString(bytesHash); // Turn into a string of hex values, java 1.9 

                workNumber = Integer.parseInt(stringOut.substring(0,4),16); // Between 0000 (0) and FFFF (65535)

                if (workNumber < 20000){
                    System.out.format("%d IS less than 20,000 so puzzle solved!\n", workNumber);
                    System.out.println("The seed (puzzle answer) was: " + randString);
                    br.RandomSeed = randString;
                    br.WinningHash = stringOut;
                    break;
                    //return randString;
                }
                //Thread.sleep(2000);
            }
        }catch(Exception ex) {ex.printStackTrace();}

    }
   
}

class BlockChain 
{
    public static int pnum;
    static int numProcesses = 3;
    static String serverName = "localhost";
    int count; 
    Block tail;
    Block head;
    /*
    public static void main(String argv[])
    {
        //System.out.println("hello world");
        if (argv.length > 2) System.out.println("Special functionality present\n");

        if (argv.length < 1) pnum = 0;
        else if (argv[0].equals("0")) pnum = 0;
        else if (argv[0].equals("1")) pnum = 1;
        else if (argv[0].equals("2")) pnum = 2;
        else pnum = 0;

        new Ports().setPorts();

        try {
            PublicKeySender publicKeySender = new PublicKeySender(pnum); 
            publicKeySender.run();
        } catch (Exception e) {}

        
        new Thread(new BlockChainReceivingServer()).start();
        BlockChain bc = new BlockChain(argv);
        bc.run(argv);
    }
    */
    
    public BlockChain(String argv[])
    {
        count = 0;
        tail = null;
        head = null;
    }

    public void run(String argv[])
    {
        try 
        {
            createBlockChain();
            sendBlockChain();
        } 
        catch (Exception x){};
    }

    public void createBlockChain() throws Exception 
    {
        String prevHash = "";
        Block genesis = new Block(prevHash);
        prevHash = genesis.createHV();
        appendGenesis(genesis);

        Block newBlock;
        for(int i = 1; i < 4; i++)
        {
            newBlock = new Block(prevHash);
            prevHash = newBlock.createHV();                 
            appendBlock(newBlock);
            //System.out.println(prevHash);
        }
        //System.out.println(count);
        verifyAll();
    }
    
    public void sendBlockChain() throws Exception 
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        
        Socket sock;
        PrintStream toServer;
        
        Thread.sleep(5000);

        for(int i = 0; i < numProcesses; i++)
        {
            try {
                sock = new Socket(serverName, Ports.blockChainPortBase + i);
                toServer = new PrintStream(sock.getOutputStream());
                toServer.println(json);
                toServer.flush();
                sock.close();
               
            } catch (Exception x) {
                System.out.println("block chain sending failed to Process " + i);
            }
        }
        //System.out.println(json);
    }

    public void appendGenesis(Block genesis)
    {
        assert count == 0;
        count++;
        tail = genesis;
        head = genesis;
    }

    public void appendBlock(Block newBlock)
    {
        count++;
        Block oldHead = head;
        oldHead.next = newBlock;
        head = newBlock;
    }

    // returns 0 on success, non-0 on not success
    public int verifyAll()
    {
        Block curr = tail;
        int rv;
        while(curr != null)
        {
            rv = curr.verifyBlock();
            if(rv != 0) { return rv; }
            curr = curr.next;
        }
        return 0;
    }

    public void printBlockChain()
    {
        Block curr = tail;
        while(curr != null)
        {
            System.out.println("block: ");
            System.out.println("data -> " + curr.br.data);
            System.out.println("previous hash -> " + curr.br.PreviousHash);
            System.out.println("random seed -> " + curr.br.RandomSeed);
            System.out.println("winning hash -> " + curr.br.WinningHash);
            System.out.println("timestamp -> " + curr.br.TimeStampString);
            curr = curr.next;
        }
    }
}

public class BlockInput {
    public static int pnum;
    private static String FILENAME;

    public static void main(String argv[])
    {
        BlockInput s = new BlockInput();
        s.run(argv);
    }

    public void run (String argv[])
    {
        System.out.println("Running now\n");
        try {
            ListExample(argv);
        } catch (Exception x) {}
    }

    public void ListExample(String argv[]) throws Exception
    {
        if (argv.length > 2) System.out.println("Special functionality present\n");

        if (argv.length < 1) pnum = 0;
        else if (argv[0].equals("0")) pnum = 0;
        else if (argv[0].equals("1")) pnum = 1;
        else if (argv[0].equals("2")) pnum = 2;
        else pnum = 0;

        new Ports().setPorts();


        try {
            PublicKeySender publicKeySender = new PublicKeySender(pnum); 
            publicKeySender.run();
        } catch (Exception e) {}
        
        new Thread(new BlockChainReceivingServer()).start();
        BlockChain bc = new BlockChain(argv);
        bc.run(argv);

        switch(pnum){
            case 1: FILENAME = "BlockInput1.txt"; break;
            case 2: FILENAME = "BlockInput2.txt"; break;
            default: FILENAME= "BlockInput0.txt"; break;
        }

        System.out.println("Using input file: " + FILENAME);

        Block curr;
        try {
            String InputLineStr;
            BufferedReader br = new BufferedReader(new FileReader(FILENAME));
            while((InputLineStr = br.readLine()) != null)
            {
                curr = new Block(InputLineStr, "");
            }
        } catch (Exception e) {e.printStackTrace();}
    }
}
