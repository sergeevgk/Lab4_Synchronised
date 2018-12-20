import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Manager {
    private Map<GrammarManager, String> configManager;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private ArrayList<Executor> workers;
    private ExecutorService pool;

    public Manager(String fileName) {
        ConfigInterpreter config = new ConfigInterpreterManager(fileName);
        configManager = new EnumMap<>(GrammarManager.class);
        this.workers = new ArrayList<>();
        if (config.readConfiguration(configManager) == -1) {
            Log.report("Can't create manager");
        }
        HuffmanTableWriter.getInstance().init(configManager.get(GrammarManager.HUFFMAN_TABLE));
        HuffmanTableReader.getInstance().init(configManager.get(GrammarManager.HUFFMAN_TABLE));
    }

    /**
     * Method open streams for input and output data for workers
     * @return 0 if streams opened successfully, -1 otherwise
     */
    public int openStreams() {
        try {
            FileInputStream inStream = new FileInputStream(configManager.get(GrammarManager.IN));
            this.inputStream = new DataInputStream(inStream);
            FileOutputStream outStream = new FileOutputStream(configManager.get(GrammarManager.OUT));
            this.outputStream = new DataOutputStream(outStream);
        } catch (NullPointerException e) {
            Log.report("Exception thrown during stream open process.\n");
            return -1;
        } catch (IOException e) {
            Log.report("Exception thrown during stream open process.\n");
            return -1;
        }
        return 0;
    }

    /**
     * Method creates workers and puts them to ArrayList()
     * @return 0 if workers created successfully, -1 otherwise
     */
    public int createWorkers() {
        Executor newWorker;
        String fileName, className;
        Map<String, Pair<String, String>> configWorkerListFileNames = new TreeMap<>();
        ConfigInterpreterWorkers configWorkerListReader;
        configWorkerListReader = new ConfigInterpreterWorkers(configManager.get(GrammarManager.WORKER_LIST));
        configWorkerListReader.readConfiguration(configWorkerListFileNames);
        for (String workerFileName : configWorkerListFileNames.keySet()){
            fileName = configWorkerListFileNames.get(workerFileName).getKey();
            className = configWorkerListFileNames.get(workerFileName).getValue();
            try {
                newWorker = (Executor) Class.forName(className).newInstance();
                newWorker.setConfig(fileName);
                this.workers.add(newWorker);
            } catch( ReflectiveOperationException e){
                return -1;
            }
        }
        pool = Executors.newFixedThreadPool(workers.size());
        return 0;
    }

    /**
     * manager introduces workers with each other according to their link list
     *
     * @return int errorValue - (if != 0) error occurred
     */
    public int introduceWorkers() {
        int i;
        ArrayList<Integer> consumersList;
        Map<Integer, ArrayList<Integer>> schedule = new HashMap<>();
        new ConfigInterpreterWorkerSchedule(configManager.get(GrammarManager.WORKERS_SCHEDULE)).readConfiguration(schedule);
        for (i = 0; i < workers.size(); i += 1) {
            if ((consumersList = schedule.get(i)) == null) {
                Log.report("Invalid worker introduce.\n");
                return -1;
            }
            if (consumersList.size() > 0) {
                for (int j : consumersList) {
                    if (workers.get(i) == null || i == j) {
                        Log.report("Invalid worker in schedule.\n");
                        return -1;
                    }
                    workers.get(i).setConsumer(workers.get(j));
                }
            }
        }
        return 0;
    }

    /**
     * Starts encode-decode conveyor with at least two workers
     * @return 0 if conveyor worked without errors, -1 otherwise
     */
    public int StartConveyor() {

        Executor first = workers.get(Integer.parseInt(configManager.get(GrammarManager.START)));
        HuffmanTableBuilder.getInstance().init(configManager.get(GrammarManager.IN));
        first.setInput(inputStream);
        Executor last = workers.get(Integer.parseInt(configManager.get(GrammarManager.END)));
        last.setOutput(outputStream);

        for (Executor worker: workers) {
            try {
                pool.submit(
                        ()->{
                            worker.run();
                        }
                );
            } catch (Exception e) {
                if (poolStop() != 0) {
                    Log.report("Exception during pool execution.\n");
                    return -1;
                }
            }
            if (poolStop() != 0) {
                return -1;
            }
        }
        return 0;
    }

    private int poolStop(){
        try {
            pool.shutdownNow();
            inputStream.close();
            outputStream.close();
        } catch(IOException e){
            Log.report("Can not close streams.\n");
            return -1;
        }
        return 0;
    }
    /**
     * Method closes streams in manager
     * @return 0 if streams closed successfully, -1 otherwise
     */
    public int closeStreams() {
        try {
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            Log.report("Exception thrown during stream close process.\n");
            return -1;
        }
        return 0;
    }
}

