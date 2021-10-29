/*

Below is from bc.java
   
http://www.javacodex.com/Concurrency/PriorityBlockingQueue-Example

-----------------------------------------------------------------------------
Below is from WorkB.java
The web sources:

https://www.quickprogrammingtips.com/java/how-to-generate-sha256-hash-in-java.html  @author JJ
https://dzone.com/articles/generate-random-alpha-numeric  by Kunal Bhatia  ·  Aug. 09, 12 · Java Zone
-----------------------------------------------------------------------------
Below is from BlockJ.java
The web sources:

https://mkyong.com/java/how-to-parse-json-with-gson/
http://www.java2s.com/Code/Java/Security/SignatureSignAndVerify.htm
https://www.mkyong.com/java/java-digital-signatures-example/ (not so clear)
https://javadigest.wordpress.com/2012/08/26/rsa-encryption-example/
https://www.programcreek.com/java-api-examples/index.php?api=java.security.SecureRandom
https://www.mkyong.com/java/java-sha-hashing-example/
https://stackoverflow.com/questions/19818550/java-retrieve-the-actual-value-of-the-public-key-from-the-keypair-object
https://www.java67.com/2014/10/how-to-pad-numbers-with-leading-zeroes-in-Java-example.html

One version of the JSON jar file here:
https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.2/

-----------------------------------------------------------------------------
Below is from BlockInputG.java
The web sources:

Reading lines and tokens from a file:
http://www.fredosaurus.com/notes-java/data/strings/96string_examples/example_stringToArray.html
Good explanation of linked lists:
https://beginnersbook.com/2013/12/linkedlist-in-java-with-example/
Priority queue:
https://www.javacodegeeks.com/2013/07/java-priority-queue-priorityqueue-example.html

*/


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
import java.security.*;
import java.security.MessageDigest; 

import java.util.Date;
import java.util.UUID;
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// this is a class for ports and something I wanted to access from outside the class such as Block Chain and public/private keys
class Ports 
{
    public static int numProcesses = 3;
    public static String serverName = "localhost";
    public static int publicKeyPortBase = 4710;
    public static int unverifiedBlockPortBase = 4820;
    public static int blockChainPortBase = 4930;
    public static boolean applicationReady = false;
    public static boolean JSONWritten = false;

    public static int unverifiedBlockPort;
    public static int publicKeyPort;
    public static int blockChainPort;

    public static BlockChain blockChain;
    public static PrivateKey privateKey;
    public static PublicKey [] publicKeys = new PublicKey[3];

    // this comparator make the Queue priority queue according to its timestamp
    public static Comparator<Block> BlockTSComparator = new Comparator<Block>()
    {
        @Override
        public int compare(Block b1, Block b2)
        {
            // basically does the c function strcmp except for some cases where strings are null, in which cases the result is explicitly stated 
            String s1 = b1.getBlockRecord().TimeStampString;
            String s2 = b2.getBlockRecord().TimeStampString;
            if(s1 == s2) {return 0;}
            if(s1 == null) {return -1;}
            if(s2 == null) {return 1;}
            return s1.compareTo(s2);
        }
    };

    // this is the priority queue
    public static Queue<Block> ourPriorityQueue = new PriorityQueue<>(13, BlockTSComparator);
    
    // this function sets the port number according to the input argument it gets
    public void setPorts()
    {
        unverifiedBlockPort = unverifiedBlockPortBase + Blockchain.pnum;
        publicKeyPort = publicKeyPortBase + Blockchain.pnum; 
        blockChainPort = blockChainPortBase + Blockchain.pnum;
    }
}

// this worker receives unverified blocks
class UnverifiedBlockReceivingWorker extends Thread 
{
    Socket sock;
    Queue<Block> priorityQueue;
    UnverifiedBlockReceivingWorker (Socket s, Queue<Block> q) {sock = s; priorityQueue = q;}

