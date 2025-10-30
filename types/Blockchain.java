package p2pblockchain.types;

import java.util.ArrayList;
import java.util.Comparator;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import p2pblockchain.utils.Base64Utils;
import p2pblockchain.utils.HashUtils;
import p2pblockchain.utils.Logger;
import p2pblockchain.utils.NonceGenerator;
import p2pblockchain.utils.SecurityUtils;
import p2pblockchain.utils.TimeUtils;
import p2pblockchain.utils.JsonArray;
import p2pblockchain.utils.JsonObject;

/**
 * Core Blockchain container and node state.
 *
 * This class holds the chain of blocks, pending transactions, known peer
 * nodes and mining parameters. It provides helper functions to mine blocks,
 * validate and receive blocks, serialize/deserialize state to JSON, and
 * broadcast messages to other peers.
 *
 * Note: human-readable toString() is preserved for logging. JSON
 * serialization methods (toJson()/toBase64() etc.) are intended for
 * network exchange and storage.
 */

public class Blockchain {
    private Wallet wallet;    
    private P2PNode myNode;
    private int difficulty = 0;
    private boolean mining = true;
    private ArrayList<Block> chain;
    private ArrayList<Transaction> pendingTransactions;
    private ArrayList<P2PNode> p2pNodes;

    /**
     * Get the local node identity.
     */
    public P2PNode getMyNode() {
        return this.myNode;
    }

    /**
     * Constructor to initialize the blockchain with a given wallet.
     *
     * @param walletName the wallet to be associated with the blockchain
     */
    public Blockchain(String walletName, int chosenPort) {
        myNode = new P2PNode(getLocalIPAddress(), chosenPort);
        wallet = new Wallet(walletName);
        Logger.log("Account loaded : " + wallet.getAccount());
        Logger.log("Node address : " + myNode.getNodeAddress() + ":" + myNode.getNodePort());
        difficulty = p2pblockchain.config.BlockchainConfig.INITIAL_DIFFICULTY;
        chain = new ArrayList<Block>();
        pendingTransactions = new ArrayList<Transaction>();
        p2pNodes = new ArrayList<P2PNode>();
        // Create genesis block
        createGenesisBlock();
    }
    
