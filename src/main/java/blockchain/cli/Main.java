package blockchain.cli;

public class Main {
    public static void main(String[] args) {
//        String[] args1 = { "-print" };
        CLI cli = new CLI(args);
        cli.parse();
    }
}
