## CountDownLatch Alternative

Implement a mechanism similar to CountDownLatch using other primitives (no using CountDownLatch itself).
Requirements:
- Count down from N to 0
- Threads can wait for count to reach 0
- Once zero, cannot be reset (unlike CyclicBarrier)
- Use synchronized/wait/notify or Lock/Condition

Key Concepts: Coordination, latches, thread signaling