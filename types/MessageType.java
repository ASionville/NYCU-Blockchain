package p2pblockchain.types;

/**
 * Defines message types used in P2PBlockchain network communication.
 *
 * Each message type corresponds to a specific command or request that can be
 * sent between nodes in the network. The message types are categorized into
 * local commands (handled within the node) and inter-machine commands
 * (exchanged between different nodes).
 */
public class MessageType {

    // Local Commands

    public static String GET_BALANCE = "getBalance";
    // Expected: getBalance, b64(address)
    // Response: b64(getBalance(address))

    public static String DO_TRANSACT = "doTransact";
    // Expected: doTransact, b64(transaction.toBase64)
    // Response: b64(Ok), b64(Error)
    public static String GET_CLONE_CHAIN_FROM = "getCloneChainFrom";
    // Expected: getCloneChainFrom, b64(networkNode.toBase64)
    // Response: b64(Ok), b64(Error)
    
    public static String JOIN_NETWORK = "joinNetwork";
    // Expected: joinNetwork, b64(networknode.toBase64)
    // Response: b64(Ok), b64(Dup)

    public static String LEAVE_NETWORK = "leaveNetwork";
    // Expected: leaveNetwork, b64(networkNode.toBase64)
    // Response: b64(Ok)

    public static String MINE_START = "startMining";
    // Expected: startMining
    // Response: b64(Ok)
    
    public static String MINE_STOP = "stopMining";
    // Expected: stopMining
    // Response: b64(Ok)


    // Inter-machine Commands


    public static String BCAST_BLOCK = "broadcastedBlock";
    // Expected: broadcastedBlock, b64(block.toBase64)
    // Response: b64(Ok), b64(Duplicate)
    
    public static String BCAST_TRANSACT = "broadcastedTransaction";
    // Expected: broadcastedTransaction, b64(transaction.toBase64)
    // Response: b64(Ok), b64(Duplicate)
    
    public static String BCAST_NEWNODE = "broadcastedNewNode";
    // Expected: broadcastedNewNode, b64(networkNode.toBase64)
    // Response: b64(Ok), b64(Duplicate)
    
    public static String CLONE_CHAIN = "cloneBlockchain";
    // Expected: cloneBlockchain
    // Response: b64(blockchain.toBase64())

    public static String GET_LOCAL_WALLETS = "getLocalWallets";
    // Expected: getLocalWallets
    // Response: b64(JSON array of wallet info)

}
