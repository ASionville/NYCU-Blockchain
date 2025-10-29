package p2pblockchain.config;

/**
 * Network configuration constants for the P2PBlockchain project.
 */
public class NetworkConfig {
    // Default port (can be changed at runtime using setSocketPort)
    public static int socketPort = 8300;
    public static final String socketHost = "0.0.0.0"; 

    // Port range to scan for existing blockchain nodes
    public static final int PORT_SCAN_START = 8000;
    public static final int PORT_SCAN_END = 9000;
    
    // Network discovery settings
    public static final boolean USE_BROADCAST_DISCOVERY = true;
    public static final int BROADCAST_PORT = 8299;
    public static final int BROADCAST_TIMEOUT_MS = 3000;

    // Fallback: scan local network if broadcast fails
    public static final boolean SCAN_LOCAL_NETWORK = false; 
    public static final int NETWORK_SCAN_TIMEOUT_MS = 100; 

    /**
     * Change the network socket port at runtime.
     * @param port new port to use for the network server
     */
    public static void setSocketPort(int port) {
        socketPort = port;
    }

    public static int getSocketPort() {return socketPort;}
}