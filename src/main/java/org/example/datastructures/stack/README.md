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