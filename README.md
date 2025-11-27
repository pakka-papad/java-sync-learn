# Java Synchronization Playground

This repository contains toy implementations for classic synchronization problems in Java. It's intended as a learning resource to explore and understand different concurrency and synchronization techniques.

## Contents
- [1. Thread-Safe Bounded Buffer (Producer-Consumer)](#1-thread-safe-bounded-buffer-producer-consumer)
- [2. Thread-Safe Singleton](#2-thread-safe-singleton)
- [3. Semaphore-based Resource Pool](#3-semaphore-based-resource-pool)


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