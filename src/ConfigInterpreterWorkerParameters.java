import java.util.Map;

public class ConfigInterpreterWorkerParameters extends ConfigInterpreterBase<GrammarWorkerHuffman, String> {
    public ConfigInterpreterWorkerParameters(String fileName) {
        super(fileName);
    }
    /**
     * @param configMap - container for putting in (key, value) pair
     * @param key       string containing GrammarWorkerHuffman key value
     * @param value     puts (key, value) pair in configMap
     *                  realisation for conveyor.Executor(worker) configuration file
     */
    @Override
    protected int addConfiguration(Map<GrammarWorkerHuffman, String> configMap, Object key, Object value) {
        try {
            GrammarWorkerHuffman g = GrammarWorkerHuffman.valueOf((String)key);
            configMap.put(g, (String) value);
        } catch (IllegalArgumentException e) {
            Log.report("Unexpected key in options config file: " + fileName);
            return -1;
        }
        return 0;
    }
}
