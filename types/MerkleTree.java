package p2pblockchain.types;

import java.util.ArrayList;

import p2pblockchain.utils.HashUtils;
import p2pblockchain.utils.Logger;

/**
 * Simple Merkle tree implementation over a list of transactions.
 *
 * The class builds a binary Merkle tree using transaction hashes and exposes
 * the Merkle root.
 * When the number of leaves is odd, the last leaf is duplicated to form a pair
 * (standard Merkle tree padding technique).
 */
public class MerkleTree {
    /**
     * Internal node used to represent tree structure.
     */
    private static class MerkleNode {
        private String hash;
        private MerkleNode left;
        private MerkleNode right;
        
        MerkleNode(String hash) {
            this.hash = hash;
        }
        
        MerkleNode(String hash, MerkleNode left, MerkleNode right) {
            this.hash = hash;
            this.left = left;
            this.right = right;
        }
    }

    private MerkleNode root;

    /**
     * Build a Merkle tree from the provided transaction list. If the list is
     * empty or null, the constructor logs an error and leaves the root null.
     *
     * @param transactions List of transactions to include in the tree
     */
    public MerkleTree(ArrayList<Transaction> transactions) {
        // If transactions are null/empty we keep root == null. This is a
        // valid state: getMerkleRoot() will return the hash of an empty
        // string. Avoid logging an error in normal operation (e.g. genesis
        // block or blocks without transactions).
        if (transactions == null || transactions.isEmpty()) {
            this.root = null;
            return;
        }

        try {
			this.root = buildTree(transactions);
		} catch (Exception e) {
            Logger.error("Cannot build Merkle Tree: " + e.getMessage());
            this.root = null;
		}
    }

    /**
     * Recursively build the Merkle tree levels from the leaf transaction hashes.
     *
     * @param transactions Input transactions
     * @return root node of the built tree
     * @throws Exception when the input list is null or empty
     */
    private MerkleNode buildTree(ArrayList<Transaction> transactions) throws Exception {
        if (transactions == null || transactions.isEmpty()) {
            throw new Exception("No transactions to build Merkle Tree");
        }

        ArrayList<MerkleNode> currentLevel = new ArrayList<MerkleNode>();
        for (Transaction tx : transactions) {
            currentLevel.add(new MerkleNode(tx.toHash()));
        }

        while (currentLevel.size() > 1) {
            ArrayList<MerkleNode> nextLevel = new ArrayList<MerkleNode>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                MerkleNode left = currentLevel.get(i);
                MerkleNode right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;

                String combined = left.hash + right.hash;
                String parentHash = HashUtils.hashString(combined);
                nextLevel.add(new MerkleNode(parentHash, left, right));
            }
            currentLevel = nextLevel;
        }

        return currentLevel.get(0);
    }

    /**
     * Return the Merkle root hash. If the tree is empty, returns the hash of
     * an empty string.
     *
     * @return merkle root hash (hex string)
     */
    public String getMerkleRoot() {
        if (this.root == null) {
            return HashUtils.hashString("");
        } else {
            return root.hash;
        }
    }
}