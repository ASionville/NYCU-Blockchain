package p2pblockchain.types;

import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import p2pblockchain.utils.Converter;
import p2pblockchain.utils.FilesUtils;
import p2pblockchain.utils.Logger;

/**
 * Represents a wallet containing a public/private key pair.
 *
 * The Wallet class provides methods for key generation, signing data, and retrieving
 * the wallet's public address.
 */
public class Wallet {
    private String walletName;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    /**
     * Create or load a wallet with the specified name.
     * If the key files do not exist, a new key pair is generated and saved.
     *
     * @param name The name of the wallet
     */
    public Wallet(String name) {
        this.walletName = name;
        try {
            if (!FilesUtils.fileExist("wallets/"+ walletName + "/public_key.key") || !FilesUtils.fileExist("wallets/"+ walletName + "/private_key.key")) {
                
                FilesUtils.createDirectory("wallets/" + walletName);
                final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(p2pblockchain.config.SecurityConfig.PUBLIC_KEY_ALGORITHM);
                keyGen.initialize(p2pblockchain.config.SecurityConfig.PUBLIC_KEY_LENGTH);
                final KeyPair keyPair = keyGen.generateKeyPair();
                
                this.publicKey = keyPair.getPublic();
                this.privateKey = keyPair.getPrivate();
                
                byte[] publicKeyBytes = publicKey.getEncoded();
                byte[] privateKeyBytes = privateKey.getEncoded();
                
                FilesUtils.writeFile("wallets/" + walletName + "/public_key.key", publicKeyBytes, "c");
                FilesUtils.writeFile("wallets/" + walletName + "/private_key.key", privateKeyBytes, "c");

                Logger.log("Keypair generated.");
            } else {
                this.publicKey = KeyFactory.getInstance(p2pblockchain.config.SecurityConfig.PUBLIC_KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(FilesUtils.readFileToBytes("wallets/"+ walletName + "/public_key.key")));
                this.privateKey = KeyFactory.getInstance(p2pblockchain.config.SecurityConfig.PUBLIC_KEY_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(FilesUtils.readFileToBytes("wallets/"+ walletName + "/private_key.key")));
            }
        } catch (Exception e) {
            Logger.error("Cannot load key pairs.");
            e.printStackTrace();
        }
    }

    public String getName() {return this.walletName;}
    public String getAccount() {
        return Converter.bytesToBase64(publicKey.getEncoded());
    }

    /**
     * Sign the given data using the wallet's private key.
     *
     * @param data Input data to sign
     * @return Base64-encoded signature string
     */
    public String sign(String data) {
        try {
            Signature signer = Signature.getInstance(p2pblockchain.config.SecurityConfig.SIGNATURE_ALGORITHM);
            signer.initSign(privateKey);
            signer.update(data.getBytes());
            byte[] signatureBytes = signer.sign();
            return Converter.bytesToBase64(signatureBytes);
        } catch (Exception e) {
            Logger.error("Signing failed.");
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Decrypt the given Base64-encoded cipher text using the wallet's private key.
     *
     * @param base64CipherText Input Base64-encoded cipher text
     * @return Decrypted plain text
     */
    public String decrypt(String base64CipherText) {
        try {
            Cipher cipher = Cipher.getInstance(p2pblockchain.config.SecurityConfig.PUBLIC_KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] cipherBytes = Converter.base64ToBytes(base64CipherText);
            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            Logger.error("Decryption failed.");
            e.printStackTrace();
            return "";
        }
    }
}