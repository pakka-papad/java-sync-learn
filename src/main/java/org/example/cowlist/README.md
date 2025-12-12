## Copy-On-Write List

Implement a simplified version of CopyOnWriteArrayList.  
Requirements:
- Thread-safe reads without locking
- Copy array on every write operation
- Support add, remove, get, and iterator
- Iterator is snapshot and never throws ConcurrentModificationException
- Discuss trade-offs vs Collections.synchronizedList
- When is this data structure appropriate?

Key Concepts: Copy-on-write, snapshot iteration, immutability, read-heavy workloads