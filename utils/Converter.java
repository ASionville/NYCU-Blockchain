package p2pblockchain.utils;

import java.util.Base64;

/**
 * Utility class for conversions between byte arrays, hex strings, and Base64 strings.
 */
public class Converter {
    
    /**
     * Convert a byte array to a hex string.
     *
     * @param bytes input byte array
     * @return hex-encoded string
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Convert a hex string to a byte array.
     *
     * @param hexString input hex string
     * @return decoded byte array
     */
    public static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Convert a byte array to a Base64-encoded string.
     *
     * @param data input byte array
     * @return Base64-encoded string
     */
    public static String bytesToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }


    /**
     * Convert a Base64-encoded string to a byte array.
     *
     * @param base64 input Base64 string
     * @return decoded byte array
     */
    public static byte[] base64ToBytes(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}