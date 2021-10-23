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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.Reader;

class BlockRecord{
  //String BlockID;
  public String data;
  public String PreviousHash; // We'll copy from previous block
  public String RandomSeed; // Our guess. Ultimately our winning guess.
  public String WinningHash;
  UUID uuid; // Just to show how JSON marshals this binary data.
  String TimeStampString;

  public BlockRecord(String ph)
  {
      data = "This is a data";
      PreviousHash = ph;
      RandomSeed = "This is RandomSeed";
      WinningHash =  null;

      Date date = new Date();
      TimeStampString = String.format("%1$s %2$tF.%2$tT", "", date);
      uuid = UUID.randomUUID();
      System.out.println("uuid: " + uuid.toString() + "\nTime Stamp: " + TimeStampString);
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
    /*
    String data;
    String prevHashValue;
    String randString;
    public String hashValue;
    */
    public Block next;

    public Block(String ph)
    {
        /*
        data = "this is data";
        prevHashValue = prevHV;
        randString = "this is randString";
        */
        br = new BlockRecord(ph);
        next = null;
    }

    public String createHV()
    {
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
            br.WinningHash = sb.toString();
        } catch(NoSuchAlgorithmException x) {
            System.out.println("no such algorithm exception caught");
        }
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
        
        if(checkStr.equals(br.WinningHash))
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
    
}

public class BlockChain 
{
    int count; 
    Block tail;
    Block head;

    public static void main(String argv[])
    {
        //System.out.println("hello world");
        BlockChain bc = new BlockChain(argv);
        bc.run(argv);
    }
    
    public BlockChain(String argv[])
    {
        count = 0;
        tail = null;
        head = null;
        System.out.println("In the constructor...");
    }

    public void run(String argv[])
    {
        System.out.println("Running now");

        try 
        {
            DemonstrateUtilities(argv);
        } 
        catch (Exception x){};
        WriteJSON();
        ReadJSON();
    }

    public void DemonstrateUtilities(String args[]) throws Exception 
    {
        /*
         * 1. create a genesis block and make a random string
         * 2. hash those values with no prevHashValue
         * 3. add that as a tail and a head 
         * 4. create other 3 blocks and concatenate them together 
         */

        String prevHash = "";
        // 1. create a genesis block
        Block genesis = new Block(prevHash);
        prevHash = genesis.createHV();
        //System.out.println(prevHash);
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

    public void WriteJSON() 
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Block curr = tail;
        //String json;
        try( FileWriter writer = new FileWriter("blockRecord.json")) 
        {
            /*
            while(curr != null)
            {
                //json = gson.toJson(curr.br);
                //System.out.println(json);
                gson.toJson(curr.br, writer);
                curr = curr.next;            
            }
            */
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ReadJSON()
    {
        Gson gson = new Gson();
        try (Reader reader = new FileReader("blockRecord.json")) 
        {
            BlockChain bc = gson.fromJson(reader, BlockChain.class);
            String json = gson.toJson(bc);
            System.out.println(json); 

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


