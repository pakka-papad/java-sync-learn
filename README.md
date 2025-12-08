# Java Synchronization Playground

This repository contains toy implementations for classic synchronization problems in Java. It's intended as a learning resource to explore and understand different concurrency and synchronization techniques.

## Contents
- [1. Thread-Safe Bounded Buffer (Producer-Consumer)](#1-thread-safe-bounded-buffer-producer-consumer)
- [2. Thread-Safe Singleton](#2-thread-safe-singleton)
- [3. Semaphore-based Resource Pool](#3-semaphore-based-resource-pool)
- [4. CountDownLatch Alternative](#4-countdownlatch-alternative)
- [5. Copy-On-Write List](#5-copy-on-write-list)
- [6. Reentrant Read-Write Lock](#6-reentrant-read-write-lock)
- [7. Reusable Barrier (CyclicBarrier)](#7-reusable-barrier-cyclicbarrier)


---

## 1. Thread-Safe Bounded Buffer (Producer-Consumer)

Implementation: [`src/main/java/org/example/boundedbuffer`](./src/main/java/org/example/boundedbuffer)

Implement a thread-safe bounded buffer that supports multiple producers and consumers.

### Requirements
- Fixed capacity buffer
- Producers block when the buffer is full
- Consumers block when the buffer is empty
- Must support concurrent operations from multiple threads

### Key Concepts
- Producer-Consumer Pattern
- `wait()` / `notify()` for coordination
- Condition Variables (`java.util.concurrent.locks.Condition`)

### Implementations

#### a. `CircularBoundedBuffer.java`
- **Technique**: Uses `synchronized` methods with `wait()` and `notifyAll()`.
- **Description**: A simple array-based circular buffer. Intrinsic locks on the object itself are used to ensure mutual exclusion. `wait()` is called to block producers/consumers, and `notifyAll()` is used to wake them up when the buffer state changes.

#### b. `LinkedBoundedBuffer.java`
- **Technique**: Uses `java.util.concurrent.locks.ReentrantLock` with `Condition` variables.
- **Description**: A more flexible implementation using explicit locks. It uses two separate `Condition` objects (`notFull` and `notEmpty`) which is more efficient than `notifyAll()` because it allows waking up only the relevant threads (e.g., waking up a producer when space becomes available, not a consumer).

---

## 2. Thread-Safe Singleton

Implementation: [`src/main/java/org/example/singleton`](./src/main/java/org/example/singleton)

Implement various thread-safe singleton patterns and understand their trade-offs.

### Requirements
- Ensure only one instance of a class is ever created.
- Provide a global point of access to that instance.
- Handle challenges from concurrency, serialization, and reflection.

### Key Concepts
- Lazy Initialization vs. Eager Initialization
- Double-Checked Locking
- Happens-before relationship
- `volatile` keyword

### Implementations & Trade-offs

#### a. Eager Initialization (`EagerInit`)
- **Pros**: Simple to implement. Inherently thread-safe because the instance is created at class loading time. No runtime synchronization overhead.
- **Cons**: Not lazy initialization. The instance is created even if it's never used, which can be wasteful if the setup is expensive.

#### b. Synchronized Method (`SyncInit`)
- **Pros**: Simple to implement. Provides lazy initialization. Guarantees thread safety by synchronizing the entire `getInstance()` method.
- **Cons**: Significant performance overhead. Every call to `getInstance()` acquires and releases the lock, which can be a bottleneck in high-concurrency scenarios even after the instance is created.

#### c. Double-Checked Locking (`DoubleCheckedLocking`)
- **Pros**: Provides lazy initialization. Achieves thread safety with reduced overhead, as the lock is only acquired during the initial creation.
- **Cons**: Complex to implement correctly. It **requires** the `volatile` keyword to prevent memory consistency errors (fixed since Java 5).

#### d. Bill Pugh (Inner Static Class) Pattern (`InnerStaticClass`)
- **Pros**: Provides lazy initialization. Inherently thread-safe because the JVM handles class initialization locking. High performance with no synchronization overhead on subsequent calls.
- **Cons**: Vulnerable to reflection and serialization attacks without extra protection.

#### e. Enum Singleton (`ResourceSingleEnum`)
- **Pros**: The most concise and robust approach. Inherently thread-safe, serialization-safe, and reflection-proof, all guaranteed by the JVM.
- **Cons**: Eager initialization (instance is created when the enum class is loaded). Less flexible, as enums cannot extend other classes.

---

## 3. Semaphore-based Resource Pool

Implementation: [`src/main/java/org/example/resourcepool`](./src/main/java/org/example/resourcepool)

Implement a generic, thread-safe resource pool using a `Semaphore` to control access. This is a common pattern for managing a limited number of resources like database connections or expensive objects.

### Requirements
- Fixed pool size
- Acquire/release resources
- Block when no resources are available
- Support a timeout when acquiring a resource
- Handle resource validation upon release
- Track the number of available resources

### Key Concepts
- Resource Pooling
- `java.util.concurrent.Semaphore` for controlling access
- Timeout Handling

### Implementations

#### a. `BlockingResourcePool.java`
- **Technique**: Uses a `java.util.concurrent.Semaphore` to manage a fixed number of permits, corresponding to the available resources.
- **Description**: A generic implementation that holds resources in a `ConcurrentLinkedQueue`. The `Semaphore` controls blocking and unblocking of threads trying to acquire resources. This is more efficient and straightforward for pool-like structures than using `wait()`/`notify()` because the semaphore handles the "counting" of available resources internally.

---

## 4. CountDownLatch Alternative

Implementation: [`src/main/java/org/example/countdownlatch`](./src/main/java/org/example/countdownlatch)

Implement a mechanism similar to CountDownLatch using other primitives (no using CountDownLatch itself).

### Requirements
- Count down from N to 0
- Threads can wait for count to reach 0
- Once zero, cannot be reset (unlike CyclicBarrier)
- Use synchronized/wait/notify or Lock/Condition

### Key Concepts
- Coordination
- Latches
- Thread signaling
- `java.util.concurrent.locks.ReentrantLock`
- `java.util.concurrent.locks.Condition`
- `volatile` keyword for performance optimization

### Implementations

#### a. `AltCDLatch.java`
- **Technique**: Uses `java.util.concurrent.locks.ReentrantLock` with `Condition` variables and `volatile` for the count.
- **Description**: An alternative implementation of a CountDownLatch. It provides `await()` methods for threads to wait until the count reaches zero, and a `countDown()` method to decrement the count. The `volatile` keyword on the count field, combined with `ReentrantLock` and `Condition` variables, ensures correct synchronization and enables a fast-path check for performance.

---

## 5. Copy-On-Write List

Implementation: [`src/main/java/org/example/cowlist`](./src/main/java/org/example/cowlist)

Implement a simplified version of `java.util.concurrent.CopyOnWriteArrayList`. This data structure is useful for read-heavy scenarios where the number of reads and iterations vastly outnumbers writes.

### Requirements
- Thread-safe reads without locking
- Copy array on every write operation
- Support `add`, `remove`, `get`, and `iterator`
- Iterator is a "snapshot" and never throws `ConcurrentModificationException`

### Key Concepts
- Copy-on-write
- Snapshot iteration
- Immutability
- Read-heavy workloads
- `volatile` keyword

### Implementations

#### a. `CoWList.java`
- **Technique**: Uses a `volatile` array for the underlying data. Write operations (`add`, `remove`) are synchronized and create a full copy of the array. Read operations (`get`, `iterator`) are lock-free and operate on the current `volatile` snapshot of the array.
- **Trade-offs**:
    - **Pros**: Excellent for read-heavy workloads. Iteration is fast and completely safe from `ConcurrentModificationException`. Reads do not require any locking.
    - **Cons**: Writes are very expensive due to copying the entire array. This makes it unsuitable for write-heavy or even moderately write-intensive scenarios. High memory consumption if the list is large and modified often.

---

## 6. Reentrant Read-Write Lock

Implementation: [`src/main/java/org/example/rwlock`](./src/main/java/org/example/rwlock)

A custom implementation of a reentrant read-write lock, demonstrating core principles and addressing common concurrency challenges.

### Requirements
- Multiple readers can hold the lock simultaneously
- Only one writer can hold the lock at a time
- Writers have priority over readers (to prevent writer starvation)
- No thread starvation
- Must handle interruption properly
- Supports lock downgrading
- Reentrant for both read and write locks

### Key Concepts
- Reader-Writer Problem
- Reentrancy
- Writer Preference
- Lock Downgrading
- `synchronized`, `wait()`, `notifyAll()`

### Implementations

#### a. `ReentrantRWLock.java`
- **Technique**: Uses Java's intrinsic monitors (`synchronized`, `wait`, `notifyAll`).
- **Description**: A custom, fair implementation that allows multiple concurrent readers or a single exclusive writer. It correctly handles reentrancy for both read and write locks, supports lock downgrading (acquiring a read lock while holding a write lock), implements writer priority to prevent starvation, and handles thread interruption during acquisition attempts.

---

## 7. Reusable Barrier (CyclicBarrier)

Implementation: [`src/main/java/org/example/cyclicbarrier`](./src/main/java/org/example/cyclicbarrier)

Implement a reusable barrier synchronization mechanism.

### Requirements
- N threads must wait at barrier
- Barrier releases all threads once N threads arrive
- Must be reusable (cyclic)
- Support optional barrier action
- Handle interruption and broken barriers

### Key Concepts
- Barrier synchronization
- Thread coordination
- Generation/epoch pattern

### Implementations

#### a. `AltCyclicBarrier.java`
- **Technique**: Uses `java.util.concurrent.locks.ReentrantLock` with a `Condition` variable and a `Generation` inner class.
- **Description**: A custom implementation of a cyclic barrier. It uses an explicit `ReentrantLock` and a single `Condition` variable (`gateOpen`) for all threads to wait on. A private `Generation` class is used to manage different cycles of the barrier and to track its 'broken' state, which is essential for correct reusability and handling of interruptions or timeouts.