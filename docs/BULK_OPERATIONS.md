# Bulk Metadata Operations - Design Document

## Overview

Bulk operations enable parallel processing of large metadata operations to improve performance and handle API governor limits effectively.

## Current State vs. Desired State

### Current (v2.1)
- ✅ Single WSDL retrieval per call
- ✅ Sequential web service detection
- ❌ No parallelization
- ❌ No batch processing
- ❌ No API limit management

### Desired (v2.3+)
- ✅ Parallel metadata retrieval
- ✅ Bulk deployment operations
- ✅ Thread pool management
- ✅ Automatic batching
- ✅ API governor limit handling
- ✅ Progress tracking

---

## Use Cases

### 1. Large Org Backup
**Scenario:** Backup an org with 10,000+ components

**Current Approach:**
```bash
# Takes 30+ minutes, single-threaded
java -jar metadata.jar retrieve --all --output ./backup/
```

**Bulk Approach:**
```bash
# Takes 5-8 minutes with 10 concurrent batches
java -jar metadata.jar bulk-retrieve --all --concurrency 10 --output ./backup/
```

**Performance Gain:** 4-6x faster

---

### 2. Multi-Package Deployment
**Scenario:** Deploy 20 packages to 5 different sandboxes

**Current Approach:**
```bash
# Sequential: 20 packages × 5 orgs = 100 sequential deploys
# ~300 minutes (5 hours)
for package in pkg*/; do
  for org in sb1 sb2 sb3 sb4 sb5; do
    java -jar metadata.jar deploy --source $package --org $org
  done
done
```

**Bulk Approach:**
```bash
# Parallel: 10 concurrent deploys
# ~40 minutes
java -jar metadata.jar bulk-deploy \
  --packages pkg*/ \
  --orgs sb1,sb2,sb3,sb4,sb5 \
  --concurrency 10
```

**Performance Gain:** 7-8x faster

---

### 3. Metadata Inventory Across Multiple Orgs
**Scenario:** Generate inventory for 50 sandboxes

**Bulk Approach:**
```bash
java -jar metadata.jar bulk-inventory \
  --orgs-file sandboxes.txt \
  --concurrency 20 \
  --output inventory/
```

**Result:** Complete in 10-15 minutes vs. 2+ hours sequential

---

## Technical Design

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Bulk Operations Manager               │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────────┐    ┌──────────────┐   ┌────────────┐  │
│  │  Request    │───▶│ Work Queue   │──▶│ Scheduler  │  │
│  │  Validator  │    │ (Priority)   │   │            │  │
│  └─────────────┘    └──────────────┘   └────────────┘  │
│                                              │          │
│                                              ▼          │
│  ┌──────────────────────────────────────────────────┐  │
│  │          Thread Pool Executor                    │  │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐   │  │
│  │  │Worker 1│ │Worker 2│ │Worker 3│ │Worker N│   │  │
│  │  └────────┘ └────────┘ └────────┘ └────────┘   │  │
│  └──────────────────────────────────────────────────┘  │
│                                              │          │
│  ┌───────────────────────┐    ┌─────────────▼───────┐  │
│  │  API Limit Tracker    │◀───│  Result Aggregator  │  │
│  │  (Rate Limiter)       │    │                     │  │
│  └───────────────────────┘    └─────────────────────┘  │
│                                              │          │
│  ┌───────────────────────┐                  ▼          │
│  │  Progress Monitor     │    ┌──────────────────────┐ │
│  │  (Callbacks/Events)   │◀───│  Error Handler       │ │
│  └───────────────────────┘    │  (Retry Logic)       │ │
│                                └──────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Core Classes

