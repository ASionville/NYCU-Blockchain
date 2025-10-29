package p2pblockchain.types;

import p2pblockchain.utils.Base64Utils;
import p2pblockchain.utils.HashUtils;
import p2pblockchain.utils.JsonObject;
import p2pblockchain.utils.TimeUtils;

/**
 * Represents a monetary transaction between two addresses.
 *
 * A Transaction contains sender and receiver addresses, an amount, an optional
 * fee, a timestamp, an optional message, and a signature.
 * This class provides JSON serialization/deserialization methods and convenience hashing helpers.
 */
public class Transaction {
    private String sender;
    private String receiver;
    private double amount;
    private double fee;
    private long timestamp;
    private String message;
    private String signature;
    
    /**
     * Create an empty transaction with default values.
     */
    public Transaction() {
        this.sender= "";
        this.receiver = "";
        this.amount = 0;
        this.fee = 0;
        this.timestamp = 0L;
        this.message = "";
        this.signature = "";
    }
    
    /**
     * Create a transaction with all fields specified.
     *
     * @param sender    The sender's address
     * @param receiver  The receiver's address
     * @param amount    The transferred amount
     * @param fee       The transaction fee
     * @param timestamp Unix epoch milliseconds timestamp (if 0, current time is used)
     * @param message   Optional message attached to the transaction
     * @param signature Signature over the transaction content
     */
    public Transaction(
            String sender,
            String receiver,
            double amount,
            double fee,
            long timestamp,
            String message,
            String signature
    ) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.fee = fee;
        if (timestamp == 0) {
            this.timestamp = TimeUtils.getNowAsLong();
        } else {
            this.timestamp = timestamp;
        }
        this.message = message;
        this.signature = signature;
    }
    
    /**
     * Create a transaction by decoding a Base64-encoded JSON representation.
     *
     * @param transactionInBase64 Base64 string previously returned by {@link #toBase64}
     */
    public Transaction(String transactionInBase64) {
        this.fromBase64(transactionInBase64);
    }

    public String getSender() {return this.sender;}
    public String getReceiver() {return this.receiver;}
    public double getAmount() {return this.amount;}
    public double getFee() {return this.fee;}
    public long getTimestamp() {return this.timestamp;}
    public String getMessage() {return this.message;}
    public String getSignature() {return this.signature;}

    public void setSender(String sender) {this.sender = sender;}
    public void setReceiver(String receiver) {this.receiver = receiver;}
    public void setAmount(double amount) {this.amount = amount;}
    public void setFee(double fee) {this.fee = fee;}
    public void setTimestamp(long timestamp) {this.timestamp = timestamp;}
    public void setMessage(String message) {this.message = message;}
    public void setSignature(String signature) {this.signature = signature;}

    @Override
    public String toString() {
        return "Transaction{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", amount=" + amount +
                ", fee=" + fee +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }

    public String toBase64() {
        return Base64Utils.encodeToString(this.toJson().toString());
    }

    /**
     * Serialize the transaction content (excluding the signature) to JSON and
     * return a Base64-encoded string.
     *
     * @return Base64(JSON(content(transaction)))
     */
    public String contentToBase64() {
        return Base64Utils.encodeToString(this.contentToJson().toString());
    }

    /**
     * Decode a Base64-encoded JSON transaction and populate this object.
     *
     * @param transactionInBase64 Base64(JSON(transaction))
     * @return true if parsing succeeded, false otherwise
     */
    public boolean fromBase64(String transactionInBase64) {
        try {
            String jsonStr = Base64Utils.decodeToString(transactionInBase64);
            return this.fromJson(new JsonObject(jsonStr));
        } catch (Exception e) {
            return false;
        }
    }
    
    public String toHash() {
        return HashUtils.hashString(this.toBase64());
    }

    public String contentToHash() {
        return HashUtils.hashString(this.contentToBase64());
    }

    /**
     * Serialize this transaction to JSON.
     *
     * @return JSON representation of the transaction
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("sender", this.sender);
        json.put("receiver", this.receiver);
        json.put("amount", this.amount);
        json.put("fee", this.fee);
        json.put("timestamp", this.timestamp);
        json.put("message", this.message);
        json.put("signature", this.signature);
        return json;
    }

    /**
     * Serialize the transaction content (excluding the signature) to JSON.
     *
     * @return JSON representation of the transaction content
     */
    public JsonObject contentToJson() {
        JsonObject json = new JsonObject();
        json.put("sender", this.sender);
        json.put("receiver", this.receiver);
        json.put("amount", this.amount);
        json.put("fee", this.fee);
        json.put("timestamp", this.timestamp);
        json.put("message", this.message);
        return json;
    }

    /**
     * Populate this transaction from a JSON representation.
     *
     * @param json JSON representation of the transaction
     * @return true if parsing succeeded, false otherwise
     */
    public boolean fromJson(JsonObject json) {
        try {
            this.sender = json.getString("sender");
            this.receiver = json.getString("receiver");
            this.amount = json.getDouble("amount");
            this.fee = json.getDouble("fee");
            this.timestamp = json.getLong("timestamp");
            this.message = json.getString("message");
            this.signature = json.getString("signature");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Populate this transaction from a JSON representation of its content
     * (excluding the signature).
     *
     * @param json JSON representation of the transaction content
     * @return true if parsing succeeded, false otherwise
     */
    public boolean fromContentJson(JsonObject json) {
        try {
            this.sender = json.getString("sender");
            this.receiver = json.getString("receiver");
            this.amount = json.getDouble("amount");
            this.fee = json.getDouble("fee");
            this.timestamp = json.getLong("timestamp");
            this.message = json.getString("message");
            // signature intentionally not set from content JSON
            this.signature = "";
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}