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


class Block {
    String data;
    String prevHashValue;
    String randString;
    public String hashValue;
    public Block next;

    public Block(String prevHV)
    {
        data = "this is data";
        prevHashValue = prevHV;
        randString = "this is randString";
        next = null;
    }

    public String createHV()
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String tmp = data + prevHashValue + randString;
            md.update(tmp.getBytes());
            byte byteData[] = md.digest();

            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < byteData.length; i++)
            {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            hashValue = sb.toString();
        } catch(NoSuchAlgorithmException x) {
            System.out.println("no such algorithm exception caught");
        }
        return hashValue;
    }

    public int verifyBlock()
    {
        String checkStr;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String tmp = data + prevHashValue + randString;
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
        
        if(checkStr.equals(hashValue))
        {
            System.out.println("a block verified");
            return 0;
        }
        else
        {
            System.out.println("a block not verified!");
            System.out.println(hashValue);
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
}


