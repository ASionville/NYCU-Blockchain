package p2pblockchain.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import p2pblockchain.types.Block;
import p2pblockchain.types.Blockchain;
import p2pblockchain.types.MessageType;
import p2pblockchain.types.P2PNode;
import p2pblockchain.types.Transaction;
import p2pblockchain.types.Wallet;
import p2pblockchain.utils.Base64Utils;
import p2pblockchain.utils.Logger;

public class startBlockchain {

    // Port range to scan for existing blockchain nodes

    /**
     * Get all local network IP addresses (private network ranges).
     * Returns a list of IP prefixes to scan (e.g., "192.168.1" for 192.168.1.0/24).
     * 
     * @return List of network prefixes to scan
     */
    public static List<String> getLocalNetworkPrefixes() {
        List<String> networkPrefixes = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                
                // Skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    
                    // Only process IPv4 addresses in private ranges
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        
                        // Check if it's a private network IP
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || 
                            (ip.startsWith("172.") && Integer.parseInt(ip.split("\\.")[1]) >= 16 && Integer.parseInt(ip.split("\\.")[1]) <= 31)) {
                            
                            // Extract network prefix (e.g., "192.168.1" from "192.168.1.100")
                            String[] parts = ip.split("\\.");
                            if (parts.length == 4) {
                                String prefix = parts[0] + "." + parts[1] + "." + parts[2];
                                if (!networkPrefixes.contains(prefix)) {
                                    networkPrefixes.add(prefix);
                                    Logger.log("Detected local network: " + prefix + ".0/24");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Error detecting local networks: " + e.getMessage());
        }
        
        // Always add localhost as fallback
        if (!networkPrefixes.contains("127.0.0")) {
            networkPrefixes.add("127.0.0");
        }
        
        return networkPrefixes;
    }

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
                        // Apply to NetworkConfig so server thread picks it up
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
        
        // Start broadcast listener thread
        Thread broadcastThread = new Thread(() -> { broadcastListener(blockchain); });
        broadcastThread.setDaemon(true);
        broadcastThread.start();

        // Wait for network server to be ready
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Try to connect to bootstrap nodes on startup
        discoverAndJoinNetwork(blockchain);

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
     * Scans the network (localhost and/or local LAN) for running P2P nodes.
     * Ignores the local node (same port) and tries to clone the blockchain from
     * the first active node found.
     * 
     * @param blockchain The local Blockchain instance
     */
    public static void discoverAndJoinNetwork(Blockchain blockchain) {
        P2PNode myNode = blockchain.getMyNode();
        List<P2PNode> discoveredNodes = new ArrayList<>();
        
        // Try broadcast discovery first if enabled
        if (p2pblockchain.config.NetworkConfig.USE_BROADCAST_DISCOVERY) {
            Logger.log("Broadcasting discovery message to local network...");
            discoveredNodes = discoverNodesByBroadcast(myNode);
            
            if (!discoveredNodes.isEmpty()) {
                Logger.log("Found " + discoveredNodes.size() + " node(s) via broadcast.");
            } else {
                Logger.log("No nodes responded to broadcast.");
                
                // Fallback to network scan if enabled
                if (p2pblockchain.config.NetworkConfig.SCAN_LOCAL_NETWORK) {
                    Logger.log("Falling back to network scan...");
                    discoveredNodes = discoverNodesByScan(myNode);
                }
            }
        } else if (p2pblockchain.config.NetworkConfig.SCAN_LOCAL_NETWORK) {
            // Broadcast disabled, use network scan directly
            Logger.log("Using network scan...");
            discoveredNodes = discoverNodesByScan(myNode);
        } else {
            // Both disabled, just check localhost
            Logger.log("Checking localhost only...");
            discoveredNodes = discoverNodesByScan(myNode);
        }
        
        // Connect to all discovered nodes
        P2PNode firstActiveNode = null;
        for (P2PNode remoteNode : discoveredNodes) {
            try {
                try (Socket testSocket = new Socket()) {
                    testSocket.connect(new java.net.InetSocketAddress(
                        remoteNode.getNodeAddress(), remoteNode.getNodePort()), 1000);
                    
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(testSocket.getOutputStream()));
                    BufferedReader in = new BufferedReader(new InputStreamReader(testSocket.getInputStream()));
                    
                    // Send a Join Network request
                    out.write(MessageType.JOIN_NETWORK + ", " + myNode.toBase64() + "\n");
                    out.flush();

                    String responseB64 = in.readLine();
                    if (responseB64 != null) {
                        String response = Base64Utils.decodeToString(responseB64);
                        
                        if ("Ok".equals(response) || "Dup".equals(response)) {
                            Logger.log("Connected to node " + remoteNode.getNodeAddress() + ":" + 
                                     remoteNode.getNodePort() + " (response: " + response + ")");
                            blockchain.addP2PNodes(remoteNode);

                            if (firstActiveNode == null) {
                                firstActiveNode = remoteNode;
                            }
                        }
                    }
                    
                    out.close();
                    in.close();
                }
            } catch (Exception e) {
                Logger.warn("Failed to join node " + remoteNode.getNodeAddress() + ":" + 
                          remoteNode.getNodePort() + " - " + e.getMessage());
            }
        }
        
        // Clone blockchain from first node if found
        if (firstActiveNode != null) {
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
     * Discover nodes using UDP broadcast.
     * Sends a broadcast message and waits for responses from other nodes.
     * 
     * @param myNode The local node information
     * @return List of discovered P2P nodes
     */
    public static List<P2PNode> discoverNodesByBroadcast(P2PNode myNode) {
        List<P2PNode> discoveredNodes = new ArrayList<>();
        
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(p2pblockchain.config.NetworkConfig.BROADCAST_TIMEOUT_MS);
            
            // Prepare broadcast message: "BLOCKCHAIN_DISCOVER:<port>:<responsePort>"
            // Include a response port so the listener knows where to send the reply
            int responsePort = socket.getLocalPort();
            String message = "BLOCKCHAIN_DISCOVER:" + myNode.getNodePort() + ":" + responsePort;
            byte[] sendData = message.getBytes();
            
            // Send broadcast to multiple addresses for better compatibility
            List<InetAddress> broadcastAddresses = new ArrayList<>();
            
            // Add general broadcast
            broadcastAddresses.add(InetAddress.getByName("255.255.255.255"));
            
            // Add subnet-specific broadcasts for all active interfaces
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) continue;
                    
                    for (InterfaceAddress ifaceAddr : iface.getInterfaceAddresses()) {
                        InetAddress broadcast = ifaceAddr.getBroadcast();
                        if (broadcast != null && !broadcastAddresses.contains(broadcast)) {
                            broadcastAddresses.add(broadcast);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.warn("Could not enumerate network interfaces: " + e.getMessage());
            }
            
            // Send to all broadcast addresses
            for (InetAddress broadcastAddr : broadcastAddresses) {
                try {
                    DatagramPacket sendPacket = new DatagramPacket(
                        sendData, sendData.length, broadcastAddr, 
                        p2pblockchain.config.NetworkConfig.BROADCAST_PORT);
                    socket.send(sendPacket);
                    Logger.log("Broadcast sent to " + broadcastAddr.getHostAddress());
                } catch (Exception e) {
                    Logger.warn("Failed to broadcast to " + broadcastAddr.getHostAddress() + ": " + e.getMessage());
                }
            }
            
            Logger.log("Waiting for responses (timeout: " + 
                p2pblockchain.config.NetworkConfig.BROADCAST_TIMEOUT_MS + "ms)...");
            
            // Listen for responses
            byte[] receiveData = new byte[1024];
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < p2pblockchain.config.NetworkConfig.BROADCAST_TIMEOUT_MS) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    
                    // Expected response: "BLOCKCHAIN_NODE:<port>"
                    if (response.startsWith("BLOCKCHAIN_NODE:")) {
                        String[] parts = response.split(":");
                        if (parts.length == 2) {
                            try {
                                int port = Integer.parseInt(parts[1].trim());
                                String address = receivePacket.getAddress().getHostAddress();
                                
                                // Don't add ourselves
                                if (port != myNode.getNodePort() || !address.equals(getLocalIPAddress())) {
                                    P2PNode node = new P2PNode(address, port);
                                    discoveredNodes.add(node);
                                    Logger.log("Discovered node: " + address + ":" + port);
                                }
                            } catch (NumberFormatException e) {
                                Logger.warn("Invalid port in broadcast response: " + response);
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // Timeout reached, stop listening
                    break;
                }
            }
            
        } catch (Exception e) {
            Logger.error("Error during broadcast discovery: " + e.getMessage());
        }
        
        return discoveredNodes;
    }
    
    /**
     * Legacy network scan method (scans all IPs in local network).
     * Used as fallback when broadcast discovery fails.
     * 
     * @param myNode The local node information
     * @return List of discovered P2P nodes
     */
    public static List<P2PNode> discoverNodesByScan(P2PNode myNode) {
        List<P2PNode> discoveredNodes = new ArrayList<>();
        List<String> hostsToScan = new ArrayList<>();
        
        // Determine which hosts to scan based on configuration
        if (p2pblockchain.config.NetworkConfig.SCAN_LOCAL_NETWORK) {
            Logger.log("Scanning local network for existing nodes...");
            List<String> networkPrefixes = getLocalNetworkPrefixes();
            
            // For each network prefix, add all possible host IPs (1-254)
            for (String prefix : networkPrefixes) {
                for (int i = 1; i <= 254; i++) {
                    hostsToScan.add(prefix + "." + i);
                }
            }
        } else {
            // Only scan localhost
            Logger.log("Scanning localhost only...");
            hostsToScan.add("127.0.0.1");
        }
        
        Logger.log("Scanning " + hostsToScan.size() + " hosts on ports " + 
                   p2pblockchain.config.NetworkConfig.PORT_SCAN_START + "-" + 
                   p2pblockchain.config.NetworkConfig.PORT_SCAN_END + "...");
        
        int myPort = myNode.getNodePort();
        String myAddress = getLocalIPAddress();
        
        int scannedCount = 0;
        int totalToScan = hostsToScan.size() * (p2pblockchain.config.NetworkConfig.PORT_SCAN_END - p2pblockchain.config.NetworkConfig.PORT_SCAN_START + 1);
        
        for (String host : hostsToScan) {
            for (int port = p2pblockchain.config.NetworkConfig.PORT_SCAN_START; 
                 port <= p2pblockchain.config.NetworkConfig.PORT_SCAN_END; port++) {
                scannedCount++;
                
                // Skip self (same port and same IP)
                if (port == myPort && (host.equals("127.0.0.1") || host.equals("localhost") || host.equals(myAddress))) {
                    continue;
                }

                try {
                    // Try to connect to the node with a short timeout
                    try (Socket testSocket = new Socket()) {
                        testSocket.connect(new java.net.InetSocketAddress(host, port), 
                                         p2pblockchain.config.NetworkConfig.NETWORK_SCAN_TIMEOUT_MS);
                        
                        // Node is listening, add it
                        P2PNode remoteNode = new P2PNode(host, port);
                        discoveredNodes.add(remoteNode);
                        Logger.log("Found node at " + host + ":" + port);
                    } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
                        // Port not responding - skip
                    }
                } catch (Exception e) {
                    // Unexpected error - skip
                }
                
                // Progress indicator every 1000 scans
                if (scannedCount % 1000 == 0) {
                    Logger.log("Scan progress: " + scannedCount + "/" + totalToScan + " (" + (scannedCount * 100 / totalToScan) + "%)");
                }
            }
        }

        Logger.log("Network scan complete. Scanned " + scannedCount + " addresses, found " + discoveredNodes.size() + " nodes.");
        return discoveredNodes;
    }
    
