package p2pblockchain.utils;

import p2pblockchain.config.BlockchainConfig;

/**
 * Simple logging utility for the P2PBlockchain project.
 */
public class Logger {
    public static void log(String message) {
        if (BlockchainConfig.getVerboseLevel() >= 4 ) {
            System.out.println("[P2PBlockchain][DEBUG] " + message);
        }
    }

    public static void info(String message) {
        if (BlockchainConfig.getVerboseLevel() >= 3) {
            System.out.println("[P2PBlockchain][INFO] " + message);
        }
    }

    public static void warn(String message) {
        if (BlockchainConfig.getVerboseLevel() >= 2) {
            System.out.println("[P2PBlockchain][WARNING] " + message);
        }
    }
    
    public static void error(String message) {
        if (BlockchainConfig.getVerboseLevel() >= 1) {
            System.err.println("[P2PBlockchain][ERROR] " + message);
        }
    }
}