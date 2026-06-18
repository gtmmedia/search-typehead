# Viva Guide: Search Typeahead System

This guide contains 50 probable viva (interview) questions for the Search Typeahead System. Each question includes a detailed answer, tradeoffs, complexity analysis (where applicable), and failure scenarios.

## Core Data Structures & Algorithms

### 1. Why use a Trie over a HashMap for autocomplete?
**Answer**: A Trie allows for efficient prefix-based lookups, finding all strings sharing a common prefix. A HashMap only allows exact matches, meaning to find a prefix "iph", we would have to iterate over all keys in the map, which is inefficient.
**Tradeoffs**: Tries consume more memory due to object overhead for every character node compared to a flat string list in a HashMap.
**Complexity**: Time: $O(P + N)$ where $P$ is prefix length, $N$ is number of matches. Space: $O(W \times L)$ where $W$ is total words and $L$ is average length.
**Failure Scenarios**: Memory exhaustion if the dataset grows too large without eviction strategies.

### 2. How do you handle sorting suggestions by popularity in the Trie?
**Answer**: Each node representing the end of a word stores a `count` or `popularityScore`. After traversing to the node matching the given prefix, we perform a Depth First Search (DFS) or Breadth First Search (BFS) to gather all descendant words, and sort them based on their count before returning the top K results.
**Tradeoffs**: Sorting during read time adds latency. Alternatively, caching the top K at each node increases memory but makes reads $O(1)$.
**Complexity**: $O(N \log N)$ to sort $N$ descendants.
**Failure Scenarios**: Heavy CPU load if a prefix is very short (e.g., "a") and has millions of descendants to sort on every request.

### 3. What happens if we use a SQL Database with `LIKE 'prefix%'` instead of a Trie?
**Answer**: While a relational database can use a B-Tree index for prefix matching, it incurs network overhead, disk I/O, and query parsing delays. An in-memory Trie operates entirely in RAM and bypasses network and DB connection overheads, providing sub-millisecond responses.
**Tradeoffs**: DB offers persistence and ACID properties. Trie in memory risks data loss on crash.
**Complexity**: DB search involves index traversal $O(\log N)$ plus disk access latency.
**Failure Scenarios**: Database connections can saturate during traffic spikes, leading to system-wide cascading failures.

### 4. How would you support fuzzy matching or typos in the Trie?
**Answer**: Fuzzy matching can be implemented by traversing multiple paths in the Trie while maintaining a Levenshtein distance budget. If the user types "aple" with a budget of 1, we can branch out to check "apple" simultaneously.
**Tradeoffs**: Drastically increases search space and CPU utilization, slowing down queries.
**Complexity**: Exponential relative to the edit distance allowed.
**Failure Scenarios**: CPU starvation if too many concurrent users trigger fuzzy searches on deep tree paths.

### 5. Why not use an inverted index (like Elasticsearch)?
**Answer**: Inverted indexes map terms to documents, which is ideal for full-text search. A Trie specifically maps prefixes to whole words, which is highly optimized for typeahead. While ES has edge n-gram support, a custom Trie is leaner and faster for this specific narrowly defined problem.
**Tradeoffs**: Building a custom Trie means reinventing features ES already has (persistence, clustering).
**Complexity**: ES indexing is $O(L)$, but query parsing adds massive overhead compared to direct memory access.
**Failure Scenarios**: Custom Trie might lack the robustness of distributed systems like Elasticsearch in large-scale deployments.

## Distributed Caching & Consistent Hashing

### 6. Why use Consistent Hashing instead of Modulo Hashing?
**Answer**: Modulo hashing (`hash(key) % N`) causes massive key redistribution when nodes are added or removed (N changes). Consistent hashing maps keys and nodes to a ring. When a node is added/removed, only the keys belonging to that specific segment are reassigned.
**Tradeoffs**: Consistent hashing is slightly more complex to implement and requires virtual nodes to ensure balanced data distribution.
**Complexity**: Routing takes $O(\log K)$ using a TreeMap, where $K$ is the number of nodes on the ring.
**Failure Scenarios**: "Hot spots" if keys aren't uniformly distributed or if virtual nodes aren't utilized.

### 7. What happens on a Cache Miss?
**Answer**: The system falls back to querying the primary Trie structure. Once the results are fetched, they are asynchronously (or synchronously) written to the assigned cache node with a TTL (Time-To-Live) so subsequent requests hit the cache.
**Tradeoffs**: Synchronous cache updates add latency to the current request. Asynchronous updates might result in multiple cache misses for the same key.
**Complexity**: Cache lookup is $O(1)$, fallback is $O(P+N)$.
**Failure Scenarios**: Cache Stampede (Thundering Herd) if a highly popular key expires and thousands of requests simultaneously hit the Trie.

