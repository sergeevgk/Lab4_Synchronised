import javafx.util.Pair;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.*;

public class ExecutorImpl implements Executor {
    private Map<GrammarWorkerHuffman, String> configWorker;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Map<Byte, String> huffmanTable;

    private Object dataStorageTemp;
    private Object dataStorage;
    private int dataStoragePos;

    private Map<Executor, APPROPRIATE_TYPES> providers;
    private Map<Executor, Integer[]> consumerMap;
    /**
     * Map with providers
     * Key:   Provider
     * Value: Adapter, that links current consumer and provider
     */
    private HashMap<Executor, InterfaceTransfer> _providers;
    private Object tempAdapter;
    private Map<Executor, Object> adapters;
    private APPROPRIATE_TYPES[] availableTypesIn;
    private APPROPRIATE_TYPES[] availableTypesOut;

    private boolean _isReadyToGet;
    private boolean _isReadyToSend;

    // Value of sleeping period in milliseconds
    private int _sleepTime;


    ExecutorImpl() {
        configWorker = new EnumMap<>(GrammarWorkerHuffman.class);
        consumerMap = new LinkedHashMap<>();
        this.providers = new LinkedHashMap<>();
        this.availableTypesIn = new APPROPRIATE_TYPES[3];
        this.availableTypesOut = new APPROPRIATE_TYPES[3];
        adapters = new HashMap<>();
        providers = new HashMap<>();
    }

    /**
     * Reads configuration file of worker and sets necessary parameters
     *
     * @param config - name of configuration file
     * @return error code
     */
    public int setConfig(String config) {
        ConfigInterpreterWorkerParameters workerInterp = new ConfigInterpreterWorkerParameters(config);
        if (workerInterp.readConfiguration(configWorker) != 0) {
            Log.report("Error occurred during worker ctor process.");
            return -1;
        }
        String delimiter = configWorker.get(GrammarWorkerHuffman.DELIMITER);
        int counter = 0;
        for (String typeIn : this.configWorker.get(GrammarWorkerHuffman.TYPES_IN).split(delimiter)) {
            if (typeIn.equals("byte")) {
                this.availableTypesIn[counter++] = APPROPRIATE_TYPES.BYTE;
            } else if (typeIn.equals("double")) {
                this.availableTypesIn[counter++] = APPROPRIATE_TYPES.DOUBLE;
            } else if (typeIn.equals("char")) {
                this.availableTypesIn[counter++] = APPROPRIATE_TYPES.CHAR;
            }
        }
        counter = 0;
        for (String typeOut : this.configWorker.get(GrammarWorkerHuffman.TYPES_OUT).split(delimiter)) {
            if (typeOut.equals("byte")) {
                this.availableTypesOut[counter++] = APPROPRIATE_TYPES.BYTE;
            } else if (typeOut.equals("double")) {
                this.availableTypesOut[counter++] = APPROPRIATE_TYPES.DOUBLE;
            } else if (typeOut.equals("char")) {
                this.availableTypesOut[counter++] = APPROPRIATE_TYPES.CHAR;
            }
        }
        return 0;
    }

    public void setInput(DataInputStream inputStream) {
        this.inputStream = inputStream;
        return;
    }

    public void setOutput(DataOutputStream outputStream) {
        this.outputStream = outputStream;
        return;
    }

    /**
     * @return consumed types
     */
    public APPROPRIATE_TYPES[] getConsumedTypes() {
        return this.availableTypesIn;
    }

    /**
     * Sets consumer for current (this) provider and considers currentType for adapter
     *
     * @param consumer - reference to the consumer
     * @return
     */
    public int setConsumer(Executor consumer) {
        this.consumerMap.put(consumer, new Integer[2]);
        APPROPRIATE_TYPES currentType;
        for (APPROPRIATE_TYPES type : consumer.getConsumedTypes()) {
            for (APPROPRIATE_TYPES thisType : this.availableTypesOut) {
                if (type == thisType) {
                    currentType = type;
                    if (currentType == APPROPRIATE_TYPES.BYTE) {
                        //adapters.put(this, new ByteTransfer());
                        this.tempAdapter = new ByteTransfer();
                    } else if (currentType == APPROPRIATE_TYPES.DOUBLE) {
                        this.tempAdapter = new DoubleTransfer();
                        //adapters.put(this, new DoubleTransfer());
                    } else if (currentType == APPROPRIATE_TYPES.CHAR) {
                        this.tempAdapter = new CharTransfer();
                        //adapters.put(this, new CharTransfer());
                    } else {
                        this.tempAdapter = null;
                        //adapters.put(this, null);
                    }
                    consumer.setAdapter(this, tempAdapter, currentType);
                    setCReady(consumer);
                    return 0;
                }
            }
        }

        return -1;
    }