#### 1. BulkOperationManager
```java
public class BulkOperationManager {
    private final ExecutorService executorService;
    private final ApiLimitTracker limitTracker;
    private final ProgressMonitor progressMonitor;
    
    /**
     * Execute operations in bulk with concurrency control.
     */
    public <T, R> BulkResult<R> executeBulk(
        List<T> inputs,
        Function<T, R> operation,
        BulkOptions options
    ) {
        // Implementation
    }
    
    /**
     * Batch operations automatically based on API limits.
     */
    public <T, R> BulkResult<R> executeBatched(
        List<T> inputs,
        Function<List<T>, List<R>> batchOperation,
        int batchSize
    ) {
        // Implementation
    }
}
```

#### 2. BulkOptions
```java
public class BulkOptions {
    // Concurrency settings
    private int maxConcurrency = 10;
    private int queueSize = 100;
    
    // API limit management
    private boolean respectApiLimits = true;
    private int maxApiCallsPerMinute = 1000;
    private Duration rateLimitWindow = Duration.ofMinutes(1);
    
    // Retry configuration
    private int maxRetries = 3;
    private RetryStrategy retryStrategy = RetryStrategy.EXPONENTIAL_BACKOFF;
    private Duration initialRetryDelay = Duration.ofSeconds(5);
    
    // Progress tracking
    private boolean enableProgressTracking = true;
    private Consumer<ProgressEvent> progressCallback;
    
    // Error handling
    private ErrorHandlingStrategy errorStrategy = ErrorHandlingStrategy.CONTINUE_ON_ERROR;
    private Consumer<OperationError> errorCallback;
    
    // Getters and setters...
}
```

#### 3. ApiLimitTracker
```java
public class ApiLimitTracker {
    private final RateLimiter rateLimiter;
    private final Map<String, AtomicInteger> apiCallCounts;
    
    /**
     * Check if operation can proceed without exceeding limits.
     */
    public boolean canProceed(String operationType) {
        return rateLimiter.tryAcquire() && 
               !isNearingDailyLimit(operationType);
    }
    
    /**
     * Record an API call.
     */
    public void recordApiCall(String operationType) {
        apiCallCounts.get(operationType).incrementAndGet();
    }
    
    /**
     * Get current API usage statistics.
     */
    public ApiUsageStats getUsageStats() {
        // Implementation
    }
}
```

#### 4. BulkResult
```java
public class BulkResult<R> {
    private final List<R> successes;
    private final List<OperationError> failures;
    private final Duration totalDuration;
    private final ApiUsageStats apiUsage;
    
    public boolean isFullySuccessful() {
        return failures.isEmpty();
    }
    
    public boolean isPartiallySuccessful() {
        return !successes.isEmpty() && !failures.isEmpty();
    }
    
    public double getSuccessRate() {
        int total = successes.size() + failures.size();
        return total == 0 ? 0.0 : (double) successes.size() / total;
    }
}
```

---

## Specific Operations

### Bulk Retrieve

```java
public class BulkRetrieve {
    
    /**
     * Retrieve multiple metadata types in parallel.
     */
    public BulkResult<RetrieveResult> retrieveTypes(
        List<String> metadataTypes,
        SessionContext sessionContext,
        BulkOptions options
    ) {
        BulkOperationManager manager = new BulkOperationManager(options);
        
        return manager.executeBulk(
            metadataTypes,
            type -> retrieveSingleType(type, sessionContext),
            options
        );
    }
    
    /**
     * Retrieve from multiple orgs in parallel.
     */
    public BulkResult<RetrieveResult> retrieveFromMultipleOrgs(
        RetrieveRequest request,
        List<SessionContext> orgs,
        BulkOptions options
    ) {
        BulkOperationManager manager = new BulkOperationManager(options);
        
        return manager.executeBulk(
            orgs,
            org -> executeRetrieve(request, org),
            options
        );
    }
    
    /**
     * Batch retrieve - automatically split large requests.
     */
    public BulkResult<RetrieveResult> batchRetrieve(
        List<String> componentNames,
        String metadataType,
        SessionContext sessionContext,
        int batchSize
    ) {
        // Split into batches
        List<List<String>> batches = Lists.partition(componentNames, batchSize);
        
        BulkOperationManager manager = new BulkOperationManager();
        
        return manager.executeBatched(
            batches,
            batch -> retrieveBatch(batch, metadataType, sessionContext),
            batchSize
        );
    }
}
```

