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

  public BlockRecord(String _data, String ph)
  {
      data = _data;
      PreviousHash = ph;
      RandomSeed = null;
      WinningHash = null;

      Date date = new Date();
      TimeStampString = String.format("%1$s %2$tF.%2$tT", "", date);
      uuid = UUID.randomUUID();
      System.out.println("data: " + data);
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


public class BlockInput {
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

    public void ListExample(String args[]) throws Exception
    {
        int pnum;
        int UnverifiedBlockPort;
        int BlockChainPort;

        /* CDE If you want to trigger bragging rights functionality... */
        if (args.length > 1) System.out.println("Special functionality is present \n");

        if (args.length < 1) pnum = 0;
        else if (args[0].equals("0")) pnum = 0;
        else if (args[0].equals("1")) pnum = 1;
        else if (args[0].equals("2")) pnum = 2;
        else pnum = 0; /* Default for badly formed argument */
        UnverifiedBlockPort = 4710 + pnum;
        BlockChainPort = 4820 + pnum;
        
        System.out.println("Process number: " + pnum + " Ports: " + UnverifiedBlockPort + " " + 
                   BlockChainPort + "\n");

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