### 8. What happens if a Cache Node dies?
**Answer**: With consistent hashing, the keys mapped to the dead node automatically fall back to the next node clockwise on the ring. The next node will initially experience cache misses and will lazily populate its cache from the Trie.
**Tradeoffs**: Temporary latency spike for keys previously hosted on the dead node.
**Complexity**: Re-routing happens in $O(\log K)$.
**Failure Scenarios**: If the next node is already near capacity, the sudden influx of new keys and requests could overwhelm it, causing cascading failures.

### 9. How do you ensure balanced load across cache nodes?
**Answer**: By using "Virtual Nodes" (vnodes). Instead of placing node A on the ring once, we hash `nodeA_1`, `nodeA_2`, ..., `nodeA_V` and place it multiple times. This interleaves nodes and ensures an even distribution of keys.
**Tradeoffs**: Increases the size of the routing table (TreeMap) slightly and hashing overhead during initialization.
**Complexity**: Time complexity for routing slightly increases, memory increases by a factor of V.
**Failure Scenarios**: Incorrect hashing algorithms could still lead to clustering of vnodes.

### 10. Why use a TTL on cached results?
**Answer**: TTL ensures that stale data is eventually evicted. In typeahead, trends change rapidly. A static cache would serve outdated top suggestions.
**Tradeoffs**: Shorter TTL improves freshness but lowers cache hit rate. Longer TTL increases hit rate but serves stale data.
**Complexity**: $O(1)$ to check TTL during reads.
**Failure Scenarios**: Memory leaks if eviction policies are flawed or TTLs are set too high.

## High-Throughput Writes & Batching

### 11. Why use Batching for writes?
**Answer**: If every keystroke or search submission immediately updates the Trie, it requires constant locking (mutexes) of the data structure. This blocks read requests, destroying read performance. Batching queues writes and applies them periodically in bulk, drastically reducing lock contention.
**Tradeoffs**: Introduces eventual consistency. A user's search won't appear in suggestions instantly.
**Complexity**: Enqueueing is $O(1)$. Batch processing is $O(B)$ where $B$ is batch size.
**Failure Scenarios**: Queue overflow if the batch writer cannot keep up with the incoming request rate.

### 12. What happens if the Batch Writer crashes?
**Answer**: If the batch writer thread crashes, items will remain in the queue. The queue will eventually hit its maximum capacity.
**Tradeoffs**: System must choose whether to drop new requests, block the API, or spool to disk when queue is full.
**Complexity**: Queue memory grows linearly until crash.
**Failure Scenarios**: Complete write outage and Out-Of-Memory (OOM) errors if the queue is unbounded.

### 13. How does the system aggregate duplicates in a batch?
**Answer**: Before writing to the Trie, the batch writer drains the queue into a HashMap, counting occurrences. E.g., `["java", "java", "react"]` becomes `{"java": 2, "react": 1}`. It then performs two writes instead of three.
**Tradeoffs**: Uses extra CPU and memory during the aggregation step.
**Complexity**: $O(B)$ to drain and group, where $B$ is batch size.
**Failure Scenarios**: If all queries in a batch are unique, aggregation yields no write-reduction benefit.

### 14. What type of queue is used for batching?
**Answer**: A thread-safe, concurrent queue like `ConcurrentLinkedQueue` or `ArrayBlockingQueue` in Java.
**Tradeoffs**: Linked structures risk garbage collection overhead. Array structures have fixed capacity.
**Complexity**: $O(1)$ enqueue and dequeue.
**Failure Scenarios**: Unbounded queues lead to OOM. Bounded queues lead to rejected requests (HTTP 503/429) if full.

### 15. How do you determine the batch interval or threshold?
**Answer**: It is tuned based on traffic. Often, a dual-trigger is used: write when the queue size reaches `MAX_BATCH_SIZE` OR when `BATCH_INTERVAL_MS` expires, whichever happens first.
**Tradeoffs**: Small intervals cause high CPU usage. Large intervals cause high data staleness.
**Complexity**: $O(1)$ check on size, timer interruption.
**Failure Scenarios**: Timer drift or GC pauses delaying the batch execution.

## Trending & Ranking