    /**
     * Listen for UDP broadcast discovery messages and respond with our node info.
     * This runs continuously in a background thread.
     * 
     * @param blockchain The local Blockchain instance
     */
    public static void broadcastListener(Blockchain blockchain) {
        try (DatagramSocket socket = new DatagramSocket(p2pblockchain.config.NetworkConfig.BROADCAST_PORT)) {
            socket.setBroadcast(true);
            Logger.log("Broadcast listener started on port " + p2pblockchain.config.NetworkConfig.BROADCAST_PORT);
            Logger.log("Ready to receive discovery requests from other nodes...");
            
            byte[] receiveData = new byte[1024];
            
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    String senderAddress = receivePacket.getAddress().getHostAddress();
                    
                    Logger.log("Received UDP message from " + senderAddress + ": " + message);
                    
                    // Check if it's a discovery request: "BLOCKCHAIN_DISCOVER:<port>:<responsePort>"
                    if (message.startsWith("BLOCKCHAIN_DISCOVER:")) {
                        String[] parts = message.split(":");
                        if (parts.length >= 2) {
                            try {
                                int senderPort = Integer.parseInt(parts[1].trim());
                                String senderAddress = receivePacket.getAddress().getHostAddress();
                                
                                // Get response port if provided, otherwise use source port
                                int responsePort = receivePacket.getPort();
                                if (parts.length >= 3) {
                                    try {
                                        responsePort = Integer.parseInt(parts[2].trim());
                                    } catch (NumberFormatException e) {
                                        // Use source port as fallback
                                    }
                                }
                                
                                // Don't respond to ourselves
                                P2PNode myNode = blockchain.getMyNode();
                                String localIP = getLocalIPAddress();
                                
                                // Check if it's from ourselves (same IP and port)
                                if (senderPort == myNode.getNodePort() && 
                                    (senderAddress.equals(localIP) || senderAddress.equals("127.0.0.1"))) {
                                    Logger.log("Ignoring own broadcast from " + senderAddress);
                                    continue;
                                }
                                
                                Logger.log("Received discovery request from " + senderAddress + ":" + senderPort);
                                
                                // Respond with our node info: "BLOCKCHAIN_NODE:<port>"
                                String response = "BLOCKCHAIN_NODE:" + myNode.getNodePort();
                                byte[] sendData = response.getBytes();
                                
                                DatagramPacket sendPacket = new DatagramPacket(
                                    sendData, sendData.length,
                                    receivePacket.getAddress(), responsePort);
                                
                                socket.send(sendPacket);
                                Logger.log("Sent discovery response to " + senderAddress + ":" + responsePort);
                                
                            } catch (NumberFormatException e) {
                                Logger.warn("Invalid discovery message format: " + message);
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.error("Error in broadcast listener: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to start broadcast listener: " + e.getMessage());
        }
    }
    
    /**
     * Get the local IP address of this machine (non-loopback).
     * 
     * @return Local IP address as a string, or "127.0.0.1" if not found
     */
    public static String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        // Return first non-loopback IPv4 address
                        if (!ip.startsWith("127.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Error getting local IP: " + e.getMessage());
        }
        return "127.0.0.1";
    }

    /**
     * Simple console REPL to interact with the local blockchain instance.
     * Supports basic commands: help, balance, mybalance, send, start, stop,
     * join, clone, listpeers, quit.
     * 
     * @param blockchain The local Blockchain instance
     * @param wallet The local Wallet instance for signing transactions
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
    
    /**
     * Network server loop to accept incoming P2P connections and handle requests.
     * 
     * @param blockchain The local Blockchain instance
     */
    public static void networkServer(Blockchain blockchain) {
        try {
            // Bind to 0.0.0.0 to accept connections from all network interfaces
            ServerSocket serverSocket = new ServerSocket(
                p2pblockchain.config.NetworkConfig.socketPort, 
                50, 
                InetAddress.getByName("0.0.0.0")
            );
            Logger.log("Network Ready on " + serverSocket.getLocalSocketAddress());
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

    /**
     * Handles an individual client connection for P2P requests.
     * 
     * @param clientSocket The connected client socket
     * @param blockchain The local Blockchain instance
     */
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