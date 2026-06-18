# Search Typeahead System

## Project Overview
The Search Typeahead System is a high-performance, distributed backend and modern React frontend application designed to provide sub-millisecond search autocomplete suggestions, track trending searches, and handle high-throughput search volumes.

## Architecture
The system consists of a modern React frontend communicating with a Spring Boot backend. 

Key architectural components include:
1. **Trie Data Structure**: A custom, thread-safe Prefix Tree optimized for fast prefix matching ($O(P + N)$).
2. **Consistent Hashing & Cache Cluster**: Distributes requests across a simulated in-memory distributed cache cluster to minimize latency and ensure high cache hit rates.
3. **Batch Writes**: Handles massive write volumes asynchronously. Searches are queued and aggregated in batches to prevent locking issues on the core Trie structure.
4. **Trending Service**: Maintains dual-ranking modes (historical vs. trending) with a decay mechanism to surface both all-time popular queries and recent viral spikes.

*(For detailed architectural diagrams and explanations, refer to `architecture.md`)*

## APIs

### 1. `GET /suggest?q={prefix}`
Fetches autocomplete suggestions based on the provided prefix.
- **Response**: `{"query": "ipho", "suggestions": ["iphone", "iphone 13", ...]}`

### 2. `POST /search`
Records a completed search query.
- **Payload**: `{"query": "iphone"}`
- **Response**: `202 Accepted`

### 3. `GET /trending?mode={trending|historical}`
Retrieves top search trends.
- **Response**: `[{"word": "iphone", "score": 95.5, "totalCount": 100}, ...]`

### 4. `GET /metrics`
Returns system performance metrics (latency, cache hit rate, write reduction).

### 5. `GET /batch/stats`
Returns batch processing statistics.

## Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+ & npm

### Running the Backend (Spring Boot)
1. Navigate to the project root directory.
2. Build the project using Maven:
   ```bash
   ./mvnw clean install
   ```
3. Run the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```
4. The backend will start on `http://localhost:8080`.

### Running the Frontend (React + Vite)
1. Navigate to the `frontend` directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. Open the application in your browser at `http://localhost:5173` (or the port specified by Vite).

## Performance Metrics
The system tracks and exposes the following metrics:
- **Average Latency**: Tracks the mean response time for suggestion queries.
- **p50 & p95 Latency**: Percentile-based latency metrics to monitor tail latency.
- **Cache Hit / Miss Rate**: Evaluates the efficiency of the consistent hashing cache.
- **Write Reduction %**: Measures the efficiency of the Batch Writer in deduplicating writes before persisting them to the Trie.

## Tradeoffs
- **Eventual Consistency vs. Strong Consistency**: Batch writing means that newly searched terms will not instantly appear in suggestions until the background batch job processes the queue. This is a deliberate tradeoff for high write throughput.
- **In-Memory Storage vs. Persistence**: The current system stores the Trie and Cache in-memory for sub-millisecond latency. A crash would result in data loss unless an external persistent store (like Redis/Cassandra) is introduced.
- **Cache Staleness**: Cached items have a Time-To-Live (TTL). Highly volatile trends might take a few seconds to bypass the cache and update.

## Future Improvements
- **Persistent Storage**: Integrate Redis or a NoSQL database (Cassandra) for persistence and crash recovery.
- **Fuzzy Matching / Typo Tolerance**: Enhance the Trie with Levenshtein distance algorithms to support typo-tolerant autocomplete (e.g., "ihpone" -> "iphone").
- **Personalization**: Adjust suggestion rankings based on user context, location, or search history.
- **Horizontal Scaling**: Deploy the application across multiple server instances using a real distributed cache (like Redis Cluster) instead of the simulated in-memory cluster.