    /**
     * Sets adapter for consumer, when we already have created adapter of currentType in provider
     *
     * @param provider - reference to the provider
     * @param adapter  - reference to the inner class of the provider
     * @param type     - appropriate type of connection between provider and consumer
     */
    public void setAdapter(Executor provider, Object adapter, APPROPRIATE_TYPES type) {
        Integer startPos = Integer.parseInt(configWorker.get(GrammarWorkerHuffman.START_POS));
        Integer requestedLength = Integer.parseInt(configWorker.get(GrammarWorkerHuffman.REQUESTED_LENGTH));
        ((InterfaceTransfer) adapter).SetMetrics(startPos, requestedLength);
        this.adapters.put(provider, adapter);
        this.providers.put(provider, type);
    }

    /**
     * Builds huffman table for input file and writes it to file, which name is contained in configuration file
     */
    private void buildHuffmanTable() {
        Map<Byte, String> huffmanTable;
        HuffmanTableBuilder huffmanTableBuilder = HuffmanTableBuilder.getInstance();
        if ((huffmanTable = huffmanTableBuilder.buildHuffmanTable()) == null) {
            return;
        }
        HuffmanTableWriter.getInstance().writeHuffmanTable(huffmanTable);
        this.huffmanTable = huffmanTable;
    }

    /**
     * Reads huffman table from file, which name is contained in configuration file
     */
    private void readHuffmanTable() {
        this.huffmanTable = new HashMap<>();
        HuffmanTableReader.getInstance().init(configWorker.get(GrammarWorkerHuffman.HUFFMAN_TABLE));
        HuffmanTableReader.getInstance().readHuffmanTable(this.huffmanTable);
    }

    /**
     * Runs worker's coding process
     *
     * @return error code
     */
    public int run() {
        HuffmanAlgorithm algorithm;
        if (this.inputStream != null && this.outputStream != null) {
            buildHuffmanTable();
            algorithm = new HuffmanAlgorithm(configWorker, huffmanTable);
            if (processFromToFile(algorithm) != 0)
                return -1;
        } else if (this.inputStream != null) {
            buildHuffmanTable();
            algorithm = new HuffmanAlgorithm(configWorker, huffmanTable);
            if (processFromFile(algorithm) != 0)
                return -1;
        } else if (this.outputStream != null) {
            readHuffmanTable();
            algorithm = new HuffmanAlgorithm(configWorker, huffmanTable);
            if (processToFile(algorithm) != 0)
                return -1;
        } else {
            readHuffmanTable();
            algorithm = new HuffmanAlgorithm(configWorker, huffmanTable);
            if (processProviderConsumer(algorithm) != 0)
                return -1;
        }
        return 0;
    }

    /**
     * Method for those workers, who read input file and write to output file
     *
     * @param algorithm - algorithm which worker is using to encode / decode byte array
     * @return error code
     */
    private int processFromToFile(HuffmanAlgorithm algorithm) {
        MyFileReader fileReader = new MyFileReader(configWorker, inputStream);
        MyFileWriter fileWriter = new MyFileWriter(configWorker, outputStream);
        HuffmanAlgorithmResult res = null;
        int bufferSize = Integer.parseInt(configWorker.get(GrammarWorkerHuffman.BUFFER_SIZE));
        byte[] buffer;
        while ((buffer = fileReader.readInputFile(bufferSize)) != null) {
            res = algorithm.startProcess(buffer, 0);
            if (res == null) {
                return -1;
            }
            fileWriter.writeOutputFile(res.getResult());
        }
        return 0;
    }

