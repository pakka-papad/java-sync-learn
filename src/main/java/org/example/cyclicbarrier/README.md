## Barrier Synchronization (CyclicBarrier)

Implement a reusable barrier synchronization mechanism.
Requirements:
- N threads must wait at barrier
- Barrier releases all threads once N threads arrive
- Must be reusable (cyclic)
- Support optional barrier action
- Handle interruption and broken barriers

Key Concepts: Barrier synchronization, thread coordination, generation/epoch pattern