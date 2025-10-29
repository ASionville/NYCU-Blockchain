package p2pblockchain.utils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class SecurityUtils {
    /**
     * Restore a PublicKey object from its Base64-encoded string representation.
     *
     * @param address Base64-encoded public key string
     * @return Restored PublicKey object, or null if restoration fails
     */
    private static PublicKey restorePublicKeyFromAddress(String address) {
        try {
            byte[] publicKeyBytes = Base64Utils.decode(address);
            return KeyFactory.getInstance(p2pblockchain.config.SecurityConfig.PUBLIC_KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (Exception e) {
            Logger.error("Cannot restore public key from: " + address);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validate a digital signature for given data using the public key derived from the address.
     *
     * @param address          Base64-encoded public key string
     * @param data             Original data that was signed
     * @param encodedSignature Base64-encoded digital signature
     * @return true if the signature is valid, false otherwise
     */
    public static boolean isSignatureValid(String address, String data, String encodedSignature) {
        try {
            PublicKey restoredPublicKey = restorePublicKeyFromAddress(address);
            Signature signer = Signature.getInstance(p2pblockchain.config.SecurityConfig.SIGNATURE_ALGORITHM);
            signer.initVerify(restoredPublicKey);
            signer.update(data.getBytes());
            byte[] signature = Base64Utils.decode(encodedSignature);
            return signer.verify(signature);
        } catch (Exception e) {
            Logger.error("Something went wrong when validating signature");
            e.printStackTrace();
            return false;
        }
    }
}