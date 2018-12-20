import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class MyFileWriter {
    private DataOutputStream writerResult;
    //private BufferedOutputStream writerTable;
    // private boolean tableWritten;
    private String codeMode;

    public MyFileWriter(Map<GrammarWorkerHuffman, String> configWorker, DataOutputStream outputStream) {
        this.writerResult = outputStream;
        this.codeMode = configWorker.get(GrammarWorkerHuffman.CODE_MODE);
    }

    /**
     * @param result HuffmanAlgorithmResult instance contains byte[] res - result of huffman algorithm work
     *               <p>
     *               smethod writes byte[] res to output buffered stream
     */
    public final void writeOutputFile(byte[] result) { //writeToFile
        try {
            writerResult.write(result);//////////////
            writerResult.flush();
           /* if (codeMode.equals("0"))
                writeHuffmanTable(result.getHuffmanTable());*/
        } catch (IOException e) {
            Log.report("Writing output file error.");
            return;
        }
    }

    /**
     * closes streams
     */
    public final void close() {
        try {
            writerResult.close();
           /* if (codeMode.equals("0"))
                writerTable.close();*/
        } catch (IOException e) {

        }
    }
}