### Bulk Deploy

```java
public class BulkDeploy {
    
    /**
     * Deploy to multiple orgs in parallel.
     */
    public BulkResult<DeployResult> deployToMultipleOrgs(
        File sourceDir,
        List<SessionContext> targetOrgs,
        DeployOptions deployOptions,
        BulkOptions bulkOptions
    ) {
        byte[] zipFile = createDeploymentZip(sourceDir);
        
        BulkOperationManager manager = new BulkOperationManager(bulkOptions);
        
        return manager.executeBulk(
            targetOrgs,
            org -> deploy(zipFile, org, deployOptions),
            bulkOptions
        );
    }
    
    /**
     * Deploy multiple packages to single org.
     */
    public BulkResult<DeployResult> deployMultiplePackages(
        List<File> sourceDirs,
        SessionContext targetOrg,
        DeployOptions deployOptions,
        BulkOptions bulkOptions
    ) {
        BulkOperationManager manager = new BulkOperationManager(bulkOptions);
        
        return manager.executeBulk(
            sourceDirs,
            dir -> deployPackage(dir, targetOrg, deployOptions),
            bulkOptions
        );
    }
    
    /**
     * Matrix deployment - multiple packages to multiple orgs.
     */
    public Map<String, BulkResult<DeployResult>> matrixDeploy(
        List<File> packages,
        List<SessionContext> orgs,
        DeployOptions deployOptions,
        BulkOptions bulkOptions
    ) {
        Map<String, BulkResult<DeployResult>> results = new ConcurrentHashMap<>();
        
        for (SessionContext org : orgs) {
            BulkResult<DeployResult> orgResult = deployMultiplePackages(
                packages, org, deployOptions, bulkOptions
            );
            results.put(org.getOrgId(), orgResult);
        }
        
        return results;
    }
}
```

### Bulk List

```java
public class BulkList {
    
    /**
     * List multiple metadata types in parallel.
     */
    public Map<String, List<FileProperties>> listMultipleTypes(
        List<String> metadataTypes,
        SessionContext sessionContext,
        BulkOptions options
    ) {
        BulkOperationManager manager = new BulkOperationManager(options);
        
        Map<String, List<FileProperties>> results = new ConcurrentHashMap<>();
        
        BulkResult<Pair<String, List<FileProperties>>> bulkResult = 
            manager.executeBulk(
                metadataTypes,
                type -> Pair.of(type, listMetadata(type, sessionContext)),
                options
            );
        
        bulkResult.getSuccesses().forEach(pair -> 
            results.put(pair.getKey(), pair.getValue())
        );
        
        return results;
    }
    
    /**
     * Generate inventory for multiple orgs.
     */
    public Map<String, OrgInventory> bulkInventory(
        List<SessionContext> orgs,
        BulkOptions options
    ) {
        BulkOperationManager manager = new BulkOperationManager(options);
        
        Map<String, OrgInventory> inventories = new ConcurrentHashMap<>();
        
        BulkResult<Pair<String, OrgInventory>> bulkResult = 
            manager.executeBulk(
                orgs,
                org -> Pair.of(org.getOrgId(), generateInventory(org)),
                options
            );
        
        bulkResult.getSuccesses().forEach(pair ->
            inventories.put(pair.getKey(), pair.getValue())
        );
        
        return inventories;
    }
}
```

---

## API Limit Management

### Salesforce API Limits

| Limit Type | Concurrent | Per Day | Per Hour |
|------------|-----------|---------|----------|
| API Calls (Enterprise) | - | 100,000 | - |
| Metadata API Deployments | 10 | - | - |
| Metadata API Retrievals | 10 | - | - |
| Bulk API Batches | - | 10,000 | - |

