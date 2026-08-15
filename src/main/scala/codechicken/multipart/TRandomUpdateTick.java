package codechicken.multipart;

/**
 * Interface for parts with random update ticks. Used in conjunction with TickScheduler.
 * <p>
 * The Scala trait extended TMultiPart, but a trait extending a class is a bare interface in bytecode, so implementors
 * must extend TMultiPart themselves.
 */
public interface TRandomUpdateTick {

    /**
     * Called on random update. Random ticks are between 800 and 1600 ticks from their last scheduled/random tick.
     */
    void randomUpdate();

    /**
     * Registers the part for random ticking. This cannot be a default method: TMultiPart declares onWorldJoin, and a
     * superclass method always beats an interface default, so the default would never run. Every implementor must
     * declare it and call {@link TickScheduler#loadRandomTick(TRandomUpdateTick)} itself.
     *
     * <pre>
     * 
     * &#64;Override
     * public void onWorldJoin() {
     *     TickScheduler.loadRandomTick(this);
     * }
     * </pre>
     */
    void onWorldJoin();
}
