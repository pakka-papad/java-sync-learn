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

---
### Serialization Concerns

Serialization is the process of converting an object's state into a byte stream, and deserialization is the reverse process. It's often used for persistence or inter-process communication.

#### How Serialization Breaks the Singleton Pattern

When a singleton object is serialized and then deserialized, the `readObject()` method used during deserialization will, by default, **always create a new instance** of the class from the byte stream. This new instance is created without invoking the `getInstance()` method, bypassing the singleton's control mechanisms. As a result, you end up with two distinct instances of your singleton in the application (the original one and the newly deserialized one), breaking the singleton guarantee.

#### How to Prevent It: The `readResolve()` Method

The Java Serialization API provides a special hook method, `private Object readResolve()`, to address this problem. If a `Serializable` class defines this method:
1. The JVM first creates a new instance during deserialization.
2. Immediately after this, it calls the `readResolve()` method on that newly created instance.
3. The object **returned by `readResolve()`** is then used as the final result of the deserialization process, and the newly created (but unwanted) instance from step 1 is discarded.

To protect your singleton, you implement `readResolve()` to simply return your single, true instance:

```java
public class MySingleton implements java.io.Serializable {
    // ... singleton implementation with getInstance() ...

    // This method is called during deserialization
    private Object readResolve() {
        // Return the one true instance and let the garbage collector
        // take care of the object just created during deserialization.
        return getInstance();
    }
}
```

#### Which Patterns are Vulnerable?

- **Vulnerable:** All patterns (Eager Initialization, Synchronized Method, Double-Checked Locking, and the Bill Pugh (Inner Static Class) method) are vulnerable if they implement `java.io.Serializable`. They require the `readResolve()` method to maintain their singleton property upon deserialization.
- **Immune:** The **Enum Singleton** pattern is inherently immune to serialization issues. The Java specification guarantees that the serialization and deserialization of enum constants will never create new instances, handling this automatically.