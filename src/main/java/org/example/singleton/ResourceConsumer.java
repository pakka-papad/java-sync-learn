package org.example.singleton;

public abstract class ResourceConsumer {


    /**
     * Pros: <br>
     * - Simple to implement. <br>
     * - Inherently thread-safe, as the instance is created at class loading time. <br>
     * - No synchronization overhead at runtime. <br>
     * Cons: <br>
     * - Not lazy initialization: The instance is created even if it's never used. This can waste resources if the
     * setup is expensive.
     */
    public static class EagerInit {
        private static final EagerInit INSTANCE = new EagerInit();

        public static EagerInit getInstance() {
            return INSTANCE;
        }

        private EagerInit() {
            if (INSTANCE != null) {
                throw new IllegalStateException("Singleton already constructed. Use getInstance() method.");
            }
        }
    }

    /**
     * Pros: <br>
     * - Simple to implement. <br>
     * - Provides lazy initialization: The instance is created only when needed. <br>
     * - Guarantees thread safety by synchronizing the entire method. <br>
     * Cons: <br>
     * - Performance overhead: Every call to getResource() has to acquire and release the lock, even after the instance
     * has been created. This can be a bottleneck in high-concurrency scenarios.
     */
    public static class SyncInit {

        private static SyncInit INSTANCE = null;

        public static synchronized SyncInit getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new SyncInit();
            }
            return INSTANCE;
        }

        private SyncInit() {
            if (INSTANCE != null) {
                throw new IllegalStateException("Singleton already constructed. Use getInstance() method.");
            }
        }
    }

    /**
     * Pros: <br>
     * - Provides lazy initialization: The instance is created only when needed. <br>
     * - Achieves thread safety with reduced synchronization overhead compared to the synchronized method, as the
     * lock is only acquired during the initial creation. <br>
     * Cons: <br>
     * - More complex to implement correctly (requires `volatile` keyword and precise ordering of checks). <br>
     * - Historically prone to issues in early Java versions due to memory model complexities (though fixed since
     * Java 5 with `volatile`).
     */
    public static class DoubleCheckedLocking {

        private static volatile DoubleCheckedLocking INSTANCE;

        public static DoubleCheckedLocking getInstance() {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            synchronized (DoubleCheckedLocking.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DoubleCheckedLocking();
                }
            }
            return INSTANCE;
        }

        private DoubleCheckedLocking() {
            if (INSTANCE != null) {
                throw new IllegalStateException("Singleton already constructed. Use getInstance() method.");
            }
        }
    }

    /**
     * Pros: <br>
     * - Provides lazy initialization: The instance is created only when the `getResource()` method is first called. <br>
     * - Inherently thread-safe: JVM guarantees thread safety during class initialization, so no explicit
     * synchronization is needed. <br>
     * - High performance: No synchronization overhead on subsequent calls to `getResource()`. <br>
     * - Simple and elegant: Considered one of the best approaches for thread-safe lazy initialization. <br>
     * Cons: <br>
     * - Can be broken by reflection (can be mitigated by checks in the constructor). <br>
     * - Can be broken by serialization (requires implementing `readResolve()` method). <br>
     */
    public static class InnerStaticClass {

        private static class ResourceHolder {
            static final InnerStaticClass INSTANCE = new InnerStaticClass();
        }

        public static InnerStaticClass getInstance() {
            return ResourceHolder.INSTANCE;
        }

        /**
         * The ResourceHolder.INSTANCE inside the private constructor will create the instance and the condition will
         * always be true. How does the check work? <br>
         * <br>
         * 1. <b>Class Initialization is Synchronized and Happens Only Once</b>:
         * The JVM guarantees that a class is initialized only one time.
         * When a thread starts initializing a class (like ResourceHolder), it acquires a lock.
         * Any other thread that needs to use that class will block until the first thread is finished. <br>
         * <br>
         * 2. <b>State of a Class *During* Initialization</b>:
         * When a thread is in the process of initializing a class, it is allowed to access the static members of that
         * class.
         * If it accesses a static field that is currently being initialized, it will see that field's default value
         * (null for an object). It does not re-trigger the initialization. <br>
         * <br>
         * This check prevents instantiation via reflection. It works because: <br>
         * 1. <b>Legitimate First Call</b>: When getInstance() is called for the first time,
         *    the ResourceHolder class is initialized. Its static field `INSTANCE` is created by
         *    calling `new InnerStaticClass()`. At this exact moment, inside the constructor,
         *    `ResourceHolder.INSTANCE` is seen as `null` (as assignment to the field hasn't completed). The check
         *    `if (ResourceHolder.INSTANCE != null)` is false, and no exception is thrown. <br>
         * <br>
         * 2. <b>Reflection Attack Call</b>: Later, an attacker uses reflection to call `new InnerStaticClass()` again.
         *    By this time, ResourceHolder is fully initialized, and `ResourceHolder.INSTANCE` holds the legitimate
         *    singleton object (it's not null). The check `if (ResourceHolder.INSTANCE != null)` is true,
         *    and an `IllegalStateException` is thrown, blocking the attack.
         */
        private InnerStaticClass() {
            // This check prevents instantiation via reflection.
            if (ResourceHolder.INSTANCE != null) {
                throw new IllegalStateException("Singleton already constructed. Use getInstance() method.");
            }
        }
    }

    /**
     * Pros: <br>
     * - Easiest to implement: Most concise way to write a singleton. <br>
     * - Inherently thread-safe: JVM guarantees thread safety during enum instance creation. <br>
     * - Serialization-safe: Handles serialization and deserialization correctly without extra effort. <br>
     * - Reflection-proof: Cannot be instantiated via reflection, making it the most robust against such attacks. <br>
     * Cons: <br>
     * - Not lazy initialization: The instance is created when the enum class is loaded, which is eager. <br>
     * - Less flexible: Cannot extend other classes (though it can implement interfaces). <br>
     */
    public static enum ResourceSingleEnum {
        INSTANCE;

        public static ResourceSingleEnum getInstance() {
            return ResourceSingleEnum.INSTANCE;
        }
    }
}
