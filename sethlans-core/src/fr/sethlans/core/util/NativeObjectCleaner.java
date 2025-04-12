package fr.sethlans.core.util;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.ArrayList;
import java.util.List;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;

/**
 * <code>NativeObjectCleaner</code> uses an internal {@link Cleaner} to handle
 * the references of tracked objects and clean them when reclaimed by the GC.
 * This class is used internally by the {@link Allocator} to de-allocate native
 * {@link Buffer}.
 * 
 * @author GnosticOccultist
 * 
 * @see Allocator
 */
public final class NativeObjectCleaner {

    /**
     * The logger for the GC.
     */
    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.gc");
    /**
     * The reference object to serve as a lock for thread-safety.
     */
    private static final Object LOCK = new Object();
    /**
     * The cleaner used to clean objects when references are reclaimed.
     */
    private static Cleaner CLEANER = Cleaner.create();
    /**
     * The list of cleaning actions to be executed at the end of the frame.
     */
    private static final List<Runnable> CLEAN_ACTIONS = new ArrayList<>();
    /**
     * The list of all cleanables.
     */
    private static final List<Cleanable> CLEANABLES = new ArrayList<>();

    /**
     * Private constructor to inhibit instantiation of
     * <code>NativeObjectCleaner</code>.
     */
    private NativeObjectCleaner() {
    }

    /**
     * Registers the specified object to be referenced and cleaned, using the
     * provided {@link Runnable}, by the <code>NativeObjectCleaner</code> when no
     * longer used.
     * 
     * @param obj         The object to track (not null).
     * @param cleanAction The action to execute when the object is reclaimed by the
     *                    GC.
     * @return A cleanable to clean the object instantly.
     */
    public static Cleanable register(Object obj, Runnable cleanAction) {
        synchronized (LOCK) {
            logger.debug("Registered " + obj + ".");

            if (CLEANER == null) {
                CLEANER = Cleaner.create();
            }

            var cleanable = CLEANER.register(obj, cleanAction);
            CLEANABLES.add(cleanable);

            return cleanable;
        }
    }

    /**
     * Clean the objects marked as unused by the <code>NativeObjectCleaner</code>.
     */
    public static void cleanUnused() {
        if (CLEAN_ACTIONS.isEmpty()) {
            return;
        }

        synchronized (LOCK) {
            for (var action : CLEAN_ACTIONS) {
                action.run();
            }

            CLEAN_ACTIONS.clear();
        }
    }

    /**
     * Clean all previously registered objects by invoking their cleaning action.
     */
    public static void cleanAll() {
        if (CLEANABLES.isEmpty()) {
            return;
        }

        synchronized (LOCK) {
            for (var cleanable : CLEANABLES) {
                cleanable.clean();
            }

            CLEANABLES.clear();
        }

        cleanUnused();
    }
}