import sun.plugin2.util.NativeLibLoader;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface Executor {
    class BlockMetrics {

        public Integer startPosition;

        public Integer blockLength;

        public BlockMetrics(Integer sPos, Integer len) {
            this.startPosition = sPos;
            this.blockLength = len;
        }
    }

    /**
     * Types that provider can transmit
     */
    enum APPROPRIATE_TYPES {
        CHAR, BYTE, DOUBLE
    }
    /**
     * The method to get state of worker
     * @return is worker ready to work
     */
    boolean isReady();
    /**
     * The function for reading the configuration file of the executor.
     *
     * @param config - name of configuration file
     * @return error code
     */
    int setConfig(String config);

    /**
     * The function for saving input stream
     *
     * @param input - opened input stream
     */
    void setInput(DataInputStream input);

    /**
     * The function for saving output stream
     *
     * @param output - opened output stream
     */
    void setOutput(DataOutputStream output);

    /**
     * The provider calls the consumer's methods 'getConsumedTypes', 'setAdapter' and retains the reference to the consumer.
     *
     * @param consumer - reference to the consumer
     */
    int setConsumer(Executor consumer);

    /**
     * Sets provider's 'ready' flag to true
     * @param provider current provider
     */
    void setPReady(Executor provider);

    /**
     * Sets consumer's 'ready' flag to true
     * @param consumer current consumer
     */
    void setCReady(Executor consumer);

    /**
     * Sets provider's 'stop' flag to true
     * @param provider current provider
     */
    void setEnd(Executor provider);

     /**
     * Consumer returns types that he can get
     *
     * @return array of these types
     */
    APPROPRIATE_TYPES[] getConsumedTypes();


    /**
     * Consumer returns BlockMetrics he works with
     *
     * @return class object with two integer fields
     */
    BlockMetrics getBlockMetrics();

    /**
     * The provider calls the consumer's method to set inner class
     *
     * @param provider - reference to the provider
     * @param adapter  - reference to the inner class of the provider
     * @param type     - appropriate type of connection between provider and consumer
     */
    void setAdapter(Executor provider, Object adapter, APPROPRIATE_TYPES type);



    /**
     * The method for getting adapter from provider
     * @param type - type of connection
     * @param blockMetrics - block metrics of consumer / current worker
     * @return adapter for transferring of data
     */
    Object getAdapter(APPROPRIATE_TYPES type, BlockMetrics blockMetrics);

    /**
     * The function for running executor
     *
     * @return error code
     */
    int run();

    /**
     * Provider calls this method of consumer when ends processing his data
     *
     * @param provider - reference to the provider
     * @return error code
     */
    int put(Executor provider);
}

// error code: 0 - success, another - error
