
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
    }
}
