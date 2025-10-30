package p2pblockchain.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import p2pblockchain.types.Block;
import p2pblockchain.types.Blockchain;
import p2pblockchain.types.MessageType;
import p2pblockchain.types.P2PNode;
import p2pblockchain.types.Transaction;
import p2pblockchain.types.Wallet;
import p2pblockchain.utils.Base64Utils;
import p2pblockchain.utils.Logger;

public class startBlockchain {

    // Liste des nœuds bootstrap connus pour la découverte initiale
    private static final String[] BOOTSTRAP_NODES = {
        "localhost:8300",
        "localhost:8301",
        "localhost:8302"
    };

    public static void main(String[] args) {
        // Ask user for wallet name and network port before starting the node
        String defaultWallet = "Aubin";
        String walletName = defaultWallet;
        int chosenPort = p2pblockchain.config.NetworkConfig.getSocketPort();

        try {
            BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter wallet name (default: " + defaultWallet + "): ");
            String inputWallet = consoleIn.readLine();
            if (inputWallet != null && !inputWallet.trim().isEmpty()) {
                walletName = inputWallet.trim();
            }

            System.out.print("Enter network port (default: " + chosenPort + "): ");
            String portLine = consoleIn.readLine();
            if (portLine != null && !portLine.trim().isEmpty()) {
                try {
                    int p = Integer.parseInt(portLine.trim());
                    if (p >= 1 && p <= 65535) {
                        chosenPort = p;
                        // apply to NetworkConfig so server thread picks it up
                        p2pblockchain.config.NetworkConfig.setSocketPort(chosenPort);
                    } else {
                        System.out.println("Invalid port range. Using default: " + chosenPort);
                    }
                } catch (NumberFormatException nfe) {
                    System.out.println("Invalid port input. Using default: " + chosenPort);
                }
            }
        } catch (Exception e) {
            Logger.warn("Failed to read startup input, using defaults.");
        }

        Blockchain blockchain = new Blockchain(walletName, chosenPort);
        // Create a local Wallet instance (loads same keypair) for signing TXs
        Wallet wallet = new Wallet(walletName);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.log("Shutdown detected, notifying peers...");
            blockchain.broadcastLeaveNetwork();
            
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            
            blockchain.closeAllConnections();
            Logger.log("Graceful shutdown complete.");
        }));

        Thread networkThread = new Thread(() -> { networkServer(blockchain); });
        networkThread.setDaemon(true);
        networkThread.start();

        // Wait for network server to be ready
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Try to connect to bootstrap nodes on startup
        discoverAndJoinNetwork(blockchain, chosenPort);

        // Start console interface
        Thread consoleThread = new Thread(() -> { runConsole(blockchain, wallet); });
        consoleThread.setDaemon(true);
        consoleThread.start();

        // Miner loop (main thread)
        while (true) {
            blockchain.mineBlock();
            blockchain.adjustMiningDifficulty();
        }
    }

    /**
     * Attempts to automatically connect to bootstrap nodes to join the network.
     * Ignores the local node (same port) and tries to clone the blockchain from
     * the first active node found.
     */
    public static void discoverAndJoinNetwork(Blockchain blockchain, int myPort) {
        Logger.log("Looking for existing nodes to join...");
        P2PNode myNode = new P2PNode("localhost", myPort);
        boolean foundNode = false;
        P2PNode firstActiveNode = null;

        for (String bootstrapAddress : BOOTSTRAP_NODES) {
            // Skip self
            if (bootstrapAddress.contains(":" + myPort)) {
                continue;
            }

            try {
                String[] parts = bootstrapAddress.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                
                Logger.log("trying to connect to " + bootstrapAddress + "...");

                // Try to connect to the node
                try (Socket testSocket = new Socket(host, port);
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(testSocket.getOutputStream()));
                    BufferedReader in = new BufferedReader(new InputStreamReader(testSocket.getInputStream()))) {
                    
                    // Create a P2PNode instance for the remote node
                    P2PNode remoteNode = new P2PNode(host, port);
                    
                    // Send a Join Network request
                    out.write(MessageType.JOIN_NETWORK + ", " + myNode.toBase64() + "\n");
                    out.flush();

                    String responseB64 = in.readLine();
                    if (responseB64 != null) {
                        String response = Base64Utils.decodeToString(responseB64);
                        
                        if ("Ok".equals(response) || "Dup".equals(response)) {
                            Logger.log("Connected to node " + bootstrapAddress + " (response: " + response + ")");
                            blockchain.addP2PNodes(remoteNode);
                            foundNode = true;

                            // Save the first active node to clone the blockchain
                            if (firstActiveNode == null) {
                                firstActiveNode = remoteNode;
                            }
                        } else {
                            Logger.warn("Node " + bootstrapAddress + " refused connection: " + response);
                        }
                    }
                } catch (Exception e) {
                    // This node is not available, continue with the next
                    Logger.log("Node " + bootstrapAddress + " not available.");
                }
            } catch (Exception e) {
                Logger.warn("Error while trying to connect to " + bootstrapAddress + ": " + e.getMessage());
            }
        }

        // If we found at least one node, clone the blockchain
        if (foundNode && firstActiveNode != null) {
            Logger.log("Attempting to clone blockchain from " + firstActiveNode.toString() + "...");
            boolean cloned = blockchain.getBlockchainFrom(firstActiveNode);
            if (cloned) {
                Logger.log("Blockchain cloned successfully!");
            } else {
                Logger.warn("Failed to clone blockchain.");
            }
        } else {
            Logger.log("No existing nodes found. Starting as the first node in the network.");
        }
    }

    /**
     * Simple console REPL to interact with the local blockchain instance.
     * Supports basic commands: help, balance, mybalance, send, start, stop,
     * join, clone, listpeers, quit.
     */
    public static void runConsole(Blockchain blockchain, Wallet wallet) {
        try {
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            System.out.println("Console ready. Type 'help' for commands.");
            String line;
            while (true) {
                System.out.print("p2p> ");
                line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.length() == 0) continue;

                String[] parts = line.split(" ", 2);
                String cmd = parts[0].toLowerCase();
                String args = parts.length > 1 ? parts[1].trim() : "";

                switch (cmd) {
                    case "help":
                        System.out.println("Commands:\n  help\n  balance <address>\n  mybalance\n  send <to> <amount> <fee> [message]\n  start\n  stop\n  join <host:port>\n  clone <host:port>\n  listpeers\n  quit");
                        break;

                    case "balance":
                        if (args.isEmpty()) { System.out.println("Usage: balance <address>"); break; }
                        double bal = blockchain.getAccountBalance(args);
                        System.out.println("Balance(" + args + ") = " + bal);
                        break;

                    case "mybalance":
                        String myAddr = wallet.getAccount();
                        System.out.println("My address: " + myAddr);
                        System.out.println("Balance = " + blockchain.getAccountBalance(myAddr));
                        break;

                    case "start":
                        blockchain.startMining();
                        System.out.println("Mining started.");
                        break;

                    case "stop":
                        blockchain.stopMining();
                        System.out.println("Mining stopped.");
                        break;

                    case "join":
                        if (args.isEmpty() || !args.contains(":")) { System.out.println("Usage: join host:port"); break; }
                        {
                            String[] hp = args.split(":" );
                            String host = hp[0];
                            int port = Integer.parseInt(hp[1]);
                            P2PNode n = new P2PNode(host, port);

                            // Attempt a network Join request to the remote node first
                            try (Socket s = new Socket(host, port);
                                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                                    BufferedReader socketIn = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

                                // Send: joinNetwork, <base64(node.toJson)>
                                out.write(p2pblockchain.types.MessageType.JOIN_NETWORK + ", " + n.toBase64() + "\n");
                                out.flush();

                                String responseB64 = socketIn.readLine();
                                String response = "(no response)";
                                if (responseB64 != null) {
                                    try { response = p2pblockchain.utils.Base64Utils.decodeToString(responseB64); } catch (Exception ex) { response = responseB64; }
                                }
                                System.out.println("Remote response: " + response);

                                if ("Ok".equals(response)) {
                                    // Remote accepted the join; add locally as well
                                    boolean ok = blockchain.addP2PNodes(n);
                                    System.out.println(ok ? "Node added" : "Duplicate or failed to add locally");
                                } else if ("Dup".equals(response)) {
                                    System.out.println("Remote reports duplicate node. Not added.");
                                } else {
                                    System.out.println("Remote error: " + response + ". Not added.");
                                }

                            } catch (Exception e) {
                                System.out.println("Network join failed: " + e.getMessage());
                            }
                        }
                        break;

                    case "clone":
                        if (args.isEmpty() || !args.contains(":")) { System.out.println("Usage: clone host:port"); break; }
                        {
                            String[] hp = args.split(":" );
                            P2PNode n = new P2PNode(hp[0], Integer.parseInt(hp[1]));
                            boolean ok = blockchain.getBlockchainFrom(n);
                            System.out.println(ok ? "Chain cloned" : "Clone failed");
                        }
                        break;

                    case "listpeers":
                        for (P2PNode p : blockchain.getP2PNodes()) System.out.println(" - " + p.toString());
                        break;

                    case "send":
                        // send <to> <amount> <fee> [message]
                        if (args.isEmpty()) { System.out.println("Usage: send <to> <amount> <fee> [message]"); break; }
                        {
                            String[] a = args.split(" ",4);
                            if (a.length < 3) { System.out.println("Usage: send <to> <amount> <fee> [message]"); break; }
                            String to = a[0];
                            double amount = Double.parseDouble(a[1]);
                            double fee = Double.parseDouble(a[2]);
                            String message = a.length >=4 ? a[3] : "";

                            Transaction t = new Transaction(wallet.getAccount(), to, amount, fee, 0L, message, "");
                            String signature = wallet.sign(t.contentToBase64());
                            t.setSignature(signature);
                            boolean ok = blockchain.receiveTransaction(t);
                            System.out.println(ok ? "Transaction accepted" : "Transaction rejected");
                        }
                        break;

                    case "quit":
                        System.out.println("Exiting...");
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Unknown command. Type 'help'.");
                }
            }
        } catch (Exception e) {
            Logger.error("Console error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void networkServer(Blockchain blockchain) {
        try {
            ServerSocket serverSocket = new ServerSocket(p2pblockchain.config.NetworkConfig.socketPort);
            Logger.log("Network Ready");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(
                        () -> {
                            clientHandler(clientSocket, blockchain);
                        }
                );
                clientThread.start();
            }
        } catch (Exception e) {
            Logger.error("Failed to start network server.");
            e.printStackTrace();
        }
    }

    public static void clientHandler(Socket clientSocket, Blockchain blockchain) {
        try {
            BufferedReader socketInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter socketOutput = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            Logger.log(clientSocket.getInetAddress().getHostAddress() + " Connected.");
            
            String receivedMessage;
            while ((receivedMessage = socketInput.readLine()) != null) {
                Logger.log("Received: " + receivedMessage);
                
                if (receivedMessage.contains(", ")) {
                    String[] messageElements = receivedMessage.split(", ");
                    String request = messageElements[0];
                    String requestContent = Base64Utils.decodeToString(messageElements[1]);
                    
                    if (request.contentEquals(MessageType.GET_BALANCE)) {
                        // receive: getBalance, address;
                        // returns: b64(getBalance(address))
                        socketOutput.write(
                            Base64Utils.encodeToString(
                                Double.toString(blockchain.getAccountBalance(requestContent))
                            ) + "\n"
                        );
                        socketOutput.flush();

                    } else if (request.contentEquals(MessageType.DO_TRANSACT)) {
                        // receive: doTransact, b64(transaction.toBase64)
                        // returns: b64(Ok), b64(Error)
                        // messageElements[1] is the Base64(transaction.toBase64())
                        Transaction receivedTransaction = new Transaction(messageElements[1]);
                        if (blockchain.receiveTransaction(receivedTransaction)) {
                            socketOutput.write(Base64Utils.encodeToString("Ok") + "\n");
                        } else {
                            socketOutput.write(Base64Utils.encodeToString("Error") + "\n");
                        }
                        socketOutput.flush();
                    
                    } else if (request.contentEquals(MessageType.GET_CLONE_CHAIN_FROM)) {
                        // receive: getCloneChainFrom, b64(networkNode.toBase64)
                        P2PNode nodeToClone = new P2PNode(messageElements[1]);
                        if (blockchain.getBlockchainFrom(nodeToClone)) {
                            socketOutput.write(Base64Utils.encodeToString("Ok") + "\n");
                        } else {
                            socketOutput.write(Base64Utils.encodeToString("Error") + "\n");
                        }
                        socketOutput.flush();
                    
                    } else if (request.contentEquals(MessageType.JOIN_NETWORK)) {
                        // receive: joinNetwork, b64(networkNode.toBase64)
                        P2PNode joiningNode = new P2PNode(messageElements[1]);
                        if (blockchain.receiveP2PNode(joiningNode)) {
                            socketOutput.write(Base64Utils.encodeToString("Ok") + "\n");
                        } else {
                            socketOutput.write(Base64Utils.encodeToString("Dup") + "\n");
                        }
                        socketOutput.flush();
                    
                    } else if (request.contentEquals(MessageType.LEAVE_NETWORK)) {
                        P2PNode leavingNode = new P2PNode(messageElements[1]);
                        blockchain.removeP2PNode(leavingNode);
                        socketOutput.write(Base64Utils.encodeToString("Bye") + "\n");
                        socketOutput.flush();
                        Logger.log("Node " + leavingNode.toString() + " has left the network.");

                    } else if (request.contentEquals(MessageType.BCAST_BLOCK)) {
                        // receive: broadcastedBlock, b64(block.toBase64)
                        // returns: b64(Ok), b64(Duplicate)
                        Block receivedBlock = new Block(messageElements[1]);
                        if (blockchain.receiveBlock(receivedBlock)) {
                            socketOutput.write(Base64Utils.encodeToString("Ok") + "\n");
                        } else {
                            socketOutput.write(Base64Utils.encodeToString("Duplicate or Tampered") + "\n");
                        }
                        socketOutput.flush();

                    } else if (request.contentEquals(MessageType.BCAST_TRANSACT)) {
                        // receive: broadcastedTransaction, b64(transaction.toBase64)
                        Transaction receivedTransaction = new Transaction(messageElements[1]);
                        if (blockchain.receiveTransaction(receivedTransaction)) {
                            socketOutput.write(Base64Utils.encodeToString("Ok") + "\n");
                        } else {
                            socketOutput.write(Base64Utils.encodeToString("Duplicate or Invalid") + "\n");
                        }
                        socketOutput.flush();

                    } else if (request.contentEquals(MessageType.BCAST_NEWNODE)) {
                        // receive: broadcastedNewNode, b64(networkNode.toBase64)
                        P2PNode receivedNode = new P2PNode(messageElements[1]);

                        // Don't add yourself - check with port

                        if (blockchain.receiveP2PNode(receivedNode)) {
                            socketOutput.write(Base64Utils.encodeToString("Ok") + "\n");
                        } else {
                            socketOutput.write(Base64Utils.encodeToString("Duplicate") + "\n");
                        }
                        socketOutput.flush();

                    }
                
                // No recognized command
                } else {
                    if (receivedMessage.contentEquals(MessageType.MINE_START)) {
                        blockchain.startMining();
                        socketOutput.write(Base64Utils.encodeToString("Ok") + "\n");
                        socketOutput.flush();
                    } else if (receivedMessage.contentEquals(MessageType.MINE_STOP)) {
                        blockchain.stopMining();
                        socketOutput.write(Base64Utils.encodeToString("Ok") + "\n");
                        socketOutput.flush();
                    } else if (receivedMessage.contentEquals(MessageType.CLONE_CHAIN)) {
                        socketOutput.write(blockchain.toBase64ForExchange() + "\n");
                        socketOutput.flush();
                    } else {
                        Logger.error("Unrecognized command: " + receivedMessage);
                        socketOutput.write(Base64Utils.encodeToString("Error") + "\n");
                        socketOutput.flush();
                    }
                }
            }
            socketInput.close();
            socketOutput.close();
            clientSocket.close();

        } catch (Exception e) {
            Logger.error("Error in client socket " + clientSocket.getInetAddress().getHostAddress());
            e.printStackTrace();
        }
    }
}