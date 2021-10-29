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

class Ports 
{
    public static int numProcesses = 3;
    public static String serverName = "localhost";
    public static int publicKeyPortBase = 4710;
    public static int unverifiedBlockPortBase = 4820;
    public static int blockChainPortBase = 4930;
    //public static int blockChainUpdated = 0;
    public static boolean applicationReady = false;
    public static boolean JSONWritten = false;

    public static int unverifiedBlockPort;
    public static int publicKeyPort;
    public static int blockChainPort;

    public static BlockChain blockChain;
    public static PrivateKey privateKey;
    public static PublicKey [] publicKeys = new PublicKey[3];

    public static Comparator<Block> BlockTSComparator = new Comparator<Block>()
    {
        @Override
        public int compare(Block b1, Block b2)
        {
            
            String s1 = b1.getBlockRecord().TimeStampString;
            String s2 = b2.getBlockRecord().TimeStampString;
            if(s1 == s2) {return 0;}
            if(s1 == null) {return -1;}
            if(s2 == null) {return 1;}
            return s1.compareTo(s2);
        }
    };

    public static Queue<Block> ourPriorityQueue = new PriorityQueue<>(13, BlockTSComparator);
    
    public void setPorts()
    {
        unverifiedBlockPort = unverifiedBlockPortBase + Blockchain.pnum;
        publicKeyPort = publicKeyPortBase + Blockchain.pnum; 
        blockChainPort = blockChainPortBase + Blockchain.pnum;
    }
}

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
            line = in.readLine();
            while (line != null)
            {
                stringBlock += line + "\n";
                line = in.readLine();
            }
            sock.close();
            Block block = new Gson().fromJson(stringBlock, Block.class); 
            // verifying the block here 
            priorityQueue.add(block);
            //System.out.println("received: " + stringBlock);
            //System.out.println("stringBlockChain: " + blockChain);
            //checkBlockChainReception(blockChain);             
        } catch (IOException x) {x.printStackTrace();}

    }

}

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

class BlockVerifier implements Runnable
{
    public void run()
    {
        System.out.println("block verifier has started");
        while(true)
        {
            try { Thread.sleep(1000); } catch (Exception e) {System.out.println("sleep failed");}
            Block curr;
            if(Ports.ourPriorityQueue.isEmpty() == false) // check if empty
            {
                // pop and write the data here
                curr = Ports.ourPriorityQueue.poll();
                if (isIncluded(curr.br) == true)
                {
                    //assert Ports.blockChainUpdated > 0;
                    //Ports.blockChainUpdated--;
                    System.out.println("already included in blockChain: " + curr.br.data);
                    continue;
                }
                //System.out.println("block received: ");
                while(Ports.blockChain.getHead() == null)
                {
                    System.out.println("blockChain head is stil null, so waiting");
                    try{ Thread.sleep(1000); } catch (Exception e) {System.out.println("sleep failed");}
                }
                curr.br.PreviousHash = Ports.blockChain.getHead().br.WinningHash;

                try {
                    doWork(curr.br);
                } catch (Exception e) {
                    System.out.println("exception caught in dowork");
                }

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

    public static boolean isIncluded(BlockRecord br) 
    {
        Block curr = Ports.blockChain.getTail();
        //System.out.println("block: " + block.br.getBlockID() + block.br.data);
        while(curr != null)
        {
            //System.out.println("curr: " + curr.br.getBlockID());
            if(br.getBlockID().equals(curr.br.getBlockID()))
            {
                return true;
            }
            curr = curr.next;
        }
        return false;
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
        //System.out.println("data: " + br.data);
        String stringIn = br.data + br.PreviousHash; 

        randString = randomAlphaNumeric(8);
        int workNumber = 0;     
        workNumber = Integer.parseInt("0000",16); 
        workNumber = Integer.parseInt("FFFF",16); 

        try {

            //while(Ports.blockChainUpdated == 0){ // Limit how long we try for this example.
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
        /*
        if( Ports.blockChainUpdated > 0)
        {
            System.out.println("blockChainUpdated: " + Ports.blockChainUpdated + "\n So quitting...");
        }
        */

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
            //System.out.println("blockChain.count :" + blockChain.getCount() + " vs. Ports.blockChain.count: " + Ports.blockChain.getCount());
            if(blockChain.getCount() > Ports.blockChain.getCount())
            {
                Ports.blockChain = blockChain;
                Ports.blockChain.updateHead();
                //System.out.println("blockChain updated:");
                //Ports.blockChainUpdated++;
                //Ports.blockChain.printBlockChain();

            }
            //System.out.println("stringBlockChain: " + blockChain);
            //checkBlockChainReception(blockChain);             
            //blockChain.printBlockChain();
            if(Ports.blockChain.getCount() == 13 && Ports.JSONWritten == false && Blockchain.pnum == 0)
            {
                Ports.JSONWritten = true; // you need a test and set here but you can just write it twice I guess
                System.out.println("writing BlockchainLedgerSample.json");
                WriteJSON(Ports.blockChain);

            }
        
        } catch (IOException x) {x.printStackTrace();}

    }

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
                Ports.applicationReady = true;
            }
            //System.out.println("from Process " + pnum + ":\n" + StringKey);
            try {
                Ports.publicKeys[Integer.parseInt(pnum)] = RestoreKey(StringKey);
            } catch (Exception x) {x.printStackTrace();}
        } catch (IOException x) {x.printStackTrace();}

    }

