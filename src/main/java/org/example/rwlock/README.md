## Read-Write Lock Implementation

Implement a custom read-write lock from scratch.  

Requirements:
- Multiple readers can hold the lock simultaneously
- Only one writer can hold the lock at a time
- Writers have priority over readers (to prevent writer starvation)
- No thread starvation
- Must handle interruption properly
- Supports lock downgrading

Key Concepts: Lock fairness, starvation prevention, reader-writer problem

--- 

## Architectural Note: Performance and AQS

This `ReentrantRWLock` implementation is built using Java's intrinsic monitors (`synchronized`, `wait`, `notifyAll`). It is a correct and fair, textbook implementation that demonstrates the core principles of a reader-writer lock.

For production environments demanding maximum performance under high contention, the standard approach is to use the `AbstractQueuedSynchronizer` (AQS) framework, which is the foundation for most locks in the `java.util.concurrent` package.

An AQS-based lock achieves higher throughput due to several advanced techniques:

1.  **Atomic State Management:** AQS avoids heavyweight `synchronized` blocks for uncontended lock acquisition. Instead, it uses lightweight atomic Compare-And-Swap (CAS) operations on a single `volatile int` that represents the lock's state (e.g., splitting the integer's bits to count readers and the writer's hold count).

2.  **Intelligent Thread Queuing:** AQS manages its own highly optimized wait queue. It avoids the "thundering herd" problem of `notifyAll()` by signaling only the specific thread(s) at the head of the queue that can acquire the lock next.

3.  **Thread-Local State:** To track re-entrant read counts, a `ThreadLocal` is typically used. This eliminates the contention and overhead of accessing a shared `HashMap` from multiple threads.

Title: The java.util.concurrent Synchronizer Framework <br>
Author: Doug Lea <br>
Link: http://gee.cs.oswego.edu/dl/papers/aqs.pdf <br>

