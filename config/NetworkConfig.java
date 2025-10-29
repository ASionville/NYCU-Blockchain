package p2pblockchain.config;

/**
 * Network configuration constants for the P2PBlockchain project.
 */
public class NetworkConfig {
    // Default port (can be changed at runtime using setSocketPort)
    public static int socketPort = 8300;
    public static final String socketHost = "127.0.0.1";

    /**
     * Change the network socket port at runtime.
     * @param port new port to use for the network server
     */
    public static void setSocketPort(int port) {
        socketPort = port;
    }

    public static int getSocketPort() {return socketPort;}
}