
public class MultiCast 
{
    public static void main(String argv[]) 
    {
        MultiCast m = new MultiCast(argv);
        m.run(argv);
    }

    public MultiCast(String argv[])
    {
        System.out.println("In the constructor...");
    }

    public void run(String argv[])
    {
        System.out.println("Running now\n");

        try {
            DemonstrateUtilities(argv);
        } catch (Exception x) {};
    }

    public void DemonstrateUtilities(String args []) throws Exception
    {
        System.out.println("=======>  In DemonstrateUtilities <======");
        int pnum;
        int UnverifiedBlockPort;
        int BlockChainPort;

        if (args.length > 2) System.out.println("Special functionality present\n");

        if (args.length < 1) pnum = 0;
        else if (args[0].equals("0")) pnum = 0;
        else if (args[0].equals("1")) pnum = 1;
        else if (args[0].equals("2")) pnum = 2;
        else pnum = 0;

        UnverifiedBlockPort = 4710 + pnum;
        BlockChainPort = 4810 + pnum;
        System.out.println("Hello from Process " + pnum + "\nPorts: " + UnverifiedBlockPort + " " + BlockChainPort + "\n");
    }
}