    /**
     * Method for those workers, who read input file and write to output file
     *
     * @param algorithm - algorithm which worker is using to encode / decode byte array
     * @return error code
     */
    private int processFromFile(HuffmanAlgorithm algorithm) {
        int selfBufferSize = Integer.parseInt(configWorker.get(GrammarWorkerHuffman.BUFFER_SIZE));
        MyFileReader fileReader = new MyFileReader(configWorker, inputStream);
        byte[] buffer;
        HuffmanAlgorithmResult res;
        DataConverterHuffman converter = new DataConverterHuffman();
        while ((buffer = fileReader.readInputFile(selfBufferSize)) != null) {
            dataStorage = buffer;//byte[]
            dataStorage = converter.wrapArray((byte[]) dataStorage);
            for (Executor sub : consumerMap.keySet()) {
                if (sub.put(this) == 0) {
                    sub.run();
                }
                this.dataStoragePos = 0;
            }
        }
        return 0;
    }

    /**
     * Method for those workers, who get data from other workers and write to output file
     *
     * @param algorithm - algorithm which worker is using to encode / decode byte array
     * @return error code
     */
    private int processToFile(HuffmanAlgorithm algorithm) {//просто напечатать
        int bufferSize = Integer.parseInt(configWorker.get(GrammarWorkerHuffman.BUFFER_SIZE));
        MyFileWriter fileWriter = new MyFileWriter(configWorker, outputStream);
        if (dataStorage == null) {
            return 0;
        }
        for (int writtenLen = 0; writtenLen < ((byte[]) this.dataStorage).length; writtenLen += bufferSize) {
            int size = ((byte[]) this.dataStorage).length < bufferSize ? ((byte[]) this.dataStorage).length : bufferSize;
            byte[] output = new byte[size];
            System.arraycopy((byte[]) this.dataStorage, writtenLen, output, 0, size);
            fileWriter.writeOutputFile(output);
        }
        //fileWriter.writeOutputFile((byte[])this.dataStorage);
        return 0;
    }

    /**
     * Method for those workers, who get data from some workers(providers) and put data to other workers(consumers)
     *
     * @param algorithm - algorithm which worker is using to encode / decode byte array
     * @return error code
     */
    private int processProviderConsumer(HuffmanAlgorithm algorithm) {
        int length = 0;
        int selfBufferSize = Integer.parseInt(configWorker.get(GrammarWorkerHuffman.BUFFER_SIZE));
        DataConverterHuffman converter = new DataConverterHuffman();
        dataStorageTemp = dataStorage;
        if (dataStorage == null) {
            return 0;
        }
        for (int i = 0; i < ((byte[]) dataStorageTemp).length; i += (selfBufferSize - length)) {//pos in result
            int size = ((byte[]) dataStorageTemp).length < selfBufferSize ? ((byte[]) dataStorageTemp).length : selfBufferSize;
            byte[] buffer = new byte[size];
            System.arraycopy((byte[]) dataStorageTemp, i, buffer, 0, size);
            HuffmanAlgorithmResult result = algorithm.startProcess(buffer, Integer.parseInt(configWorker.get(GrammarWorkerHuffman.REQUESTED_LENGTH)));
            length = result.getExtra().length;
            for (Executor sub : consumerMap.keySet()) {
                dataStorage = result.getResult();
                dataStorage = converter.wrapArray((byte[]) dataStorage);
                this.dataStoragePos = 0;
                if (sub.put(this) == 0) {
                    sub.run();
                }
                this.dataStoragePos = 0;
                dataStorage = dataStorageTemp;
            }
        }
        return 0;
    }