    /**
     * Get the local IP address of this machine (non-loopback).
     * 
     * @return Local IP address as a string, or "127.0.0.1" if not found
     */
    private static String getLocalIPAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    
                    // We want IPv4 addresses only
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Error getting local IP: " + e.getMessage());
        }
        return "127.0.0.1";
    }

    /**
     * Return a JSON representation of this blockchain suitable for serialization
     * and network exchange. This uses the project's JsonObject/JsonArray helpers
     * and contains the wallet account, difficulty, mining flag, peers, chain
     * and pending transactions.
     *
     * NOTE: Keep human-readable toString() for logs; this method is meant for
     * P2P exchange.
     *
     * @return JsonObject containing the blockchain state
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("wallet", this.wallet == null ? "" : this.wallet.getAccount());
        json.put("difficulty", this.difficulty);
        json.put("mining", this.mining);

        JsonArray nodes = new JsonArray();
        if (this.p2pNodes != null) {
            for (P2PNode node : this.p2pNodes) {
                nodes.add(node.toJson());
            }
        }
        json.put("p2pNodes", nodes);

        JsonArray chainArray = new JsonArray();
        if (this.chain != null) {
            for (Block block : this.chain) {
                chainArray.add(block.toJson());
            }
        }
        json.put("chain", chainArray);

        JsonArray pending = new JsonArray();
        if (this.pendingTransactions != null) {
            for (Transaction t : this.pendingTransactions) {
                pending.add(t.toJson());
            }
        }
        json.put("pendingTransactions", pending);

        return json;
    }

    /**
     * Create the genesis block if the chain is empty.
     */
    private void createGenesisBlock() {
        if (chain.isEmpty()) {
            Logger.log("Creating genesis block...");
            mineBlock();
            Logger.log("Genesis block created : " + chain.getFirst().toString());
        } else {
            Logger.log("Genesis block already exists.");
        }
    }

    /**
     * Mine a new block and add it to the blockchain.
     *
     * This method performs proof-of-work by iterating nonces until the
     * difficulty target is matched. When a block is successfully mined it
     * is appended to the local chain and broadcast to peers using the
     * block JSON/Base64 format.
     */
    public void mineBlock() {
        if (!this.mining) {
            Logger.log("Mining is disabled. Cannot mine new block.");
            return;
        }

        Instant startTime = Instant.now();
        String prefixZeros = new String(new char[difficulty]).replace('\0', '0');
        
        // Initialize a new block
        Block newBlock = new Block();
        newBlock.setMiningDifficulty(difficulty);
        newBlock.setMinerAddress(wallet.getAccount());
        newBlock.setMinerRewards(p2pblockchain.config.BlockchainConfig.MINING_REWARDS);

        // Set previous hash
        if (chain.isEmpty()) {
            newBlock.setPreviousHash("0");
        } else {
            newBlock.setPreviousHash(chain.getLast().getHash());
            newBlock = addTransactionsToBlock(newBlock);
        }

        // Proof of Work
        while (true) {
            newBlock.setNonce(NonceGenerator.getNonce());
            newBlock.setTimestamp(TimeUtils.getNowAsLong());

            String tempHash = HashUtils.hashString(newBlock.contentToBase64());
            if (tempHash.startsWith(prefixZeros)) {
                newBlock.setHash(tempHash);
                break;
            }
        }

        Instant endTime = Instant.now();
        Logger.log("Hash found: " + newBlock.getHash() + " (Difficulty: " + difficulty + ", Time taken: " + Duration.between(startTime, endTime).toMillis() + " ms)");

        // Check block in case another block is added while mining
        if (!chain.isEmpty()) {
            Block lastBlock = chain.getLast();
            if (!newBlock.getPreviousHash().equals(lastBlock.getHash())) {
                // Compare blocks content
                // If the new transactions are already included in the last block, discard the mined block
                ArrayList<Transaction> newBlockTransactions = newBlock.getTransactions();

                int differentFrom = 0;
                for (int i = 0; i < this.chain.size(); i++) {
                    if (this.chain.get(i).getHash().contentEquals(newBlock.getPreviousHash())) {
                        differentFrom = i;
                    }
                }

                for (int i = differentFrom; i < this.chain.size(); i++) {
                    Block duringBlock = chain.get(i);
                    for (Transaction transactionInDuringBlock : duringBlock.getTransactions()) {
                        for (Transaction transactionInNewBlock : newBlockTransactions) {
                            if (transactionInDuringBlock.toHash().contentEquals(transactionInNewBlock.toHash())) {
                                newBlockTransactions.remove(transactionInNewBlock);
                                Logger.log("Mined block contains transactions already included in the chain. Discarding mined block.");
                            }
                        }
                    }
                }

                // If there are still new transactions, add them back
                if (!newBlockTransactions.isEmpty()) {
                    for (Transaction transactionInNewBlock : newBlockTransactions) {
                        this.pendingTransactions.addLast(transactionInNewBlock);
                    }
                }

            } else {
                // No conflict, add the new block
                chain.addLast(newBlock);
                // send JSON(Base64) produced by Block.toBase64()
                this.broadcastNetworkMessage(MessageType.BCAST_BLOCK, newBlock.toBase64());
            }

        } else {
            // Chain is empty, add the new block
            chain.addLast(newBlock);
            this.broadcastNetworkMessage(MessageType.BCAST_BLOCK, newBlock.toBase64());
        }
    }

    /**
     * Start the mining process.
     */
    public void startMining() {
        this.mining = true;
        Logger.log("Mining started.");
    }

    /**
     * Stop the mining process.
     */
    public void stopMining() {
        this.mining = false;
        Logger.log("Mining stopped.");
    }

    /**
     * Serialize the full blockchain as Base64(JSON) for storage or network transmission.
     *
     * @return Base64 encoded JSON string representing the blockchain
     */
    public String toBase64() {
        return Base64Utils.encodeToString(this.toJson().toString());
    }

    /**
     * Populate the blockchain from a Base64(JSON) payload previously created
     *
     * @param blockchainAsBase64 Base64 encoded JSON representation
     * @return true on success, false on parse error
     */
    public boolean fromBase64(String blockchainAsBase64) {
        try {
            String jsonStr = Base64Utils.decodeToString(blockchainAsBase64);
            JsonObject json = new JsonObject(jsonStr);

            String walletAccount = json.getString("wallet");
            this.wallet = new Wallet(walletAccount);

            this.difficulty = json.getInt("difficulty");
            this.mining = json.getBoolean("mining");

            this.p2pNodes.clear();
            JsonArray nodes = json.getJsonArray("p2pNodes");
            for (int i = 0; i < nodes.size(); i++) {
                JsonObject njson = nodes.getJsonObject(i);
                P2PNode node = new P2PNode();
                node.fromJson(njson);
                this.p2pNodes.add(node);
            }

            this.chain.clear();
            JsonArray chainArray = json.getJsonArray("chain");
            for (int i = 0; i < chainArray.size(); i++) {
                JsonObject bj = chainArray.getJsonObject(i);
                Block b = new Block();
                b.fromJson(bj);
                this.chain.add(b);
            }

            this.pendingTransactions.clear();
            JsonArray pend = json.getJsonArray("pendingTransactions");
            for (int i = 0; i < pend.size(); i++) {
                JsonObject tj = pend.getJsonObject(i);
                Transaction t = new Transaction();
                t.fromJson(tj);
                this.pendingTransactions.add(t);
            }

            return true;
        } catch (Exception e) {
            Logger.error("Failed to decode Blockchain from Base64 JSON.");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Receive and validate a block from the network.
     *
     * The method checks for duplicates, previous-hash linkage, difficulty,
     * proof-of-work, hash correctness, Merkle root and transaction
     * signatures. If valid, the block is appended and broadcast locally.
     *
     * @param newBlock block received from peer
     * @return true if the block was accepted and added to the chain
     */
    public boolean receiveBlock(Block newBlock) {
        for (Block block : this.chain) {
            if (block.getHash().contentEquals(newBlock.getHash())) {
                Logger.log("Received block is already in the chain. Discarding.");
                return false;
            }
        }

        if (newBlock.getPreviousHash().contentEquals(this.chain.getLast().getHash())) {
            // Validate the block's difficulty against the last block's difficulty
            // The new block should have the same difficulty as the chain expects at this point
            int expectedDifficulty = this.chain.getLast().getMiningDifficulty();
            
            // Allow some flexibility: the new block's difficulty should be within ±1 of expected
            if (Math.abs(newBlock.getMiningDifficulty() - expectedDifficulty) > 1) {
                Logger.error("Received block has incompatible difficulty (expected ~" + expectedDifficulty + ", got " + newBlock.getMiningDifficulty() + "). Discarding.");
                return false;
            }

            // If the block doesn't meet the proof of work, discard it
            String prefixZeros = new String(new char[newBlock.getMiningDifficulty()]).replace('\0', '0');
            if (!newBlock.getHash().startsWith(prefixZeros)) {
                Logger.error("Received block does not meet the difficulty requirement. Discarding.");
                return false;
            }

            // If the block hash is invalid, discard it
            if (!newBlock.getHash().contentEquals(HashUtils.hashString(newBlock.contentToBase64()))) {
                Logger.error("Received block hash is invalid. Discarding.");
                return false;
            }

            // If the Merkle root is invalid, discard it
            if (!newBlock.getMerkleRoot().contentEquals(new MerkleTree(newBlock.getTransactions()).getMerkleRoot())) {
                Logger.error("Received block has invalid Merkle root. Discarding.");
                return false;
            }

            // If any transaction signature is invalid, discard it
            for (Transaction transaction : newBlock.getTransactions()) {
                if (!SecurityUtils.isSignatureValid(transaction.getSender(), transaction.contentToBase64(), transaction.getSignature())) {
                    Logger.error("Received block contains tampered transaction signature. Discarding.");
                    return false;
                }
            }

            // Remove included transactions from pending list
            for (Transaction transaction : newBlock.getTransactions()) {
                for (Transaction pendingTransaction : this.pendingTransactions) {
                    if (transaction.toHash().contentEquals(pendingTransaction.toHash())) {
                        this.pendingTransactions.remove(pendingTransaction);
                        break;
                    }
                }
            }

            Logger.log("Received valid block. Adding to chain: " + newBlock.toString());
            this.chain.addLast(newBlock);
            
            // Update local difficulty to match the received block
            this.difficulty = newBlock.getMiningDifficulty();
            
            this.broadcastNetworkMessage(MessageType.BCAST_BLOCK, newBlock.toBase64());
            return true;
            
        } else {
            Logger.error("Received block is not valid. Discarding.");
            return false;
        }
    }

    /**
     * Receive and validate a transaction from the network.
     *
     * The method checks the transaction signature, sender balance,
     * and for duplicates in pending transactions and the chain.
     * If valid, the transaction is added to the pending list and
     * broadcast locally.
     *
     * @param newTransaction transaction received from peer
     * @return true if the transaction was accepted and added to pending
     */
    public boolean receiveTransaction(Transaction newTransaction) {
        if (SecurityUtils.isSignatureValid(newTransaction.getSender(), newTransaction.contentToBase64(), newTransaction.getSignature())) {

            if ((newTransaction.getFee() + newTransaction.getAmount()) > getAccountBalance(newTransaction.getSender())) {
                Logger.error("Received transaction exceeds sender's balance. Discarding.");
                return false;
            } else {
                // Checking for duplicate transactions
                for (Transaction pendingTransaction : this.pendingTransactions) {
                    if (newTransaction.toHash().contentEquals(pendingTransaction.toHash())) {
                        Logger.error("Received duplicate transaction. Discarding.");
                        return false;
                    }
                }
                for (Block block : this.chain) {
                    for (Transaction blockTransaction : block.getTransactions()) {
                        if (newTransaction.toHash().contentEquals(blockTransaction.toHash())) {
                            Logger.error("Received transaction is already included in the chain. Discarding.");
                            return false;
                        }
                    }
                }

                pendingTransactions.add(newTransaction);
                Logger.warn("Received valid transaction. Added to pending list: " + newTransaction.toString());
                this.broadcastNetworkMessage(MessageType.BCAST_TRANSACT, newTransaction.toBase64());
                return true;
            }

        } else {
            Logger.error("Received transaction has invalid signature. Discarding.");
            return false;
        }
    }

    /**
     * Add a new peer node and broadcast it to other peers.
     *
     * @param newNode the node to add
     * @return true when node was added (false when duplicate)
     */
    public boolean receiveP2PNode(P2PNode newNode) {
        if (this.addP2PNodes(newNode)) {
            this.broadcastNetworkMessage(MessageType.BCAST_NEWNODE, newNode.toBase64(), newNode);
        } else {
            Logger.error("Failed to add duplicate P2P node: " + newNode.toString());
            return false;
        }
        return true;
    }

    /**
     * Request and clone the full blockchain from a given peer.
     *
     * This will stop local mining, clear the local chain and pending
     * transactions, and replace them with the chain received from the
     * remote node. The received payload is expected to be a Base64(JSON)
     *
     * @param node peer to request chain from
     * @return true on success, false on failure
     */
    public boolean getBlockchainFrom(P2PNode node) {
        // Stop mining before syncing blockchain
        this.mining = false;

        if (!this.chain.isEmpty()) {
            Logger.warn("Local Chain is not empty!");
            Logger.warn("Syncing blockchain will overwrite local chain.");
            this.chain.clear();
            this.pendingTransactions.clear();
        }

        Logger.log("Cloning blockchain from node " + node.toString() + " ...");

        try {
            node.connect();
            if (node.isConnected()) {
                Socket nodeSocket = node.getNodeSocket();
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
                BufferedWriter socketOutput = new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream()));

                socketOutput.write(MessageType.CLONE_CHAIN + "\n");
                socketOutput.flush();

                String encodedBlockchain = socketInput.readLine();
                // encodedBlockchain is expected to be Base64(JSON) produced by toBase64ForExchange()
                this.fromBase64OfExchange(encodedBlockchain);

                // Integrity check after cloning
                if (this.chain.size() > 2) {
                    for (int i = 0; i < this.chain.size() - 2; i++) {
                        // Check previous hash linkage
                        if (!this.chain.get(i + 1).getPreviousHash().contentEquals(this.chain.get(i).getHash())) {
                            Logger.error("Blockchain integrity check failed after cloning. Discarding cloned chain.");
                            this.chain.clear();
                            this.pendingTransactions.clear();
                            return false;
                        }

                        // Check timestamp order
                        if (this.chain.get(i + 1).getTimestamp() < this.chain.get(i).getTimestamp()) {
                            Logger.error("Blockchain integrity check failed after cloning. Discarding cloned chain.");
                            this.chain.clear();
                            this.pendingTransactions.clear();
                            return false;
                        }
                    }
                }

                socketInput.close();
                socketOutput.close();
                node.disconnect();
                Logger.log("Blockchain cloned successfully from node " + node.toString() + ". Current chain length: " + this.chain.size());
                
                // Synchronize difficulty with the cloned chain
                if (!this.chain.isEmpty()) {
                    Block lastBlock = this.chain.getLast();
                    this.difficulty = lastBlock.getMiningDifficulty();
                    Logger.log("Synchronized difficulty to " + this.difficulty + " from cloned chain.");
                }
                
                // Restart mining after successful clone
                this.mining = true;
                Logger.log("Mining restarted after blockchain sync.");
            }
            return true;

        } catch (Exception e) {
            Logger.error("Failed to clone blockchain from node " + node.toString() + ".");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fill the provided block with pending transactions up to the
     * configured maximum per block. Transactions are prioritized by fee.
     *
     * @param block block to populate
     * @return the same block instance with transactions added
     */
    private Block addTransactionsToBlock(Block block) {
        pendingTransactions.sort(Comparator.comparingDouble(p2pblockchain.types.Transaction::getFee));

        if (pendingTransactions.size() > p2pblockchain.config.BlockchainConfig.MAX_TRANSACTIONS_PER_BLOCK) {
            for (int i = 0; i < p2pblockchain.config.BlockchainConfig.MAX_TRANSACTIONS_PER_BLOCK; i++) {
                block.addTransaction(pendingTransactions.getFirst());
                pendingTransactions.removeFirst();
            }

        } else {
            int pendingTransactionsSize = pendingTransactions.size();
            for (int i = 0; i < pendingTransactionsSize; i++) {
                block.addTransaction(pendingTransactions.getFirst());
                pendingTransactions.removeFirst();
            }
        }
        return block;
    }

    /**
     * Adjust mining difficulty periodically based on observed average block
     * time compared to the target. This method is called during normal
     * operation to adapt difficulty.
     */
    public void adjustMiningDifficulty() {
        if (!this.mining) {
            return;
        }

        if (chain.size() % p2pblockchain.config.BlockchainConfig.DIFFICULTY_ADJUSTMENT_INTERVAL == 0 && chain.size() != 0) {
            long timestampStart = chain.get(chain.size() - p2pblockchain.config.BlockchainConfig.DIFFICULTY_ADJUSTMENT_INTERVAL).getTimestamp();
            long timestampEnd = chain.getLast().getTimestamp();
            long elapsedTime = ChronoUnit.SECONDS.between(
                TimeUtils.longTimestampToInstant(timestampStart),
                TimeUtils.longTimestampToInstant(timestampEnd)
            );

            double averageBlockTime = (double) elapsedTime / p2pblockchain.config.BlockchainConfig.DIFFICULTY_ADJUSTMENT_INTERVAL;
            if (averageBlockTime > (double) p2pblockchain.config.BlockchainConfig.TARGET_BLOCK_TIME_SECONDS) {
                difficulty = Math.max(1, difficulty - 1);
                Logger.log("Decreasing mining difficulty to " + difficulty + " (Average block time: " + averageBlockTime + " seconds)");
            } else {
                difficulty += 1;
                Logger.log("Increasing mining difficulty to " + difficulty + " (Average block time: " + averageBlockTime + " seconds)");
            }
        }
    }

    /**
     * Compute the balance for a given account address by scanning the
     * blockchain. Miner rewards and transaction fees are taken into
     * account.
     *
     * @param accountAddress address to compute balance for
     * @return computed account balance
     */
    public double getAccountBalance(String accountAddress) {
        double balance = 0.0;

        for (Block block : chain) {
            boolean isMiner = false;
            if (block.getMinerAddress().contentEquals(accountAddress)) {
                balance += block.getMinerRewards();
                isMiner = true;
            }

            for (Transaction transaction : block.getTransactions()) {
                if (isMiner) {
                    balance += transaction.getFee();
                }

                if (transaction.getReceiver().contentEquals(accountAddress)) {
                    balance += transaction.getAmount();
                }

                if (transaction.getSender().contentEquals(accountAddress)) {
                    balance -= (transaction.getAmount() + transaction.getFee());
                }
            }
        }
        return balance;
    }

    /**
     * Return the list of known P2P nodes.
     *
     * @return list of P2PNode
     */
    public ArrayList<P2PNode> getP2PNodes() {
        return this.p2pNodes;
    }

    /**
     * Add a single P2P node to the known peers list if not already present.
     *
     * @param newNode node to add
     * @return true when the node was added, false when duplicate
     */
    public boolean addP2PNodes(P2PNode newNode) {
        if (this.p2pNodes.contains(newNode)) {
            return false;
        }

        if (newNode == null) {
            Logger.error("Cannot add P2P node with null socket: " + newNode.toString());
            return false;
        }
        this.p2pNodes.add(newNode);
        Logger.log("Added new P2P node: " + newNode.toString());
        return true;
    }

    /**
     * Remove a P2P node from the known peers list.
     *
     * @param badNode node to remove
     * @return true when successfully removed
     */
    public boolean removeP2PNode(P2PNode badNode) {
        boolean result = this.p2pNodes.remove(badNode);
        if (result) {
            Logger.log("Removed P2P node: " + badNode.toString());
        } else {
            Logger.error("Failed to remove P2P node (not found): " + badNode.toString());
        }
        return result;
    }

    /**
     * Broadcast a leave network message to all known peers.
     */
    public void broadcastLeaveNetwork() {
        if (this.myNode == null) {
            Logger.error("Cannot broadcast leave: local node not set!");
            return;
        }
        Logger.log("Broadcasting leave network message to all peers...");
        this.broadcastNetworkMessage(MessageType.LEAVE_NETWORK, this.myNode.toBase64());
        Logger.log("Leave network message sent to all peers.");
    }

    /**
     * Check if we should exclude ourselves from broadcasts.
     */
    private boolean isMyself(P2PNode node) {
        return this.myNode != null && this.myNode.equals(node);
    }

    /**
     * Close all connections and clean up resources. Should be called during
     * graceful shutdown.
     */
    public void closeAllConnections() {
        Logger.log("Closing all peer connections...");

        // Disconnect all peer nodes
        for (P2PNode node : this.p2pNodes) {
            try {
                if (node.isConnected()) {
                    node.disconnect();
                }
            } catch (Exception e) {
                Logger.warn("Failed to disconnect from " + node.toString());
            }
        }

        Logger.log("All connections closed.");
    }

    /**
     * Send a message to all known peers. Message content is expected to be
     * a string (for example a Base64(JSON) payload). The message is written
     * as a single line: "<MessageType>, <messageContent>\n".
     *
     * @param MessageType identifier of message type
     * @param messageContent payload string
     */
    public void broadcastNetworkMessage(String MessageType, String messageContent, P2PNode... excludeNodes) {
        Logger.log("Broadcasting [" + MessageType + "] message : " + messageContent);

        // Iterate over a snapshot to avoid ConcurrentModification when
        // removing unreachable peers. Collect nodes to remove and apply
        // removals after finishing the broadcast loop.
        ArrayList<P2PNode> toRemove = new ArrayList<P2PNode>();
        ArrayList<P2PNode> snapshot = new ArrayList<P2PNode>(this.p2pNodes);

        for (P2PNode node : snapshot) {
            boolean isExcluded = false;
            for (P2PNode excludeNode : excludeNodes) {
                if (node.toHash().contentEquals(excludeNode.toHash())) {
                    Logger.log("Skipping excluded node " + node.toString() + " from broadcast.");
                    isExcluded = true;
                    break;
                }
            }

            if (isExcluded) {
                continue;
            }

            // Always try to establish a fresh connection for broadcasting
            // This ensures the socket is valid even if it was closed elsewhere
            try {
                if (node.isConnected()) {
                    node.disconnect();
                }
                if (!node.connect()) {
                    Logger.error("Cannot connect to node " + node.toString());
                    toRemove.add(node);
                    continue;
                }
            } catch (Exception e) {
                Logger.error("Failed to connect to node " + node.toString());
                toRemove.add(node);
                continue;
            }

            if (node.isNull()) {
                Logger.error("Socket is null for node " + node.toString());
                toRemove.add(node);
                continue;
            }

            BufferedReader socketInput = null;
            BufferedWriter socketOutput = null;
            try {
                socketInput = new BufferedReader(new InputStreamReader(node.getNodeSocket().getInputStream()));
                socketOutput = new BufferedWriter(new OutputStreamWriter(node.getNodeSocket().getOutputStream()));
                socketOutput.write(MessageType + ", " + messageContent + "\n");
                socketOutput.flush();

                Logger.log("Message sent to node " + node.toString());

                String response = socketInput.readLine();
                Logger.log("Response from node " + node.toString() + " : " + response);
            } catch (Exception e) {
                Logger.error("Failed to send message to node " + node.toString());
                e.printStackTrace();
                toRemove.add(node);
            } finally {
                try { if (socketInput != null) socketInput.close(); } catch (Exception ex) {}
                try { if (socketOutput != null) socketOutput.close(); } catch (Exception ex) {}
                try { node.disconnect(); } catch (Exception ex) {}
            }
        }

        // Apply removals after iterating snapshot to avoid concurrent mod.
        for (P2PNode n : toRemove) {
            try { this.p2pNodes.remove(n); Logger.log("Removed P2P node: " + n.toString()); } catch (Exception ex) { }
        }
    }

    /**
     * Human-readable string representation of the blockchain state.
     *
     * This method is intended for logging and debugging purposes.
     *
     * @return string representation of the blockchain
     */
    @Override
    public String toString() {
        StringBuilder p2pNodesString = new StringBuilder();
        StringBuilder chainString = new StringBuilder();
        StringBuilder pendingTransactionsString = new StringBuilder();

        if (this.p2pNodes != null && !this.p2pNodes.isEmpty()) {
            for (P2PNode node : this.p2pNodes) {
                p2pNodesString.append(node.toString()).append(":");
            }
            p2pNodesString.setLength(p2pNodesString.length() - 1);
        }

        if (this.chain != null && !this.chain.isEmpty()) {
            for (Block block : this.chain) {
                chainString.append(block.toString()).append(":");
            }
            if (chainString.length() > 0) chainString.setLength(chainString.length() - 1);
        }

        if (this.pendingTransactions != null && !this.pendingTransactions.isEmpty()) {
            for (Transaction transaction : this.pendingTransactions) {
                pendingTransactionsString.append(transaction.toString()).append(":");
            }
            pendingTransactionsString.setLength(pendingTransactionsString.length() - 1);
        }

        String walletId = (this.wallet == null) ? "null" : (this.wallet.getAccount() != null ? this.wallet.getAccount() : "null");

        return "Blockchain [wallet:" + walletId +
                ", difficulty:" + Integer.toString(this.difficulty) +
                ", mining:" + Boolean.toString(this.mining) +
                ", p2pNodes:" + p2pNodesString.toString() +
                ", chain:" + chainString.toString() +
                ", pendingTransactions:" + pendingTransactionsString.toString() + "]";
    }

    /**
     * Serialize only the chain for exchange with peers (Base64(JSON)).
     * The payload contains a JSON object { "chain": [ <block_json>, ... ] }.
     *
     * @return Base64 encoded JSON containing the chain
     */
    public String toBase64ForExchange() {
        JsonObject json = new JsonObject();
        JsonArray chainArray = new JsonArray();
        if (this.chain != null) {
            for (Block b : this.chain) {
                chainArray.add(b.toJson());
            }
        }
        json.put("chain", chainArray);
        return Base64Utils.encodeToString(json.toString());
    }

    /**
     * Populate only the chain from a Base64(JSON) exchange payload
     *
     * @param blockchainAsBase64 Base64 encoded JSON containing the chain
     * @return true on success, false on parse error
     */
    public boolean fromBase64OfExchange(String blockchainAsBase64) {
        try {
            String jsonStr = Base64Utils.decodeToString(blockchainAsBase64);
            JsonObject json = new JsonObject(jsonStr);
            JsonArray chainArray = json.getJsonArray("chain");
            this.chain = new ArrayList<Block>();
            for (int i = 0; i < chainArray.size(); i++) {
                JsonObject bj = chainArray.getJsonObject(i);
                Block b = new Block();
                b.fromJson(bj);
                this.chain.add(b);
            }
            return true;
        } catch (Exception e) {
            Logger.error("Failed to decode Blockchain from Base64 for exchange.");
            e.printStackTrace();
            return false;
        }
    }
}
