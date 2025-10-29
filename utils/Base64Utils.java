package p2pblockchain.utils;

import java.util.Base64;

/**
 * Utility class for Base64 encoding and decoding.
 */
public class Base64Utils {
    /**
     * Encode the provided data to a Base64 string.
     *
     * @param data Data to encode
     * @return Base64-encoded string
     */
    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decode the provided Base64 string to raw bytes.
     *
     * @param base64 Base64 string to decode
     * @return Decoded byte array
     */
    public static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Encode the provided string to a Base64 string.
     *
     * @param data String to encode
     * @return Base64-encoded string
     */
    public static String encodeToString(String data) {
        return encode(data.getBytes());
    }

    /**
     * Decode the provided Base64 string to a regular string.
     *
     * @param base64 Base64 string to decode
     * @return Decoded string
     */
    public static String decodeToString(String base64) {
        return new String(decode(base64));
    }
}