### 16. How does trending ranking work?
**Answer**: Trending uses a composite score combining all-time popularity and recent velocity. Formula: `Score = (W1 * TotalCount) + (W2 * RecentCount)`. `RecentCount` is decayed periodically.
**Tradeoffs**: More complex to calculate and maintain than simple historical counts.
**Complexity**: Scoring is $O(1)$ per node during traversal.
**Failure Scenarios**: Unbalanced weights might cause permanent top-ranking of historically popular terms (e.g., "facebook"), masking new trends.

### 17. Why do we need a decay mechanism?
**Answer**: Without decay, temporary viral spikes (e.g., "super bowl 2024") would permanently alter `RecentCount`, staying in the trending list forever. Decaying (e.g., halving the count every minute) ensures spikes cool off over time.
**Tradeoffs**: Requires a background scheduled job to iterate over trending data.
**Complexity**: $O(K)$ where $K$ is the number of items in the trending cache.
**Failure Scenarios**: If decay job crashes, old trends persist forever.

### 18. How do you store trending data?
**Answer**: Trending data can be maintained in a separate Min-Heap or Top-K data structure that is updated asynchronously whenever the Trie is updated.
**Tradeoffs**: Duplicates data storage but provides $O(1)$ read access to the global trending list.
**Complexity**: Updates to Top-K heap take $O(\log K)$.
**Failure Scenarios**: Concurrency issues if multiple batch writers try to update the heap simultaneously.

### 19. How do you prevent bots from manipulating trends?
**Answer**: Implementing rate-limiting per IP/User, tracking unique users rather than raw counts, and using CAPTCHAs for anomalous spikes.
**Tradeoffs**: Adds latency to the write path and complexity to the architecture.
**Complexity**: Rate limiting checks are $O(1)$ using Redis or token buckets.
**Failure Scenarios**: Distributed Botnets using rotating IPs bypass simple rate limits.

### 20. What is the difference between autocomplete and trending?
**Answer**: Autocomplete provides completions for a specific user-typed prefix. Trending provides globally popular search terms regardless of prefix, usually displayed when the search bar is focused but empty.

## Scale & Resilience

### 21. How would you scale the Trie if it exceeds RAM?
**Answer**: Sharding the Trie based on the first character (e.g., Server 1 gets A-M, Server 2 gets N-Z). 
**Tradeoffs**: Cross-server routing adds complexity. Unbalanced shards if 'S' is heavily used and 'X' is rare.
**Complexity**: Shard routing is $O(1)$.
**Failure Scenarios**: Node failure results in the loss of an entire alphabetic shard unless replicated.

### 22. What happens if the primary datacenter goes down?
**Answer**: A multi-region active-active or active-passive deployment with geo-routing. Writes must be asynchronously replicated via Kafka or a distributed DB to the secondary region.
**Tradeoffs**: High cost and potential replication lag.
**Failure Scenarios**: Split-brain syndrome if regions lose connectivity to each other.

### 23. How do you handle cold starts?
**Answer**: On reboot, the in-memory Trie is empty. We must hydrate it by reading from a persistent datastore, daily log dumps, or a snapshot file before accepting traffic.
**Tradeoffs**: High startup time.
**Complexity**: Hydration takes $O(W \times L)$ where $W$ is total vocabulary.
**Failure Scenarios**: Serving traffic before hydration completes results in 100% cache misses and poor UX.

### 24. Why is debouncing important on the frontend?
**Answer**: Debouncing (e.g., 300ms) prevents the UI from firing an API request on every single keystroke. It waits until the user pauses typing.
**Tradeoffs**: Slight perceived lag before suggestions appear.
**Failure Scenarios**: Without debouncing, the backend is DDOSed by partial keystrokes.

### 25. How do you monitor system health?
**Answer**: Track metrics like p95 latency, cache hit rate, queue size, batch write reduction percentage, and memory usage. Alert on anomalies (e.g., hit rate drops below 50%).
**Tradeoffs**: Metrics collection adds slight overhead.
**Failure Scenarios**: Silent failures if monitoring daemons crash.

## Deeper Dives & Edge Cases

### 26. How do you handle multi-word prefixes? (e.g. "new york")
**Answer**: The Trie treats spaces as characters. "new york" is processed identically to "newyork", just traversing a space character node.

### 27. What if the user types quickly and responses arrive out of order?
**Answer**: Frontend race conditions. A request for "ap" might return after "app". The UI should use cancellation tokens (AbortController in JS) to ignore outdated responses.

### 28. Should we cache empty results?
**Answer**: Yes. If "asdfghjkl" has zero results, caching it prevents the backend from repeatedly traversing the Trie for a known dead end.

