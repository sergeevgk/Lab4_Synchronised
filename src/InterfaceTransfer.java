import javafx.util.Pair;

public interface InterfaceTransfer {
    /**
     * Getting format of current transfer class
     *
     * @return - the format of transfer
     */
    Executor.APPROPRIATE_TYPES GetFormat();

    /**
     * Setting the current executor metrics function
     *
     * @param Off - offset in array
     * @param Len - length of block
     */
    default void SetMetrics(Integer Off, Integer Len) {
    }

    /**
     * Getting the current executor metrics function
     *
     * @return - the metrics
     */
    default Pair<Integer, Integer> GetMetrics() {
        return new Pair<>(0, 0);
    }
}