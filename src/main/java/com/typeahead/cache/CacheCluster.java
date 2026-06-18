package com.typeahead.cache;

import com.typeahead.hash.ConsistentHashRing;
import com.typeahead.model.QueryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the distributed cache nodes and delegates routing
 * to the ConsistentHashRing.
 */
@Component
public class CacheCluster {

    private static final Logger log = LoggerFactory.getLogger(CacheCluster.class);

    private final ConsistentHashRing hashRing;
    
    // Keep track of active physical nodes for broadcast invalidation
    private final Map<String, CacheNode> activeNodes = new ConcurrentHashMap<>();

    public CacheCluster() {
        this.hashRing = new ConsistentHashRing(3); // 3 virtual nodes per physical node
        
        // Initialize with 3 physical nodes
        addNode("CacheNode-1");
        addNode("CacheNode-2");
        addNode("CacheNode-3");
    }

    public void addNode(String nodeId) {
        CacheNode node = new CacheNode(nodeId);
        activeNodes.put(nodeId, node);
        hashRing.addNode(node);
    }

    public void removeNode(String nodeId) {
        CacheNode removed = hashRing.removeNode(nodeId);
        if (removed != null) {
            activeNodes.remove(nodeId);
            removed.clear();
        }
    }

    public CacheEntry get(String prefix) {
        CacheNode node = hashRing.getNode(prefix);
        if (node != null) {
            return node.get(prefix);
        }
        return null;
    }

    public void put(String prefix, List<QueryData> suggestions, long ttlSeconds) {
        CacheNode node = hashRing.getNode(prefix);
        if (node != null) {
            node.put(prefix, new CacheEntry(prefix, suggestions, ttlSeconds));
        }
    }

    /**
     * Broadcasts invalidation to all nodes since a query update
     * can affect prefixes hashed to any node in the cluster.
     */
    public void invalidateRelated(String query) {
        for (CacheNode node : activeNodes.values()) {
            node.invalidateRelated(query);
        }
    }
    
    public CacheNode getNodeForPrefix(String prefix) {
        return hashRing.getNode(prefix);
    }
    
    public int getHashForPrefix(String prefix) {
        return hashRing.hash(prefix);
    }
    
    public List<String> getActiveNodeIds() {
        return new ArrayList<>(activeNodes.keySet());
    }
}