    PublicKey RestoreKey(String stringKey) throws Exception
    {
        byte[] bytePubkey = Base64.getDecoder().decode(stringKey);
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(bytePubkey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey RestoredKey = keyFactory.generatePublic(pubSpec);
        return RestoredKey;
    }
}

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

class PublicKeySender
{
    int pnum;
    PrivateKey privateKey;
    PublicKey publicKey;

    PublicKeySender(int _pnum) throws Exception
    {
       
        KeyPair keyPair = generateKeyPair(999);
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

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

        for(int i = 0; i < Ports.numProcesses; i++)
        {
            //System.out.println("From Process " + pnum + " to " + i);
            try {
                sock = new Socket(Ports.serverName, Ports.publicKeyPortBase + i); 
                toServer = new PrintStream(sock.getOutputStream());
                //toServer.println("Hello multicast message from Process " + pnum);
                toServer.println(pnum);
                toServer.flush();
                toServer.println(stringKey);
                toServer.flush();
                sock.close();
            } catch (Exception x) {
                System.out.println("failed to send public key from Process " + i);
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
  String BlockID;
  public String data;
  public String PreviousHash; 
  public String RandomSeed; 
  public String WinningHash;
  UUID uuid; // Just to show how JSON marshals this binary data.
  public String TimeStampString;
  public byte[] Signature;

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
      //System.out.println("data: " + data);
  }


  public String getBlockID() {return BlockID;}
  public void setBlockID(String BID){this.BlockID = BID;}

  public String getPreviousHash() {return this.PreviousHash;}
  public void setPreviousHash (String PH){this.PreviousHash = PH;}
  
  public UUID getUUID() {return uuid;} 
  public void setUUID (UUID ud){this.uuid = ud;}

  public String getRandomSeed() {return RandomSeed;}
  public void setRandomSeed (String RS){this.RandomSeed = RS;}
  
  public String getWinningHash() {return WinningHash;}
  public void setWinningHash (String WH){this.WinningHash = WH;}
  
}

class Block {
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

            //while(Ports.blockChainUpdated == 0){ 
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

    public void run(String argv[])
    {
        // I don't think you need to do this 
        try 
        {
            createBlockChain();
            sendBlockChain();
        } 
        catch (Exception x){};
    }

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

public class Blockchain { // the other one is BlockChain <- capitalized!!!
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

        new Thread(new UnverifiedBlockReceivingServer()).start();
        new Thread(new BlockChainReceivingServer()).start();
        new Thread(new publicKeyReceivingServer()).start();

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
        
        Ports.blockChain = new BlockChain(argv);
        if(pnum == 0)
        {
            Ports.blockChain.run(argv);
        }
        
        switch(pnum){
            case 1: FILENAME = "BlockInput1.txt"; break;
            case 2: FILENAME = "BlockInput2.txt"; break;
            default: FILENAME= "BlockInput0.txt"; break;
        }

        new Thread(new BlockVerifier()).start();

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

    public void sendUnverifiedBlock(Block block) throws Exception 
    {
        signBlock(block);
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

    public void signBlock(Block block) throws Exception
    {
        String raw = "Process" + pnum;
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(Ports.privateKey);
        signer.update(raw.getBytes());
        block.br.Signature = signer.sign(); 
    }

}
