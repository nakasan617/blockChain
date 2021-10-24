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
import java.util.Scanner;
import java.util.Arrays;

class WorkB {
  
    public WorkB ()
    {
        //System.out.println("in the constructor...");
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
    //static String someText = "one two three";
    static String randString;
  
    public static String doWork(String data) throws Exception 
    {
        String concatString = "";  // Random seed string concatenated with the existing data
        String stringOut = ""; // Will contain the new SHA256 string converted to HEX and printable.

        Scanner ourInput = new Scanner(System.in);
        //System.out.print("Enter some blockdata: ");
        //String stringIn = ourInput.nextLine();
        System.out.println("data: " + data);
        String stringIn = data; 

        randString = randomAlphaNumeric(8);
        //System.out.println("Our example random seed string is: " + randString + "\n");
        //System.out.println("Concatenated with the \"data\": " + stringIn + randString + "\n");

        //System.out.println("Number will be between 0000 (0) and FFFF (65535)\n");
        int workNumber = 0;     // Number will be between 0000 (0) and FFFF (65535), here's proof:
        workNumber = Integer.parseInt("0000",16); // Lowest hex value
        //System.out.println("0x0000 = " + workNumber);

        workNumber = Integer.parseInt("FFFF",16); // Highest hex value
        //System.out.println("0xFFFF = " + workNumber + "\n");

        try {

            while(true){ // Limit how long we try for this example.
                randString = randomAlphaNumeric(8); // Get a new random AlphaNumeric seed string
                concatString = stringIn + randString; // Concatenate with our input string (which represents Blockdata)
                MessageDigest MD = MessageDigest.getInstance("SHA-256");
                byte[] bytesHash = MD.digest(concatString.getBytes("UTF-8")); // Get the hash value
            
                stringOut = ByteArrayToString(bytesHash); // Turn into a string of hex values, java 1.9 
                //System.out.println("Hash is: " + stringOut); 

                workNumber = Integer.parseInt(stringOut.substring(0,4),16); // Between 0000 (0) and FFFF (65535)
                //System.out.println("First 16 bits in Hex and Decimal: " + stringOut.substring(0,4) +" and " + workNumber);

                if (!(workNumber < 20000)){  
                    //System.out.format("%d is not less than 20,000 so we did not solve the puzzle\n\n", workNumber);
                }
                if (workNumber < 20000){
                    System.out.format("%d IS less than 20,000 so puzzle solved!\n", workNumber);
                    System.out.println("The seed (puzzle answer) was: " + randString);
                    return randString;
                }
                Thread.sleep(2000);
            }
        }catch(Exception ex) {ex.printStackTrace();}
        return randString;

    }
}

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
        WorkB work = new WorkB();
        try {
            br.WinningHash = work.doWork(br.data);
        } catch (Exception e) {}
        return br.WinningHash;
        /*
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
        */
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
        //verifyAll();
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
}


