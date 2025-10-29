package p2pblockchain.utils;

import java.security.SecureRandom;

/**
 * Utility class for generating nonces.
 */
public class NonceGenerator {
    final private static int nonce_mode = 1;
    private static SecureRandom secureRandom = new SecureRandom();
    private static int counter = 0;
    
    /**
     * Get a nonce based on the configured mode.
     *
     * @return generated nonce as an integer
     */
    public static int getNonce() {
        switch (nonce_mode) {
        case 0:
            return additionMode();
        case 1:
            return randomMode();
        default:
            return additionMode();
        }
    }
    
    /**
     * Generate a nonce using an incrementing counter.
     *
     * @return incremented nonce
     */
    private static int additionMode() {
        return counter++;
    }
    
    /**
     * Generate a nonce using a secure random number generator.
     *
     * @return random nonce
     */
    private static int randomMode() {
        return secureRandom.nextInt(0, Integer.MAX_VALUE);
    }
}