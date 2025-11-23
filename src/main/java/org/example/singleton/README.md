## Thread-Safe Singleton

Implement various thread-safe singleton patterns and explain trade-offs.
Requirements:
- Implement: 
  - Eager initialization
  - Synchronized method
  - Double-checked locking
  - Bill Pugh (inner static class)
  - Enum singleton
- Explain pros/cons of each approach
- Discuss serialization concerns
- Handle reflection attacks

Key Concepts: Lazy initialization, double-checked locking, happens-before, volatile

---
### Handling Reflection Attacks

A reflection attack is a common way to break singleton patterns that rely on a private constructor to prevent instantiation.

#### What is the attack?

Java's Reflection API allows code to inspect and manipulate classes at runtime, including their private members. An attacker can use reflection to:
1.  Get the `Class` object of the singleton.
2.  Access its `private` constructor using `getDeclaredConstructor()`.
3.  Make the private constructor accessible by calling `setAccessible(true)`.
4.  Create a new instance using the constructor's `newInstance()` method.

This completely bypasses the logic in the `getInstance()` method and results in a second, unauthorized instance of the singleton class, breaking the core principle of the pattern.

#### How to Prevent It

The standard defense is to add a check inside the private constructor itself. The constructor can verify if an instance has already been created. If it has, it throws an exception to block the creation of a second instance.

**Example Logic:**
```java
private class MySingleton() {
    // Check if instance already exists
    if (INSTANCE != null) {
        throw new IllegalStateException("Singleton already constructed. Use getInstance() method.");
    }
    // ... proceed with normal initialization
}
```

#### Which Patterns are Vulnerable?

- **Vulnerable:** Eager Initialization, Synchronized Method, Double-Checked Locking, and the Bill Pugh (Inner Static Class) method are all vulnerable and require the constructor check mentioned above for full protection.
- **Immune:** The **Enum Singleton** pattern is inherently immune to reflection attacks. The JVM itself guarantees that enums cannot be instantiated via reflection, making it the most robust pattern against this specific threat.