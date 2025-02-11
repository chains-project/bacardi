package ml.peya.plugins.Learn;

import java.util.ArrayList;
import java.lang.Math; // Added import for Math class

/**
 * 重みを突っ込む器。
 */
public class Neuron
{
    /**
     * 加算したWeight。
     */
    private double sum;

    /**
     * 入力用値。
     */
    private double value = 0.0;

    /**
     * ゲッター。
     *
     * @return value参照。
     */
    public double getValue()
    {
        return value;
    }

    /**
     * セッター。
     *
     * @param value value参照。
     */
    public void setValue(double value)
    {
        this.value = value;
    }

    /**
     * ReLU関数で値を強めて出力系に入れる。
     *
     * @param inputData InputクラスのArrayListデータ。
     */
    public void input(ArrayList<Input> inputData)
    {
        inputData.parallelStream().forEachOrdered(input -> input(input.getWeightingValue()));
        setValue(sigmoid(sum));
    }

    /**
     * Sigmoid関数の実装。
     *
     * @param x 入力値。
     * @return Sigmoid関数の出力。
     */
    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    /**
     * input(ArrayList)のオーバーロード。
     *
     * @param value sumに加算する値。
     */
    public void input(double value)
    {
        sum += value;
    }
}