    public void run() 
    {
        String stringBlock = "";
        String line;
        try 
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String pnum = in.readLine();
            // This just takes in the lines until the line is no longer sent and concatenates
            line = in.readLine();
            while (line != null)
            {
                stringBlock += line + "\n";
                line = in.readLine();
            }
            sock.close();
            Block block = new Gson().fromJson(stringBlock, Block.class); 

            // IMPORTANT: THIS IS THE PART WHERE THE UNVERIFIED BLOCKS VERIFIES THE AUTHOR 

            // here it waits until the public key is given to the process 
            while(Ports.publicKeys[Blockchain.pnum] == null)
            {
                System.out.println("publicKeys[" + Blockchain.pnum + "] is null: sleeping");
                try {Thread.sleep(1000);} catch (Exception e) {}
            }
            
            // and then it verifies the signature 
            // each process signes the string "Process" + process number
            try {
                boolean rv = verifySig(pnum.getBytes(), Ports.publicKeys[Blockchain.pnum], block.br.Signature);
                
                if(rv == true)
                {
                    // when it is verified add it to the priority queue
                    priorityQueue.add(block);
                    System.out.println("verification completed: " + block.br.data);
                }
                else 
                {
                    System.out.println("verification for the unverified block failed");
                } 
                
                // makes it null so that you won't have to write it as a JSON file later
                block.br.Signature = null;

            } catch (Exception x) {x.printStackTrace();}
        } catch (IOException x) {x.printStackTrace();}

    }

    // this function verifies the signature, returns true when verified, otherwise false
    public static boolean verifySig(byte[] data, PublicKey key, byte[] sig) throws Exception
    {
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initVerify(key);
        signer.update(data);
        return signer.verify(sig);
    }

}

// just like other servers, creates the socket and waits
// when connection is requested from the other end, accecpts and takes in strings and converts it to necessary information (unnecessary block in this case)
class UnverifiedBlockReceivingServer implements Runnable 
{
    public void run() 
    {
        int q_len = 6;
        Socket sock;
        System.out.println("unverifiedBlock Receiving Server has started");
        //System.out.println("ready to listen at Port " + Ports.unverifiedBlockPort);
        try {
            ServerSocket servsock = new ServerSocket(Ports.unverifiedBlockPort, q_len);
            while(true)
            {
                sock = servsock.accept();
                new UnverifiedBlockReceivingWorker (sock, Ports.ourPriorityQueue).start();
            }
        } catch (IOException ioe) {System.out.println(ioe);}
    }
}

// this is the part where unverified blocks are verified
class BlockVerifier implements Runnable
{
    public void run()
    {
        System.out.println("block verifier has started");
        while(true)
        {
            try { Thread.sleep(1000); } catch (Exception e) {System.out.println("sleep failed");}
            Block curr;
            // so I was thinking is this a good idea to pull instantly when it was pushed, and it is fine because when the block is pushed so late, it should have a timestamp that is so late too, unless the network issue had screwed us up.
            if(Ports.ourPriorityQueue.isEmpty() == false) // check if empty
            {
                // pop and write the data here
                curr = Ports.ourPriorityQueue.poll();
                // if it is already included in the block chain skip the entire routine and go get the next one
                if (isIncluded(curr.br) == true)
                {
                    System.out.println("already included in blockChain: " + curr.br.data);
                    continue;
                }

                // it assumes that genesis block has to be there before referring to the blcokChain, so waiting for that
                while(Ports.blockChain.getHead() == null)
                {
                    System.out.println("blockChain head is stil null, so waiting");
                    try{ Thread.sleep(1000); } catch (Exception e) {System.out.println("sleep failed");}
                }

                // does the work and gets the hash
                curr.br.PreviousHash = Ports.blockChain.getHead().br.WinningHash;

                try {
                    doWork(curr.br);
                } catch (Exception e) {
                    System.out.println("exception caught in dowork");
                }

                // if the block is not yet included in the block chain add it to the blockchain and send it 
                // there is still a possiblity of collision here but this is something extensive so we will do it afterwards
                if(isIncluded(curr.br) == false)
                {
                    Ports.blockChain.appendBlock(curr);
                    try {
                        Ports.blockChain.sendBlockChain();
                    } catch (Exception e) {
                        System.out.println("send blockChain failed");
                    }
                }
                else
                {
                    System.out.println(curr.br.data + ": someone else solved it already");
                }

            }
        }
    }

    // return true if the BlockRecord is included, otherwise, returns false
    public static boolean isIncluded(BlockRecord br) 
    {
        Block curr = Ports.blockChain.getTail();
        while(curr != null)
        {
            if(br.BlockID.equals(curr.br.BlockID))
            {
                return true;
            }
            curr = curr.next;
        }
        return false;
    }
    
    // the 3 functions below are just copy from the code professor Elliott gave us
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

        String stringIn = br.data + br.PreviousHash; 

        randString = randomAlphaNumeric(8);
        int workNumber = 0;     
        workNumber = Integer.parseInt("0000",16); 
        workNumber = Integer.parseInt("FFFF",16); 

