package neureka.backend.main.operations.operator.impl;

public class CLBroadcastMultiplication extends CLBroadcast
{
    public CLBroadcastMultiplication(String id) {
        super(id, "value = src1 * src2;\n", "value += ( d == 0 ? drain : handle );\n");
    }
}
