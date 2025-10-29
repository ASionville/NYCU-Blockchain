package p2pblockchain.types;

import java.util.ArrayList;
import p2pblockchain.utils.Base64Utils;
import p2pblockchain.utils.HashUtils;
import p2pblockchain.utils.JsonArray;
import p2pblockchain.utils.JsonObject;
import p2pblockchain.utils.Logger;
import p2pblockchain.utils.TimeUtils;

/**
 * Represents a block in the blockchain.
 *
 * A Block contains a list of transactions, the hash of the previous block,
 * metadata for mining (difficulty, nonce), a timestamp, and miner reward info.
 * The class provides JSON and Base64 serialization helpers as well as methods
 * to compute content hashes used for consensus and verification.
 */
public class Block {
    private String previousHash;
    private String hash;

    private int miningDifficulty;
    private int nonce;
    private long timestamp;

    private ArrayList<Transaction> transactions;
    private MerkleTree merkleTree;

    private String minerAddress;
    private double minerRewards;

    /**
     * Create an empty/default block.
     */
    public Block() {
        this.previousHash = "";
        this.hash = "";
        this.miningDifficulty = 0;
        this.nonce = 0;
        this.timestamp = 0L;
        this.minerAddress = "";
        this.minerRewards = 0.0;
        this.transactions = new ArrayList<Transaction>();
        this.merkleTree = new MerkleTree(this.transactions);
    }

    /**
     * Create a fully populated block instance.
     *
     * @param previousHash     The hash of the previous block
     * @param hash             The block's own hash (may be empty until mined)
     * @param miningDifficulty Difficulty used for mining
     * @param nonce            Nonce used to meet difficulty
     * @param timestamp        Block timestamp (if 0, current time is used)
     * @param transactions     List of transactions included in the block
     * @param minerAddress     Address that mined the block
     * @param minerRewards     Rewards paid to the miner
     */
    public Block(
            String previousHash,
            String hash,
            int miningDifficulty,
            int nonce,
            long timestamp,
            ArrayList<Transaction> transactions,
            String minerAddress,
            double minerRewards
    ) {
        this.previousHash = previousHash;
        this.hash = hash;
        this.miningDifficulty = miningDifficulty;
        this.nonce = nonce;
        
        if (timestamp == 0L) {
            this.timestamp = TimeUtils.getNowAsLong();
        } else {
            this.timestamp = timestamp;
        }
        
        this.transactions = transactions;
        this.merkleTree = new MerkleTree(transactions);
        
        this.minerAddress = minerAddress;
        this.minerRewards = minerRewards;
    }

    /**
     * Build a Block by decoding a Base64-encoded JSON representation.
     *
     * @param blockInBase64 Base64(JSON(block))
     */
    public Block(String blockInBase64) {
        this.transactions = new ArrayList<Transaction>();
        this.fromBase64(blockInBase64);
    }

    public String getPreviousHash() {return previousHash;}
    public String getHash() {return hash;}
    public int getMiningDifficulty() {return miningDifficulty;}
    public int getNonce() {return nonce;}
    public long getTimestamp() {return timestamp;}
    public ArrayList<Transaction> getTransactions() {return transactions;}
    public String getMerkleRoot() {return merkleTree.getMerkleRoot();}
    public String getMinerAddress() {return minerAddress;}
    public double getMinerRewards() {return minerRewards;}

    public void setPreviousHash(String previousHash) {this.previousHash = previousHash;}
    public void setHash(String hash) {this.hash = hash;}
    public void setMiningDifficulty(int miningDifficulty) {this.miningDifficulty = miningDifficulty;}
    public void setNonce(int nonce) {this.nonce = nonce;}
    public void setTimestamp(long timestamp) {this.timestamp = timestamp;}
    public void setMinerAddress(String minerAddress) {this.minerAddress = minerAddress;}
    public void setMinerRewards(double minerRewards) {this.minerRewards = minerRewards;}

    /**
     * Add a transaction to this block if not already present (by hash).
     * Rebuilds the internal Merkle tree after adding.
     *
     * @param newTransaction Transaction to add
     * @return true when the transaction was added, false if it was a duplicate
     */
    public boolean addTransaction(Transaction newTransaction) {
        for (Transaction transaction : this.transactions) {
            if (transaction.toHash().contentEquals(newTransaction.toHash())) {
                Logger.warn("Block.addTransaction: duplicate transaction detected: " + newTransaction.toString());
                return false;
            }
        }
        this.transactions.addLast(newTransaction);
        this.merkleTree = new MerkleTree(this.transactions);
        return true;
    }

    /**
     * Remove a transaction by index.
     *
     * @param transactionIndex Index of transaction to remove
     * @return true if removed, false on invalid index
     */
    public boolean removeTransaction(int transactionIndex) {
        if (transactionIndex < 0 || transactionIndex >= this.transactions.size()) {
            Logger.warn("Block.removeTransaction: invalid transaction index: " + transactionIndex);
            return false;
        }
        this.transactions.remove(transactionIndex);
        this.merkleTree = new MerkleTree(this.transactions);
        return true;
    }

    /**
     * Remove a transaction matching the provided transaction (by hash).
     *
     * @param transactionToRemove Transaction to remove
     * @return true if removed, false if not found
     */
    public boolean removeTransaction(Transaction transactionToRemove) {
        boolean removed = this.transactions.removeIf(transaction -> transaction.toHash().equals(transactionToRemove.toHash()));
        if (removed) {
            this.merkleTree = new MerkleTree(this.transactions);
        } else {
            Logger.warn("Block.removeTransaction: transaction not found: " + transactionToRemove.toString());
        }
        return removed;
    }

