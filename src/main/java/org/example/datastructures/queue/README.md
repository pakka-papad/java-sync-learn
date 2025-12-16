##  Thread-Safe Queue

Implement a thread-safe unbounded queue.

Requirements:
- Version A: Coarse-grained locking (single lock)
- Version B: Fine-grained locking (separate head/tail locks)
- Version C: Lock-free using AtomicReference
- Support: enqueue(), dequeue(), size()
- Use linked nodes internally
- Handle empty queue gracefully

Learning Goals: Fine-grained locking, lock-free linked structures, memory visibility

Key Considerations:
- Why can head and tail have separate locks?
- How to ensure happens-before relationship in lock-free version?
- What's the challenge with size() in fine-grained version?