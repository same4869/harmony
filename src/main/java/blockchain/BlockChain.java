package blockchain;

import blockchain.pow.ProofOfWork;
import blockchain.transaction.SpendableOutputResult;
import blockchain.transaction.TXInput;
import blockchain.transaction.TXOutput;
import blockchain.transaction.Transaction;
import blockchain.utils.ByteUtil;
import blockchain.utils.LogUtil;
import blockchain.utils.RocksDBUtil;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


public class BlockChain {
    @Getter
    private String lastBlockHash;

    private BlockChain(String lastBlockHash) {
        this.lastBlockHash = lastBlockHash;
    }

    /**
     * 打包交易，进行挖矿
     *
     * @param transactions
     */
    public void mineBlock(Transaction[] transactions) throws Exception {
        String lastBlockHash = RocksDBUtil.getInstance().getLastBlockHash();
        if (lastBlockHash == null) {
            throw new Exception("ERROR: Fail to get last block hash ! ");
        }
        Block block = Block.newBlock(transactions, lastBlockHash);
        this.addBlock(block);
    }

    /**
     * 添加区块
     *
     * @param block
     */
    public void addBlock(Block block) {
        RocksDBUtil.getInstance().putLastBlockHash(block.getHash());
        RocksDBUtil.getInstance().putBlock(block);
        this.lastBlockHash = block.getHash();
    }

    /**
     * 从 DB 从恢复区块链数据
     *
     * @return
     * @throws Exception
     */
    public static BlockChain initBlockchainFromDB() throws Exception {
        String lastBlockHash = RocksDBUtil.getInstance().getLastBlockHash();
        if (lastBlockHash == null) {
            throw new Exception("ERROR: Fail to init blockchain from db. ");
        }
        return new BlockChain(lastBlockHash);
    }

