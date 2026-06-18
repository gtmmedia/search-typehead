package com.typeahead.hash;

import com.typeahead.cache.CacheNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Consistent Hash Ring for distributed cache routing.
 *
 * Requirements:
 * - Ring size = 360
 * - Multiple virtual nodes
 * - Add node / Remove node
 * - Clockwise lookup
 * - Minimal key movement
 */
public class ConsistentHashRing {

    private static final Logger log = LoggerFactory.getLogger(ConsistentHashRing.class);

    public static final int RING_SIZE = 360;
    private final int virtualNodeCount;

    private final TreeMap<Integer, CacheNode> ring;
    private final Map<String, List<Integer>> nodePositions;

    public ConsistentHashRing(int virtualNodeCount) {
        this.virtualNodeCount = virtualNodeCount;
        this.ring = new TreeMap<>();
        this.nodePositions = new HashMap<>();
        log.info("ConsistentHashRing initialized: ringSize={}, virtualNodesPerNode={}", RING_SIZE, virtualNodeCount);
    }

    public synchronized void addNode(CacheNode node) {
        String nodeId = node.getNodeId();
        if (nodePositions.containsKey(nodeId)) {
            return;
        }

        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < virtualNodeCount; i++) {
            String virtualKey = nodeId + "-vn" + i;
            int position = hash(virtualKey);

            // Handle collision
            while (ring.containsKey(position)) {
                position = (position + 1) % RING_SIZE;
            }

            ring.put(position, node);
            positions.add(position);
        }

        nodePositions.put(nodeId, positions);
        log.info("Added node '{}' with virtual positions: {}", nodeId, positions);
    }

    public synchronized CacheNode removeNode(String nodeId) {
        List<Integer> positions = nodePositions.remove(nodeId);
        if (positions == null) {
            return null;
        }

        CacheNode removedNode = null;
        for (int position : positions) {
            CacheNode node = ring.remove(position);
            if (node != null) {
                removedNode = node;
            }
        }
        log.info("Removed node '{}' from ring", nodeId);
        return removedNode;
    }

    public CacheNode getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }

        int position = hash(key);
        Map.Entry<Integer, CacheNode> entry = ring.ceilingEntry(position);

        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    public int hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));

            int hashValue = ((digest[0] & 0xFF) << 24)
                          | ((digest[1] & 0xFF) << 16)
                          | ((digest[2] & 0xFF) << 8)
                          | (digest[3] & 0xFF);

            return Math.abs(hashValue) % RING_SIZE;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
