package blockchain.cli;

import blockchain.block.Block;
import blockchain.block.BlockChain;
import blockchain.block.BlockchainIterator;
import blockchain.pow.ProofOfWork;
import blockchain.transaction.TXOutput;
import blockchain.transaction.Transaction;
import blockchain.transaction.UTXOSet;
import blockchain.utils.Base58Check;
import blockchain.utils.LogUtil;
import blockchain.utils.RocksDBUtil;
import blockchain.wallet.Wallet;
import blockchain.wallet.WalletUtil;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.Set;

/**
 * 命令行解析类
 */
public class CLI {
    private String[] args;
    private Options options = new Options();

    public CLI(String[] args) {
        this.args = args;

        Option helpCmd = Option.builder("h").desc("show help").build();
        options.addOption(helpCmd);

        Option address = Option.builder("address").hasArg(true).desc("Source wallet address").build();
        Option sendFrom = Option.builder("from").hasArg(true).desc("Source wallet address").build();
        Option sendTo = Option.builder("to").hasArg(true).desc("Destination wallet address").build();
        Option sendAmount = Option.builder("amount").hasArg(true).desc("Amount to send").build();

        options.addOption(address);
        options.addOption(sendFrom);
        options.addOption(sendTo);
        options.addOption(sendAmount);
    }

    /**
     * 命令行解析入口
     */
    public void parse() {
        this.validateArgs(args);
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            switch (args[0]) {
                case "createblockchain":
                    String createblockchainAddress = cmd.getOptionValue("address");
                    if (StringUtils.isBlank(createblockchainAddress)) {
                        help();
                    }
                    this.createBlockchain(createblockchainAddress);
                    break;
                case "getbalance":
                    String getBalanceAddress = cmd.getOptionValue("address");
                    if (StringUtils.isBlank(getBalanceAddress)) {
                        help();
                    }
                    this.getBalance(getBalanceAddress);
                    break;
                case "send":
                    String sendFrom = cmd.getOptionValue("from");
                    String sendTo = cmd.getOptionValue("to");
                    String sendAmount = cmd.getOptionValue("amount");
                    if (StringUtils.isBlank(sendFrom) ||
                            StringUtils.isBlank(sendTo) ||
                            !NumberUtils.isDigits(sendAmount)) {
                        help();
                    }
                    this.send(sendFrom, sendTo, Integer.valueOf(sendAmount));
                    break;
                case "createwallet":
                    this.createWallet();
                    break;
                case "printaddresses":
                    this.printAddresses();
                    break;
                case "printchain":
                    this.printChain();
                    break;
                case "h":
                    this.help();
                    break;
                default:
                    this.help();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RocksDBUtil.getInstance().closeDB();
        }
    }

    /**
     * 创建区块链
     *
     * @param address
     */
    private void createBlockchain(String address) {
        BlockChain blockchain = BlockChain.newBlockchain(address);
        UTXOSet utxoSet = new UTXOSet(blockchain);
        utxoSet.reIndex();
        LogUtil.d("Done ! ");
    }


    /**
     * 验证入参
     *
     * @param args
     */
    private void validateArgs(String[] args) {
        if (args == null || args.length < 1) {
            help();
        }
    }

    /**
     * 打印帮助信息
     */
    private void help() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("Main", options);
        System.exit(0);
    }

    /**
     * 打印出区块链中的所有区块
     */
    private void printChain() throws Exception {
        BlockChain blockchain = BlockChain.initBlockchainFromDB();
        for (BlockchainIterator iterator = blockchain.getBlockchainIterator(); iterator.hashNext(); ) {
            Block block = iterator.next();
            if (block != null) {
                boolean validate = ProofOfWork.newProofOfWork(block).validate();
                LogUtil.d(block.toString() + ", validate = " + validate);
            }
        }
    }

    /**
     * 查询钱包余额
     *
     * @param address 钱包地址
     */
    private void getBalance(String address) throws Exception {
        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(address);
        } catch (Exception e) {
            throw new Exception("ERROR: invalid wallet address");
        }
        BlockChain blockchain = BlockChain.newBlockchain(address);
        // 得到公钥Hash值
        byte[] versionedPayload = Base58Check.base58ToBytes(address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);

        UTXOSet utxoSet = new UTXOSet(blockchain);

        TXOutput[] txOutputs = utxoSet.findUTXOs(pubKeyHash);
        int balance = 0;
        if (txOutputs != null && txOutputs.length > 0) {
            for (TXOutput txOutput : txOutputs) {
                balance += txOutput.getValue();
            }
        }
        LogUtil.d("Balance of address:" + address + " balance:" + balance);
    }

    /**
     * 转账
     *
     * @param from
     * @param to
     * @param amount
     */
    private void send(String from, String to, int amount) throws Exception {
        BlockChain blockchain = BlockChain.newBlockchain(from);
        Transaction transaction = Transaction.newUTXOTransaction(from, to, amount, blockchain);
        // 奖励
        Transaction rewardTx = Transaction.newCoinbaseTX(from, "");
        Block newBlock = blockchain.mineBlock(new Transaction[]{transaction, rewardTx});
        new UTXOSet(blockchain).update(newBlock);
        RocksDBUtil.getInstance().closeDB();
        LogUtil.d("Success!");
    }

    /**
     * 创建钱包
     *
     * @throws Exception
     */
    private void createWallet() throws Exception {
        Wallet wallet = WalletUtil.getInstance().createWallet();
        LogUtil.d("wallet address : " + wallet.getAddress());
    }

    /**
     * 打印钱包地址
     *
     * @throws Exception
     */
    private void printAddresses() throws Exception {
        Set<String> addresses = WalletUtil.getInstance().getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            LogUtil.d("There isn't address");
            return;
        }
        for (String address : addresses) {
            LogUtil.d("Wallet address: " + address);
        }
    }

}