    /**
     * 创建区块链，没有创世区块的话会创建
     *
     * @return
     */
    public static BlockChain newBlockchain(String address) {
        String lastBlockHash = RocksDBUtil.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            // 创建 coinBase 交易
            Transaction coinbaseTX = Transaction.newCoinbaseTX(address, "");
            Block genesisBlock = Block.newGenesisBlock(coinbaseTX);
            lastBlockHash = genesisBlock.getHash();
            RocksDBUtil.getInstance().putBlock(genesisBlock);
            RocksDBUtil.getInstance().putLastBlockHash(lastBlockHash);
        }
        return new BlockChain(lastBlockHash);
    }

    public BlockchainIterator getBlockchainIterator() {
        return new BlockchainIterator(lastBlockHash);
    }

    /**
     * 查找钱包地址对应的所有未花费的交易
     *
     * @param address 钱包地址
     * @return
     */
    private Transaction[] findUnspentTransactions(String address) throws Exception {
        Map<String, int[]> allSpentTXOs = this.getAllSpentTXOs(address);
        Transaction[] unspentTxs = {};

        // 再次遍历所有区块中的交易输出
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();
            for (Transaction transaction : block.getTransactions()) {

                String txId = Hex.encodeHexString(transaction.getTxId());

                int[] spentOutIndexArray = allSpentTXOs.get(txId);

                for (int outIndex = 0; outIndex < transaction.getOutputs().length; outIndex++) {
                    if (spentOutIndexArray != null && ArrayUtils.contains(spentOutIndexArray, outIndex)) {
                        continue;
                    }

                    // 保存不存在 allSpentTXOs 中的交易
                    if (transaction.getOutputs()[outIndex].canBeUnlockedWith(address)) {
                        unspentTxs = ArrayUtils.add(unspentTxs, transaction);
                    }
                }
            }
        }
        return unspentTxs;
    }

    /**
     * 从交易输入中查询区块链中所有已被花费了的交易输出
     *
     * @param address 钱包地址
     * @return 交易ID以及对应的交易输出下标地址
     * @throws Exception
     */
    private Map<String, int[]> getAllSpentTXOs(String address) throws Exception {
        // 定义TxId ——> spentOutIndex[]，存储交易ID与已被花费的交易输出数组索引值
        Map<String, int[]> spentTXOs = new HashMap<>();
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();

            for (Transaction transaction : block.getTransactions()) {
                // 如果是 coinbase 交易，直接跳过，因为它不存在引用前一个区块的交易输出
                if (transaction.isCoinbase()) {
                    continue;
                }
                for (TXInput txInput : transaction.getInputs()) {
                    if (txInput.canUnlockOutputWith(address)) {
                        String inTxId = Hex.encodeHexString(txInput.getTxId());
                        int[] spentOutIndexArray = spentTXOs.get(inTxId);
                        if (spentOutIndexArray == null) {
                            spentTXOs.put(inTxId, new int[]{txInput.getTxOutputIndex()});
                        } else {
                            spentOutIndexArray = ArrayUtils.add(spentOutIndexArray, txInput.getTxOutputIndex());
                            spentTXOs.put(inTxId, spentOutIndexArray);
                        }
                    }
                }
            }
        }
        return spentTXOs;
    }

    /**
     * 查找钱包地址对应的所有UTXO
     *
     * @param address 钱包地址
     * @return
     */
    public TXOutput[] findUTXO(String address) throws Exception {
        Transaction[] unspentTxs = this.findUnspentTransactions(address);
        TXOutput[] utxos = {};
        if (unspentTxs == null || unspentTxs.length == 0) {
            return utxos;
        }
        for (Transaction tx : unspentTxs) {
            for (TXOutput txOutput : tx.getOutputs()) {
                if (txOutput.canBeUnlockedWith(address)) {
                    utxos = ArrayUtils.add(utxos, txOutput);
                }
            }
        }
        return utxos;
    }

    /**
     * 寻找能够花费的交易
     *
     * @param address 钱包地址
     * @param amount  花费金额
     */
    public SpendableOutputResult findSpendableOutputs(String address, int amount) throws Exception {
        Transaction[] unspentTXs = this.findUnspentTransactions(address);
        int accumulated = 0;
        Map<String, int[]> unspentOuts = new HashMap<>();
        for (Transaction tx : unspentTXs) {

            String txId = Hex.encodeHexString(tx.getTxId());

            for (int outId = 0; outId < tx.getOutputs().length; outId++) {

                TXOutput txOutput = tx.getOutputs()[outId];

                if (txOutput.canBeUnlockedWith(address) && accumulated < amount) {
                    accumulated += txOutput.getValue();

                    int[] outIds = unspentOuts.get(txId);
                    if (outIds == null) {
                        outIds = new int[]{outId};
                    } else {
                        outIds = ArrayUtils.add(outIds, outId);
                    }
                    unspentOuts.put(txId, outIds);
                    if (accumulated >= amount) {
                        break;
                    }
                }
            }
        }
        return new SpendableOutputResult(accumulated, unspentOuts);
    }


    /**
     * 区块链迭代器,倒序遍历
     */
    public class BlockchainIterator {
        private String currentBlockHash; //当前迭代器指向区块的hash

        public BlockchainIterator(String currentBlockHash) {
            this.currentBlockHash = currentBlockHash;
        }

        /**
         * 是否有下一个区块
         *
         * @return
         */
        public boolean hashNext() {
            if (StringUtils.isBlank(currentBlockHash) || currentBlockHash.equals(ByteUtil.ZERO_HASH)) {
                return false;
            }
            Block lastBlock = RocksDBUtil.getInstance().getBlock(currentBlockHash);
            if (lastBlock == null) {
                return false;
            }
            // 创世区块直接放行
            if (ByteUtil.ZERO_HASH.equals(lastBlock.getPrevBlockHash())) {
                return true;
            }
            return RocksDBUtil.getInstance().getBlock(lastBlock.getPrevBlockHash()) != null;
        }


        /**
         * 返回区块
         *
         * @return
         */
        public Block next() throws Exception {
            Block currentBlock = RocksDBUtil.getInstance().getBlock(currentBlockHash);
            if (currentBlock != null) {
                this.currentBlockHash = currentBlock.getPrevBlockHash();
                return currentBlock;
            }
            return null;
        }

        /**
         * 遍历打印整条区块链信息
         *
         * @param blockChain
         */
        public void printBlockChain(BlockChain blockChain) {
            for (BlockChain.BlockchainIterator iterator = blockChain.getBlockchainIterator(); iterator.hashNext(); ) {
                Block block = null;
                try {
                    block = iterator.next();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (block != null) {
                    boolean validate = ProofOfWork.newProofOfWork(block).validate();
                    LogUtil.d(block.toString() + ", validate = " + validate);
                }
            }
        }
    }

}
