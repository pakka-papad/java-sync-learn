## Thread-Safe Bounded Buffer (Producer-Consumer)

Implement a thread-safe bounded buffer that supports multiple producers and consumers.
Requirements:
- Fixed capacity buffer
- Producers block when buffer is full
- Consumers block when buffer is empty
- Must support concurrent operations
- Implement using: 
  - synchronized/wait/notify
  - ReentrantLock with Condition

Key Concepts: Wait/notify, condition variables, producer-consumer pattern
