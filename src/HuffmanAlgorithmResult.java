public class HuffmanAlgorithmResult {
    private byte[] result;
    private byte[] extraSymbols;
    private int uncodedLength;

    public HuffmanAlgorithmResult(byte[] result, byte[] extraSymbols, int length) {
        this.result = result;
        this.extraSymbols = extraSymbols;
        this.uncodedLength = length;
    }

    public byte[] getResult() {
        return result;
    }

    public byte[] getExtra() {
        return extraSymbols;
    }

}
