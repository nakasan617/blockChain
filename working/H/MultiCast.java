/*
 * Thanks: http://www.javacodex.com/Concurrency/PriorityBlockingQueue-Example
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class Ports 
{
    public static int PortBase = 4710;
    public static int Port;

    public void setPorts()
    {
        Port = PortBase + MultiCast.pnum; 
    }
}

class Worker extends Thread 
{
    Socket sock;
    Worker (Socket s) {sock = s;}

    public void run() 
    {
        try 
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String data = in.readLine();
            System.out.println(data);
            sock.close();
        } catch (IOException x) {x.printStackTrace();}

    }
}

class server implements Runnable 
{
    public void run() 
    {
        int q_len = 6;
        Socket sock;
        System.out.println("ready to listen at Port " + Ports.Port);
        try {
            ServerSocket servsock = new ServerSocket(Ports.Port, q_len);
            while(true)
            {
                sock = servsock.accept();
                new Worker (sock).start();
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
        m.run(argv);
    }

    public MultiCast(String argv[])
    {
        System.out.println("In the constructor...");
        pnum = 0;
    }

    public void run(String argv[])
    {
        System.out.println("Running now\n");
        try {
            DemonstrateUtilities(argv);
        } catch (Exception x) {};
        new Thread(new server()).start();
        sendMessage();
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

    public void sendMessage()
    {
        Socket sock;
        PrintStream toServer;
        while(true)
        {
            try{Thread.sleep(3000);} catch (Exception x) {}

            for(int i = 0; i < numProcesses; i++)
            {
                System.out.println("From Process " + pnum + " to " + i);
                try {
                    sock = new Socket(serverName, Ports.PortBase + i); 
                    toServer = new PrintStream(sock.getOutputStream());
                    toServer.println("Hello multicast message from Process " + pnum);
                    toServer.flush();
                    sock.close();
                } catch (Exception x) {
                    System.out.println("did not work when trying to send to Process " + i);
                }
            }
        }
    }
    
}
