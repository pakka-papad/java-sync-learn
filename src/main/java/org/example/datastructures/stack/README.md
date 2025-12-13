## Thread-Safe Stack

Implement a thread-safe stack with multiple approaches.

Requirements:
- Version A: Using ReentrantLock
- Version B: Using AtomicReference (lock-free)
- Support: push(), pop(), peek(), size(), isEmpty()
- Pop from empty stack should throw exception or return null (specify behavior)
- Compare performance of all versions

Learning Goals: Basic synchronization, coarse-grained locking, introduction to CAS  

Key Considerations:
- Which operations need synchronization?
- Can size() be done without locking?
- How to handle concurrent push/pop?

---

### Challenges with Lock-Free Stacks (TrieberStack)

While the lock-free `TrieberStack` (Version B) can offer better performance under certain contention levels by avoiding kernel-level locking, it introduces its own set of complexities and potential problems:

1.  **The ABA Problem:** This is a classic issue in lock-free data structures that use compare-and-set (CAS). The problem occurs when a thread reads a value `A`, another thread changes it to `B` and then back to `A`, and the first thread's CAS operation succeeds because it still sees `A`. The first thread is unaware that the underlying data has been modified.
    *   **Scenario:**
        1. Thread T1 reads the `head` of the stack, which is node `A`.
        2. T1 is suspended.
        3. Thread T2 pops `A`, then pops `B`.
        4. T2 pushes a new node `A'` onto the stack. If memory is recycled, `A'` might be allocated at the exact same memory address as the original `A`. The stack's `head` now points to `A` again.
        5. T1 resumes and executes its CAS operation. It checks if the current `head` is still `A`. It is, so the CAS succeeds.
    *   **Result:** T1 has performed its operation (e.g., a `pop`) successfully, but it's completely unaware that node `B` was also popped in the meantime. This can lead to data loss or an inconsistent state. This particular stack implementation is susceptible to this issue. A common solution is to use an `AtomicStampedReference`, which pairs the reference with a version "stamp" or counter that is incremented on every update, turning the ABA problem into an AB*A* problem, which is far less likely.

2.  **Inaccurate `size()` Method:** The `size()` method, while using an `AtomicInteger`, is not linearizable with `push()` and `pop()`. An `incrementAndGet()` on `size` is not atomically bound to the `compareAndSet()` on the `head`. This means a call to `size()` can return a value that does not reflect the "true" state of the stack at a single point in time, especially during high contention. For many use cases, this "eventually consistent" size is acceptable, but it's not strictly correct.

3.  **High-Contention Performance:** Under very high contention, threads can spend significant time in "spin-loops," repeatedly trying and failing their CAS operations. This wastes CPU cycles and can even lead to performance degradation compared to a well-implemented lock-based alternative, as the constant creation of new `LNode` objects puts pressure on the garbage collector.