## Thread-Safe Priority Queue

Implement a thread-safe priority queue (min-heap or max-heap).

Requirements:
- Version A: Coarse-grained locking (synchronized on entire heap)
- Version B: Fine-grained locking (lock individual heap levels or subtrees)
- Support: insert(), extractMin()/extractMax(), peek(), size()
- Maintain heap property during concurrent operations
- Handle structural modifications atomically

Learning Goals: Complex invariants under concurrency, hierarchical locking

Key Considerations:
- What's the minimum unit that needs locking?
- How to prevent deadlock in fine-grained version?
- How do heapify operations work with concurrent access?