### Rate Limiting Strategy

```java
public class RateLimitStrategy {
    
    /**
     * Token bucket algorithm for smooth rate limiting.
     */
    public static RateLimiter createTokenBucket(int maxRate, Duration window) {
        return RateLimiter.create(maxRate, window);
    }
    
    /**
     * Adaptive rate limiting based on API usage.
     */
    public static AdaptiveRateLimiter createAdaptive(ApiLimitTracker tracker) {
        return new AdaptiveRateLimiter(tracker) {
            @Override
            protected double calculateRate() {
                ApiUsageStats stats = tracker.getUsageStats();
                
                // Slow down as we approach limits
                double dailyUsagePercent = stats.getDailyUsagePercent();
                if (dailyUsagePercent > 0.9) {
                    return maxRate * 0.5; // 50% rate
                } else if (dailyUsagePercent > 0.75) {
                    return maxRate * 0.75; // 75% rate
                }
                
                return maxRate; // Full speed
            }
        };
    }
}
```

---

## Error Handling & Retry

### Retry Strategies

```java
public enum RetryStrategy {
    /**
     * Fixed delay between retries.
     */
    FIXED_DELAY,
    
    /**
     * Exponential backoff: delay doubles each retry.
     */
    EXPONENTIAL_BACKOFF,
    
    /**
     * Exponential backoff with jitter to avoid thundering herd.
     */
    EXPONENTIAL_BACKOFF_WITH_JITTER,
    
    /**
     * No retry - fail immediately.
     */
    NO_RETRY
}
```

### Error Handling

```java
public class ErrorHandler {
    
    /**
     * Determine if error is retryable.
     */
    public static boolean isRetryable(Throwable error) {
        return error instanceof TimeoutException ||
               error instanceof SocketTimeoutException ||
               error instanceof ConnectException ||
               (error instanceof ApiException && 
                ((ApiException) error).getCode() == 429); // Rate limit
    }
    
    /**
     * Calculate retry delay with exponential backoff.
     */
    public static Duration calculateRetryDelay(
        int attemptNumber,
        Duration initialDelay,
        boolean useJitter
    ) {
        long baseDelay = initialDelay.toMillis() * (1L << (attemptNumber - 1));
        
        if (useJitter) {
            // Add random jitter: 0-25% of base delay
            long jitter = (long) (baseDelay * 0.25 * Math.random());
            baseDelay += jitter;
        }
        
        // Cap at 5 minutes
        return Duration.ofMillis(Math.min(baseDelay, Duration.ofMinutes(5).toMillis()));
    }
}
```

---

## Progress Tracking

### Progress Events

```java
public class ProgressEvent {
    private final String operationId;
    private final int totalItems;
    private final int completedItems;
    private final int failedItems;
    private final Duration elapsed;
    private final Duration estimatedRemaining;
    
    public double getProgressPercent() {
        return (double) completedItems / totalItems * 100;
    }
    
    public String getProgressBar() {
        int percent = (int) getProgressPercent();
        int bars = percent / 2; // 50 character bar
        return "[" + "=".repeat(bars) + " ".repeat(50 - bars) + "] " + percent + "%";
    }
}
```

### Progress Monitoring

```java
public interface ProgressMonitor {
    
    /**
     * Called when operation starts.
     */
    void onStart(String operationId, int totalItems);
    
    /**
     * Called on each item completion.
     */
    void onItemComplete(String operationId, Object result);
    
    /**
     * Called on each item failure.
     */
    void onItemFailed(String operationId, Throwable error);
    
    /**
     * Called when all items complete.
     */
    void onComplete(String operationId, BulkResult<?> result);
    
    /**
     * Get current progress.
     */
    ProgressEvent getProgress(String operationId);
}
```

### Console Progress Display