    /**
     * @param provider - reference to the provider
     * @return error code
     */
    public int put(Executor provider) {
        int counter;
        APPROPRIATE_TYPES currentType = providers.get(provider);
        int startPos = Integer.parseInt(this.configWorker.get(GrammarWorkerHuffman.START_POS));
        int requestedLength = Integer.parseInt(this.configWorker.get(GrammarWorkerHuffman.REQUESTED_LENGTH));
        Object adapter = this.adapters.get(provider);
        DataConverterHuffman converter = new DataConverterHuffman();
        Pair<Integer, Integer> blockMetrics = new Pair<>(startPos, requestedLength);
        ArrayList<Byte> byteArrayList = new ArrayList<>();
        switch (currentType) {
            case BYTE:
                for (counter = 0; counter < requestedLength; counter += 1) {
                    Byte b = ((InterfaceByteTransfer) adapter).getNextByte(blockMetrics);
                    if (b == null)
                        break;
                    else
                        byteArrayList.add(b);
                }
                dataStorage = converter.convertToPrimitive(byteArrayList);
                break;
            case DOUBLE:
                ArrayList<Double> doubleArrayList = new ArrayList<>();
                for (counter = 0; counter < requestedLength; counter += 1) {
                    Double d = ((InterfaceDoubleTransfer) adapter).getNextDouble(blockMetrics);
                    if (d == null)
                        break;
                    else
                        doubleArrayList.add(d);
                }
                Double[] tempDouble = converter.convertDoubleListToDoubleArr(doubleArrayList);
                dataStorage = converter.convertToPrimitiveArray(converter.convertDoubleToByte(tempDouble));
                break;
            case CHAR:
                ArrayList<Character> charArrayList = new ArrayList<>();
                for (counter = 0; counter < requestedLength; counter += 1) {
                    Character c = ((InterfaceCharTransfer) adapter).getNextChar(blockMetrics);
                    if (c == null)
                        break;
                    else
                        charArrayList.add(c);
                }
                Character[] tempChar = converter.convertCharListToCharArr(charArrayList);
                dataStorage = converter.convertToPrimitiveArray(converter.convertCharToByte(tempChar));
                break;
            default:
                return 0; //error
            //Log.report();
        }
        return 0;
    }

    class ByteTransfer implements InterfaceByteTransfer {
        Integer startPos;
        Integer blockLength;
        public APPROPRIATE_TYPES GetFormat(){
            return APPROPRIATE_TYPES.BYTE;
        }

        public void SetMetrics(Pair<Integer, Integer> blockMetrics){
            this.startPos = blockMetrics.getKey();
            this.blockLength = blockMetrics.getValue();
        }
        public Pair<Integer, Integer> GetMetrics() {
            return new Pair<>(startPos, blockLength);
        }

        public Byte getNextByte(Object blockMetrics) {
            if (dataStoragePos == 0)
                dataStoragePos = ((Pair<Integer, Integer>) blockMetrics).getKey();
            if (dataStoragePos >= ((Byte[]) dataStorage).length)
                return null;
            return ((Byte[]) dataStorage)[dataStoragePos++];
        }
    }

    class DoubleTransfer implements InterfaceDoubleTransfer {
        Integer startPos;
        Integer blockLength;
        public APPROPRIATE_TYPES GetFormat(){
            return APPROPRIATE_TYPES.DOUBLE;
        }
        public void SetMetrics(Pair<Integer, Integer> blockMetrics){
            this.startPos = blockMetrics.getKey();
            this.blockLength = blockMetrics.getValue();
        }
        public Pair<Integer, Integer> GetMetrics() {
            return new Pair<>(startPos, blockLength);
        }
        public Double getNextDouble(Object blockMetrics) {
            if (dataStoragePos == 0)
                dataStoragePos = ((Pair<Integer, Integer>) blockMetrics).getKey();
            if (dataStoragePos >= ((Byte[]) dataStorage).length)
                return null;
            return ((Double[]) dataStorage)[dataStoragePos++];
        }
    }

    class CharTransfer implements InterfaceCharTransfer {
        Integer startPos;
        Integer blockLength;
        public APPROPRIATE_TYPES GetFormat(){
            return APPROPRIATE_TYPES.CHAR;
        }
        public void SetMetrics(Pair<Integer, Integer> blockMetrics){
            this.startPos = blockMetrics.getKey();
            this.blockLength = blockMetrics.getValue();
        }
        public Pair<Integer, Integer> GetMetrics() {
            return new Pair<>(startPos, blockLength);
        }
        public Character getNextChar(Object blockMetrics) {
            if (dataStoragePos == 0)
                dataStoragePos = ((Pair<Integer, Integer>) blockMetrics).getKey();
            if (dataStoragePos >= ((Byte[]) dataStorage).length)
                return null;
            return ((Character[]) dataStorage)[dataStoragePos++];
        }
    }
}
