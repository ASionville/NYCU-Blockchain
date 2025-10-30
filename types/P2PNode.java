package p2pblockchain.types;

import java.net.Socket;

import p2pblockchain.utils.Base64Utils;
import p2pblockchain.utils.HashUtils;
import p2pblockchain.utils.JsonObject;
import p2pblockchain.utils.Logger;

/**
 * Represents a peer-to-peer network node in the P2PBlockchain system.
 * Encapsulates the node's network address and port, and manages the socket connection.
 */
public class P2PNode implements AutoCloseable {
    private String nodeAddress;
    private int nodePort;
    private Socket socket;

    /**
     * Create an empty P2PNode with default values.
     */
    public P2PNode() {
        this.nodeAddress = "";
        this.nodePort = 0;
        this.socket = null;
    }

    /**
     * Create a P2PNode with specified address and port.
     *
     * @param nodeAddress The IP address or hostname of the node
     * @param nodePort    The port number of the node
     */
    public P2PNode(String nodeAddress, int nodePort) {
        this.nodeAddress = nodeAddress;
        this.nodePort = nodePort;
        this.socket = null;
    }

    /**
     * Create a P2PNode from its Base64-encoded representation.
     *
     * @param b64EncodedNode Base64 string encoding the node's address and port
     */
    public P2PNode(String b64EncodedNode) {
        this.fromBase64(b64EncodedNode);
    }

    /**
     * Closes the socket connection for this P2PNode.
     */
    @Override
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Logger.error("Cannot close connection to " + this.nodeAddress + " : " + Integer.toString(nodePort));
            e.printStackTrace();
        }
    }

    public String getNodeAddress() {return this.nodeAddress;}
    public int getNodePort() {return this.nodePort;}
    
    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
        this.disconnect();
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
        this.disconnect();
    }

    /**
     * Establish a socket connection to the P2PNode.
     *
     * @return true if the connection is successful, false otherwise
     */
    public boolean connect() {
        try {
            if (this.nodeAddress.isBlank() || this.nodePort == 0) {
                Logger.error("Cannot connect to node : address or port missing.");
                return false;
            }
            if (this.socket == null) {
                this.socket = new Socket(this.nodeAddress, this.nodePort);
                if (this.socket.isConnected()) {
                    return true;
                } else {
                    Logger.error("Cannot connect to " + this.nodeAddress + " : " + Integer.toString(this.nodePort));
                    this.socket.close();
                    return false;
                }
            } else {
                return this.socket.isConnected();
            }
        } catch (Exception e) {
            Logger.error("Cannot connect to " + this.nodeAddress + " : " + Integer.toString(this.nodePort));
            e.printStackTrace();
            this.socket = null;
            return false;
        }
    }

    /**
     * Disconnect the socket connection from the P2PNode.
     *
     * @return true if disconnection is successful, false otherwise
     */
    public boolean disconnect() {
        try {
            if (this.socket == null) {
                return true;
            } else {
                this.socket.close();
                this.socket = null;
                return true;
            }
        } catch (Exception e) {
            Logger.error("Cannot disconnect from " + this.nodeAddress + " : " + Integer.toString(this.nodePort));
            e.printStackTrace();
            this.socket = null;
            return false;
        }
    }

    /**
     * Check if the P2PNode is currently connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return this.socket != null && this.socket.isConnected();
    }

    /**
     * Check if the P2PNode has no active socket connection.
     *
     * @return true if there is no active socket, false otherwise
     */
    public boolean isNull() {
        return socket == null;
    }

    /**
     * Get the socket associated with this P2PNode.
     *
     * @return the Socket object
     */
    public Socket getNodeSocket() {
        if (this.socket == null) {
            Logger.error("Socket is null for node " + this.nodeAddress + " : " + Integer.toString(this.nodePort));
        }

        return this.socket;
    }

    /**
     * Encode the P2PNode to a plain String.
     * 
     * @return String representation of the node
     */
    public String toString() {
        return this.nodeAddress + ":" + Integer.toString(this.nodePort);
    }

    /**
     * Encode the P2PNode to a Base64 string.
     *
     * @return Base64 representation of the node
     */
    public String toBase64() {
        return Base64Utils.encodeToString(this.toJson().toString());
    }

    /**
     * Decode a Base64 string to populate the P2PNode fields.
     *
     * @param nodeAsBase64 Base64 representation of the node
     * @return true if decoding is successful, false otherwise
     */
    public boolean fromBase64(String nodeAsBase64) {
        try {
            String jsonStr = Base64Utils.decodeToString(nodeAsBase64);
            return this.fromJson(new JsonObject(jsonStr));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert this node to a JSON representation.
     *
     * @return JSON representation of the node
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("nodeAddress", this.nodeAddress);
        json.put("nodePort", this.nodePort);
        return json;
    }

    /**
     * Populate this node from a JSON representation.
     *
     * @param json JSON representation of the node
     * @return true if parsing succeeded, false otherwise
     */
    public boolean fromJson(JsonObject json) {
        try {
            this.nodeAddress = json.getString("nodeAddress");
            this.nodePort = json.getInt("nodePort");
            this.socket = null;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Compute the hash of the P2PNode's Base64 representation.
     *
     * @return Hash string of the node
     */
    public String toHash() {
        return HashUtils.hashString(this.toBase64());
    }

    /**
     * Two nodes are equal if they have the same address and port.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        P2PNode other = (P2PNode) obj;

        // For node address, consider "localhost" and "127.0.0.1" as equal
        if ((this.nodeAddress.equals("localhost") && other.nodeAddress.equals("127.0.0.1"))
            || (this.nodeAddress.equals("127.0.0.1") && other.nodeAddress.equals("localhost"))) {
            return this.nodePort == other.nodePort;
        }
        return this.nodeAddress.equals(other.nodeAddress) && this.nodePort == other.nodePort;
    }

    /**
     * Hash code based on address and port for use in HashSet/HashMap.
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(nodeAddress, nodePort);
    }
}