```java
public class ConsoleProgressMonitor implements ProgressMonitor {
    
    @Override
    public void onItemComplete(String operationId, Object result) {
        ProgressEvent progress = getProgress(operationId);
        
        // Update console with ANSI escape codes
        System.out.print("\r" + progress.getProgressBar() + 
                        " | " + progress.getCompletedItems() + "/" + progress.getTotalItems() +
                        " | ETA: " + formatDuration(progress.getEstimatedRemaining()));
    }
}
```

---

## Configuration Examples

### YAML Configuration

```yaml
bulk-operations:
  # Thread pool settings
  concurrency:
    max-threads: 10
    queue-size: 100
    keep-alive-seconds: 60
  
  # API limit management
  api-limits:
    respect-limits: true
    max-calls-per-minute: 1000
    adaptive-rate-limiting: true
    slowdown-threshold: 0.75  # Slow down at 75% usage
  
  # Retry configuration
  retry:
    max-retries: 3
    strategy: EXPONENTIAL_BACKOFF_WITH_JITTER
    initial-delay-seconds: 5
    max-delay-minutes: 5
  
  # Progress tracking
  progress:
    enabled: true
    update-interval-ms: 500
    show-eta: true
  
  # Error handling
  errors:
    strategy: CONTINUE_ON_ERROR
    log-failures: true
    collect-errors: true
```

### Programmatic Configuration

```java
BulkOptions options = BulkOptions.builder()
    .maxConcurrency(10)
    .respectApiLimits(true)
    .maxApiCallsPerMinute(1000)
    .retryStrategy(RetryStrategy.EXPONENTIAL_BACKOFF_WITH_JITTER)
    .maxRetries(3)
    .progressCallback(event -> {
        System.out.println("Progress: " + event.getProgressPercent() + "%");
    })
    .errorCallback(error -> {
        logger.error("Operation failed: " + error.getMessage());
    })
    .build();
```

---

## Performance Benchmarks (Projected)

### Metadata Retrieval

| Operation | Sequential | Bulk (10 threads) | Speedup |
|-----------|-----------|-------------------|---------|
| Retrieve 100 types | 50 min | 8 min | 6.25x |
| Full org backup (5000 items) | 120 min | 18 min | 6.67x |
| Multi-org backup (10 orgs) | 500 min | 65 min | 7.69x |

### Deployment

| Operation | Sequential | Bulk (5 threads) | Speedup |
|-----------|-----------|------------------|---------|
| Deploy to 10 sandboxes | 150 min | 35 min | 4.29x |
| Deploy 20 packages | 200 min | 45 min | 4.44x |
| Matrix: 10 pkgs × 5 orgs | 500 min | 95 min | 5.26x |

---

## Implementation Plan

### Phase 1: Foundation (2 weeks)
- [ ] BulkOperationManager core
- [ ] Thread pool management
- [ ] Basic error handling
- [ ] Unit tests

### Phase 2: API Management (1 week)
- [ ] ApiLimitTracker
- [ ] Rate limiting
- [ ] Adaptive throttling
- [ ] Integration tests

### Phase 3: Progress & Monitoring (1 week)
- [ ] ProgressMonitor interface
- [ ] Console progress display
- [ ] Event callbacks
- [ ] Metrics collection

### Phase 4: Operations (2 weeks)
- [ ] BulkRetrieve
- [ ] BulkDeploy
- [ ] BulkList
- [ ] End-to-end tests

### Phase 5: Polish (1 week)
- [ ] Configuration support (YAML/JSON)
- [ ] Documentation
- [ ] Examples
- [ ] Performance tuning

**Total:** ~7 weeks

---

## References

- [Salesforce Metadata API Limits](https://developer.salesforce.com/docs/atlas.en-us.api_meta.meta/api_meta/meta_limits.htm)
- [Java Concurrency Utilities](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/package-summary.html)
- [Guava RateLimiter](https://github.com/google/guava/wiki/CachesExplained#refresh)
