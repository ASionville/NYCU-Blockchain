package p2pblockchain.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing data using the project's configured hash algorithm.
 */
public class HashUtils {
    /**
     * Compute hash of the given bytes and return a hex string.
     *
     * @param data input bytes
     * @return hex-encoded digest
     */
    public static String hashBytes(byte[] data) {
        try {
            // Use SHA3-256 as the project's hashing algorithm for stronger security.
            MessageDigest digest = MessageDigest.getInstance(p2pblockchain.config.SecurityConfig.HASH_ALGORITHM);
            byte[] hashedBytes = digest.digest(data);
            return p2pblockchain.utils.Converter.bytesToHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            p2pblockchain.utils.Logger.error("Something went wrong when hashing data.");
            return "";
        }
    }

    /**
     * Compute hash of the given string (using platform default charset).
     *
     * @param data input string
     * @return hex-encoded hash digest
     */
    public static String hashString(String data) {
        return hashBytes(data.getBytes());
    }

    /**
     * Validate that the given byte array matches the provided hash.
     *
     * @param hash Expected hash value
     * @param data Input data to validate
     * @return true if the data's hash matches the expected hash, false otherwise
     */
    public static boolean validateHash(String hash, byte[] data) {
        String computedHash = hashBytes(data);
        return computedHash.equals(hash);
    }

    /**
     * Validate that the given string matches the provided hash.
     *
     * @param hash Expected hash value
     * @param data Input string to validate
     * @return true if the data's hash matches the expected hash, false otherwise
     */
    public static boolean validateHash(String hash, String data) {
        return validateHash(hash, data.getBytes());
    }
}