### 29. How do you handle profanity filtering?
**Answer**: Maintain a blocklist. Filter on the write-path (never add to Trie) or read-path (filter before returning). Write-path is more efficient.

### 30. How do you update the system without downtime?
**Answer**: Blue/Green deployments or Rolling Updates. Since state is in-memory, draining connections and hydrating new nodes before putting them in load balancer rotation is critical.

### 31. What is the impact of GC pauses on your latency?
**Answer**: In Java, "Stop-The-World" Garbage Collection pauses halt the application. Tuning the JVM (using G1GC or ZGC) and minimizing object creation (reusing nodes/buffers) reduces tail latency.

### 32. Why not use a standard Map in Java for the Trie Nodes' children?
**Answer**: `HashMap` has memory overhead and autoboxing issues. For a small fixed alphabet (e.g., 26 lowercase English letters), an array `Node[26]` is faster and more memory-efficient.

### 33. How does network latency affect perceived autocomplete speed?
**Answer**: If server processing is 1ms but network latency is 150ms, the user feels it's slow. Solutions include CDN edge caching and keeping the server geographically close to users.

### 34. What happens to the cache ring when you double the nodes?
**Answer**: Consistent hashing ensures that only $1/(N+M)$ keys are moved. This minimizes cache invalidation compared to modulo hashing where almost all keys would move.

### 35. Can a malicious user poison the autocomplete?
**Answer**: Yes, by repeatedly querying a rare word, driving up its count. Mitigation involves rate limiting and IP clustering detection.

### 36. How to test the consistency of the Batch Writer?
**Answer**: Use multithreaded integration tests asserting that after submitting N requests, waiting for the batch timer, the Trie count exactly matches N.

### 37. How do you represent uppercase and lowercase?
**Answer**: Typically, all inputs are normalized to lowercase on both read and write paths to simplify the Trie and consolidate counts.

### 38. How is the Trie's DFS bounded to avoid slow queries?
**Answer**: Return early. Stop the DFS once $K$ (e.g., 10) words have been found, or limit the maximum depth to prevent infinite loops in malformed graphs.

### 39. What happens if multiple batch writers try to run at once?
**Answer**: Use scheduling locks (e.g., ShedLock or database locks) to ensure only one instance of the batch job runs per cluster if they write to a shared datastore. For local Tries, `synchronized` blocks.

### 40. Why expose metrics via an endpoint?
**Answer**: Allows monitoring tools like Prometheus to scrape the service and Grafana to visualize it, enabling automated alerts.

### 41. How does the frontend handle API errors?
**Answer**: Catch block in the `axios` call, setting an `error` state, and gracefully displaying an empty dropdown or an error toast instead of crashing the UI.

### 42. How do you implement keyboard navigation in the dropdown?
**Answer**: Maintain a state variable `activeIndex`. `ArrowDown` increments it, `ArrowUp` decrements. Apply a CSS class to the highlighted item. `Enter` key sets the search bar value.

### 43. Why use Axios over Fetch API?
**Answer**: Axios automatically parses JSON, rejects promises on HTTP errors (e.g., 404, 500), and allows easy interception/configuration for timeouts.

### 44. What happens if the Trie becomes a bottleneck on reads?
**Answer**: Increase the Cache cluster size, or horizontally scale the application instances, placing a Load Balancer in front.

### 45. What is the memory footprint of an empty Trie vs 1 million words?
**Answer**: Empty is tiny (just root node). 1 million words depend on character overlap, but each node has overhead (pointers, arrays, boolean flags), which can total hundreds of MBs in Java.

### 46. How do you handle unicode characters/emojis?
**Answer**: If using a fixed Array `Node[26]`, it breaks. Must switch to a `HashMap<Character, Node>` to support the massive unicode space.

### 47. What happens if `POST /search` is synchronous instead of batched?
**Answer**: The user waits until the write lock is acquired, Trie is updated, and lock released. Under heavy load, requests queue up, timing out the client.

### 48. Why track write reduction percentage?
**Answer**: It validates the effectiveness of the batching strategy. A 0% reduction means batching is useless; 90% means it's saving the Trie massive amounts of lock contention.

### 49. How do you deploy this locally vs in production?
**Answer**: Locally: `npm run dev` and `mvn spring-boot:run`. Production: Build static frontend files, serve via Nginx/CDN, package backend as a Docker container, deploy to K8s.

### 50. If you had 6 months more, what would you add?
**Answer**: Deep Learning based personalization (LTR - Learning to Rank), real-time distributed persistence (Kafka + Cassandra), spelling correction, and geo-spatial trend filtering.
