import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class ConfigInterpreterWorkers extends ConfigInterpreterBase<String, Pair<String, String>> {
    public ConfigInterpreterWorkers(String fileName) {
        super(fileName);
    }
    final static  String DELIMITER1 = ",";

    @Override
    public int readConfiguration(Map<String, Pair<String, String>> configMap) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ((line = line.replaceAll("\\s", "")).isEmpty())
                    continue;
                String[] set = line.split(DELIMITER);
                if (set.length != 2) {
                    Log.report("Invalid configuration syntax in file: " + fileName);
                    return -1;
                }
                String[] temp = set[1].split(DELIMITER1);
                Pair<String, String> pair = new Pair<>(temp[0], temp[1]);
                addConfiguration(configMap,  set[0], pair);
            }
        } catch (IOException e) {
            Log.report("Error while reading config file: " + fileName);
            return -1;
        }
        return 0;
    }
    /**
     * @param configMap - container for putting in (key, value) pair
     * @param key       string containing key value
     * @param value     puts (key, value) pair in configMap
     *                  realisation for conveyor.Executor(worker) configuration file
     */
    @Override
    protected int addConfiguration(Map <String, Pair<String, String>> configMap, Object key, Object value) {
        try {
            configMap.put((String) key, (Pair<String, String>) value);
        } catch (IllegalArgumentException e) {
            Log.report("Unexpected key in options config file: " + fileName);
            return -1;
        }
        return 0;
    }
}