        try {

            while(isIncluded(br) == false) {

                randString = randomAlphaNumeric(8); 
                concatString = stringIn + randString; 
                MessageDigest MD = MessageDigest.getInstance("SHA-256");
                byte[] bytesHash = MD.digest(concatString.getBytes("UTF-8")); 
            
                stringOut = ByteArrayToString(bytesHash); 

                workNumber = Integer.parseInt(stringOut.substring(0,4),16); 

                if (workNumber < 10000){
                    //System.out.format("%d IS less than 20,000 so puzzle solved!\n", workNumber);
                    System.out.println("Puzzle solved for " + br.data);
                    //System.out.println("The seed (puzzle answer) was: " + randString);
                    br.RandomSeed = randString;
                    br.WinningHash = stringOut;
                    break;
                    
                }
                Thread.sleep(1000);
            }
        }catch(Exception ex) {ex.printStackTrace();}

    }
}

// it updates the blockChain
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
            line = in.readLine();
            while (line != null)
            {
                stringBlockChain += line + "\n";
                line = in.readLine();
            }
            sock.close();
            // this line changes the JSON string to blockchain
            BlockChain blockChain = new Gson().fromJson(stringBlockChain, BlockChain.class); 

            //System.out.println("blockChain.count :" + blockChain.getCount() + " vs. Ports.blockChain.count: " + Ports.blockChain.getCount());
            
            // it only updates when the blockChain has more length than the previous one
            if(blockChain.getCount() > Ports.blockChain.getCount())
            {
                Ports.blockChain = blockChain;
                // getting the head of the BlockChain updated because it becomes a copy from a pointer
                Ports.blockChain.updateHead();

            }

            // if the length is 13 and have not been written, the process 0 will write the BlockChain
            if(Ports.blockChain.getCount() == 13 && Ports.JSONWritten == false && Blockchain.pnum == 0)
            {
                Ports.JSONWritten = true; 
                System.out.println("writing BlockchainLedgerSample.json");
                WriteJSON(Ports.blockChain);

            }
        
        } catch (IOException x) {x.printStackTrace();}

    }

    // this function writes out the JSON format
    public void WriteJSON(BlockChain blockChain)
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try(FileWriter writer = new FileWriter("BlockchainLedgerSample.json")) {
            gson.toJson(blockChain, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

// same as other receiving server
class BlockChainReceivingServer implements Runnable 
{
    public void run() 
    {
        int q_len = 6;
        Socket sock;
        System.out.println("BlockChain Receiving Server has started");
        //System.out.println("ready to listen at Port " + Ports.blockChainPort);

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

// this receives public key in string from and changes it back to public key and keep it as a global variable
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
            if (pnum.equals("2"))
            {
                // this part notifies the other process that the process 2 is ready
                Ports.applicationReady = true;
            }
            try {
                Ports.publicKeys[Integer.parseInt(pnum)] = RestoreKey(StringKey);
            } catch (Exception x) {x.printStackTrace();}
        } catch (IOException x) {x.printStackTrace();}

    }

    // this is a copy of a function in Professor's code
    PublicKey RestoreKey(String stringKey) throws Exception
    {
        byte[] bytePubkey = Base64.getDecoder().decode(stringKey);
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(bytePubkey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey RestoredKey = keyFactory.generatePublic(pubSpec);
        return RestoredKey;
    }
}

// it's the same conventional server except it exits when it gets 3 keys
class publicKeyReceivingServer implements Runnable 
{
    int keyCount = 0;
    public void run() 
    {
        int q_len = 6;
        Socket sock;
        System.out.println("Public Key Receiving Server has started");
        //System.out.println("ready to listen at Port " + Ports.publicKeyPort);
        try {
            ServerSocket servsock = new ServerSocket(Ports.publicKeyPort, q_len);
            while(keyCount < 3)
            {
                sock = servsock.accept();
                new publicKeyReceivingWorker (sock).start();
                keyCount++;
            }
        } catch (IOException ioe) {System.out.println(ioe);}
        System.out.println("publicKeyReceivingServer exitting");
    }
}

// this class multicasts the public keys to other processes
class PublicKeySender
{
    int pnum;
    PublicKey publicKey;

    PublicKeySender(int _pnum) throws Exception
    {
       
        KeyPair keyPair = generateKeyPair(999); // creates the key pair here
        Ports.privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        pnum = _pnum;
    }

    public void run()
    {
        sendPublicKey(publicKey);    
    }

    // just sends the public key out after converting it into a string (and Base64 conversion)
    public void sendPublicKey(PublicKey publicKey)
    {
        Socket sock;
        PrintStream toServer;


        byte[] bytePubKey = publicKey.getEncoded();
        String stringKey = Base64.getEncoder().encodeToString(bytePubKey);

        //try{Thread.sleep(4000);} catch (Exception x) {}

        for(int i = 0; i < Ports.numProcesses; i++)
        {
            try {
                sock = new Socket(Ports.serverName, Ports.publicKeyPortBase + i); 
                toServer = new PrintStream(sock.getOutputStream());
                // sends the process number first
                toServer.println(pnum);
                toServer.flush();
                // then sends the key next
                toServer.println(stringKey);
                toServer.flush();
                sock.close();
            } catch (Exception x) {
                System.out.println("failed to send public key from Process " + i);
            }
        }
    }

    // this is just a copy of function Professor Elliott made 
    public static KeyPair generateKeyPair(long seed) throws Exception 
    {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
        rng.setSeed(seed);
        keyGenerator.initialize(1024, rng);
        return (keyGenerator.generateKeyPair());
    }
}


// Notice that I didn't parse the data and just made the data into "data"
// I didn't see the requirements to parse it and was too lazy so I made it this way
// It's more robust this way too
// I didn't see the point in making member private if one is going to create all the methods of get and set, so I made the members public to make it easier to read 
class BlockRecord
{
  public String BlockID;
  public String data;
  public String PreviousHash; 
  public String RandomSeed; 
  public String WinningHash;
  UUID uuid; // Just to show how JSON marshals this binary data.
  public String TimeStampString;
  public byte[] Signature; // this is signature but will be set to null when written as JSON for the aethestic reasons

  public BlockRecord(String _data, String ph)
  {
      data = _data;
      PreviousHash = ph;
      RandomSeed = null;
      WinningHash = null;

      Date date = new Date();
      TimeStampString = String.format("%1$s %2$tF.%2$tT", "", date);
      uuid = UUID.randomUUID();
      BlockID = new String(uuid.toString());
  }

}

// consider this class as a node of a linked list
// it does work for genesis block
// I can get rid of the duplicate codes later
class Block 
{
    BlockRecord br;
    public Block next;

    public Block(String data, String ph)
    {
        br = new BlockRecord(data, ph);
        next = null;
    }

    public String createHV()
    {
        try {
            doWork(br);
        } catch (Exception e) {}
        return br.WinningHash;
    }

    public BlockRecord getBlockRecord()
    {
        return br;
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

        String stringIn = br.data + br.PreviousHash; 

        randString = randomAlphaNumeric(8);
        int workNumber = 0;     
        workNumber = Integer.parseInt("0000",16); 
        workNumber = Integer.parseInt("FFFF",16); 

        try {

            while(true) {
                randString = randomAlphaNumeric(8); 
                concatString = stringIn + randString; 
                MessageDigest MD = MessageDigest.getInstance("SHA-256");
                byte[] bytesHash = MD.digest(concatString.getBytes("UTF-8")); 
            
                stringOut = ByteArrayToString(bytesHash); 

                workNumber = Integer.parseInt(stringOut.substring(0,4),16); 

                if (workNumber < 10000){
                    //System.out.format("%d IS less than 20,000 so puzzle solved!\n", workNumber);
                    //System.out.println("The seed (puzzle answer) was: " + randString);
                    br.RandomSeed = randString;
                    br.WinningHash = stringOut;
                    break;
                }
                // there is no sleep statement here because it is only used for genesis block only the process 0 solves
            }
            
        }catch(Exception ex) {ex.printStackTrace();}

    }
   
}

class BlockChain 
{
    int count; 
    Block tail;
    Block head;
   
    public BlockChain(String argv[])
    {
        count = 0;
        tail = null;
        head = null;
    }

    public Block getHead()
    {
        return head;
    }
    public int getCount()
    {
        return count;
    }
    public Block getTail()
    {
        return tail;
    }

    // only the process 0 does this 
    public void run(String argv[])
    {
        try 
        {
            createBlockChain();
            sendBlockChain();
        } 
        catch (Exception x){};
    }

    // this is necessary when the head is created as a copy after the network communication, this function resets the head in the right place
    public void updateHead() 
    {
        Block curr = tail;
        while (curr.next != null) 
        {
            curr = curr.next;
        }
        head = curr;
    }

    public void createBlockChain() throws Exception 
    {
        String prevHash = "";
        Block genesis = new Block("genesis block", prevHash);
        prevHash = genesis.createHV();
        appendBlock(genesis);

    }
    
    // send block chain in the JSON format
    public void sendBlockChain() throws Exception 
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        
        Socket sock;
        PrintStream toServer;
        
        Thread.sleep(5000);

        for(int i = 0; i < Ports.numProcesses; i++)
        {
            try {
                sock = new Socket(Ports.serverName, Ports.blockChainPortBase + i);
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

    public void appendBlock(Block newBlock)
    {
        if (count == 0) 
        {
            tail = newBlock;
            head = newBlock;
        }
        else 
        {
            Block oldHead = head;
            oldHead.next = newBlock;
            head = newBlock;
        }
        count++;
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

    // prints whole blockChain by traversing from tail
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

public class Blockchain // the other one is BlockChain <- capitalized!!!
{
    public static int pnum;
    private static String FILENAME;

    public static void main(String argv[])
    {
        Blockchain s = new Blockchain();
        s.run(argv);
    }

    public void run (String argv[])
    {
        try {
            ListExample(argv);
        } catch (Exception x) {}
    }

    public void ListExample(String argv[]) throws Exception
    {
        // basically copying from bc.java getting the process number
        if (argv.length > 2) System.out.println("Special functionality present\n");

        if (argv.length < 1) pnum = 0;
        else if (argv[0].equals("0")) pnum = 0;
        else if (argv[0].equals("1")) pnum = 1;
        else if (argv[0].equals("2")) pnum = 2;
        else pnum = 0;

        new Ports().setPorts();

        if(pnum == 0) 
        {
            System.out.println("PROCESS ZERO");
        }
        else if(pnum == 1)
        {
            System.out.println("PROCESS ONE");
        }
        else if(pnum == 2)
        {
            System.out.println("PROCESS TWO");
        }

        // creating the receiving servers first
        new Thread(new UnverifiedBlockReceivingServer()).start();
        new Thread(new BlockChainReceivingServer()).start();
        new Thread(new publicKeyReceivingServer()).start();

        // sends the public keys to each process
        try {
            PublicKeySender publicKeySender = new PublicKeySender(pnum); 
            if(pnum != 2)
            {
                while(Ports.applicationReady == false)
                {
                    System.out.println("not ready yet, going to sleep");
                    Thread.sleep(3000);
                }
            }
            publicKeySender.run();
        } catch (Exception e) {}
        
        // sends the block chain with genesis block in it
        Ports.blockChain = new BlockChain(argv); // im doing this to avoid null pointer exception
        if(pnum == 0)
        {
            // process 0 sends the blockChain with genesis block in it
            Ports.blockChain.run(argv);
        }
        
        switch(pnum){
            case 1: FILENAME = "BlockInput1.txt"; break;
            case 2: FILENAME = "BlockInput2.txt"; break;
            default: FILENAME= "BlockInput0.txt"; break;
        }

        // blockVerifier starts and waits for the priority queue to be filled
        new Thread(new BlockVerifier()).start();

        // sends all the unverified blocks read from files from each process
        Block curr;
        Date date;
        String T1;
        String TimeStampString;
        try {
            String InputLineStr;
            BufferedReader br = new BufferedReader(new FileReader(FILENAME));
            while((InputLineStr = br.readLine()) != null)
            {
                curr = new Block(InputLineStr, "");
                date = new Date(); 
                T1 = String.format("%1$s %2$tF.%2$tT", "", date);
                TimeStampString = T1 + "." + pnum;
                curr.getBlockRecord().TimeStampString = TimeStampString;                 
                sendUnverifiedBlock(curr);
                
            }
        } catch (Exception e) {e.printStackTrace();}
    }

    // sign the block and send it over the network in a JSON format
    public void sendUnverifiedBlock(Block block) throws Exception 
    {
        signBlock(block); // signs the block here
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(block);
        
        Socket sock;
        PrintStream toServer;
        
        Thread.sleep(2000);

        for(int i = 0; i < Ports.numProcesses; i++)
        {
            try {
                sock = new Socket(Ports.serverName, Ports.unverifiedBlockPortBase + i);
                toServer = new PrintStream(sock.getOutputStream());
                toServer.println("Process" + pnum);
                toServer.println(json);
                toServer.flush();
                sock.close();
               
            } catch (Exception x) {
                System.out.println("unverifiedBlock sending failed to Process " + i);
            }
        }
    }

    // signs the block with this function
    public void signBlock(Block block) throws Exception
    {
        String raw = "Process" + pnum;
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(Ports.privateKey);
        signer.update(raw.getBytes());
        block.br.Signature = signer.sign(); 
    }

}