    /**
     * Convert this block to a string representation.
     *
     * @return string representation of the block
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Block{\n");
        sb.append("  previousHash='").append(previousHash).append("',\n");
        sb.append("  hash='").append(hash).append("',\n");
        sb.append("  miningDifficulty=").append(miningDifficulty).append(",\n");
        sb.append("  nonce=").append(nonce).append(",\n");
        sb.append("  timestamp=").append(timestamp).append(",\n");
        sb.append("  minerAddress='").append(minerAddress).append("',\n");
        sb.append("  minerRewards=").append(minerRewards).append(",\n");
        sb.append("  transactions=[\n");
        for (Transaction transaction : transactions) {
            sb.append("    ").append(transaction.toString()).append(",\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert this block's content (excluding hash) to a string representation.
     *
     * @return string representation of the block content
     */
    public String contentToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Block{\n");
        sb.append("  previousHash='").append(previousHash).append("',\n");
        sb.append("  miningDifficulty=").append(miningDifficulty).append(",\n");
        sb.append("  nonce=").append(nonce).append(",\n");
        sb.append("  timestamp=").append(timestamp).append(",\n");
        sb.append("  minerAddress='").append(minerAddress).append("',\n");
        sb.append("  minerRewards=").append(minerRewards).append(",\n");
        sb.append("  transactions=[\n");
        for (Transaction transaction : transactions) {
            sb.append("    ").append(transaction.toString()).append(",\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    public String toBase64() {
        return Base64Utils.encodeToString(this.toJson().toString());
    }

    public String contentToBase64() {
        return Base64Utils.encodeToString(this.contentToJson().toString());
    }

    public boolean fromBase64(String blockInBase64) {
        String jsonStr = Base64Utils.decodeToString(blockInBase64);
        try {
            return this.fromJson(new JsonObject(jsonStr));
        } catch (Exception e) {
            Logger.error("Block.fromBase64: failed to parse Base64 JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Convert this block to a JSON representation.
     *
     * @return JSON representation of the block
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("previousHash", this.previousHash);
        json.put("hash", this.hash);
        json.put("miningDifficulty", this.miningDifficulty);
        json.put("nonce", this.nonce);
        json.put("timestamp", this.timestamp);
        json.put("minerAddress", this.minerAddress);
        json.put("minerRewards", this.minerRewards);

        JsonArray transactionsArray = new JsonArray();
        for (Transaction transaction : this.transactions) {
            transactionsArray.add(transaction.toJson());
        }
        json.put("transactions", transactionsArray);
        return json;
    }

    /**
     * Convert this block's content (excluding hash) to a JSON representation.
     *
     * @return JSON representation of the block content
     */
    public JsonObject contentToJson() {
        JsonObject json = new JsonObject();
        json.put("previousHash", this.previousHash);
        json.put("miningDifficulty", this.miningDifficulty);
        json.put("nonce", this.nonce);
        json.put("timestamp", this.timestamp);
        json.put("minerAddress", this.minerAddress);
        json.put("minerRewards", this.minerRewards);

        JsonArray transactionsArray = new JsonArray();
        for (Transaction transaction : this.transactions) {
            transactionsArray.add(transaction.contentToJson());
        }
        json.put("transactions", transactionsArray);
        return json;
    }

    /**
     * Populate this block from a JSON representation.
     *
     * @param json JSON representation of the block
     * @return true if parsing succeeded, false otherwise
     */
    public boolean fromJson(JsonObject json) {
        try {
            this.previousHash = json.getString("previousHash");
            this.hash = json.getString("hash");
            this.miningDifficulty = json.getInt("miningDifficulty");
            this.nonce = json.getInt("nonce");
            this.timestamp = json.getLong("timestamp");
            this.minerAddress = json.getString("minerAddress");
            this.minerRewards = json.getDouble("minerRewards");

            this.transactions.clear();
            JsonArray transactionsArray = json.getJsonArray("transactions");
            for (int i = 0; i < transactionsArray.size(); i++) {
                JsonObject transactionJson = transactionsArray.getJsonObject(i);
                Transaction transaction = new Transaction();
                transaction.fromJson(transactionJson);
                this.transactions.add(transaction);
            }
            this.merkleTree = new MerkleTree(this.transactions);
            return true;
        } catch (Exception e) {
            Logger.error("Block.fromJson: failed to parse JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Populate this block's content (excluding hash) from a JSON representation.
     *
     * @param json JSON representation of the block content
     * @return true if parsing succeeded, false otherwise
     */
    public boolean fromContentJson(JsonObject json) {
        try {
            this.previousHash = json.getString("previousHash");
            this.miningDifficulty = json.getInt("miningDifficulty");
            this.nonce = json.getInt("nonce");
            this.timestamp = json.getLong("timestamp");
            this.minerAddress = json.getString("minerAddress");
            this.minerRewards = json.getDouble("minerRewards");

            this.transactions.clear();
            JsonArray transactionsArray = json.getJsonArray("transactions");
            for (int i = 0; i < transactionsArray.size(); i++) {
                JsonObject transactionJson = transactionsArray.getJsonObject(i);
                Transaction transaction = new Transaction();
                transaction.fromJson(transactionJson);
                this.transactions.add(transaction);
            }
            this.merkleTree = new MerkleTree(this.transactions);
            return true;
        } catch (Exception e) {
            Logger.error("Block.fromContentJson: failed to parse JSON: " + e.getMessage());
            return false;
        }
    }

    public String toHash() {
        return HashUtils.hashString(this.toBase64());
    }

    public String contentToHash() {
        return HashUtils.hashString(this.contentToBase64());
    }
}