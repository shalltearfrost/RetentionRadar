package auk.sdt202.team4.optimization;

public interface ObjectiveFunction {
    int dimension();

    double value(double[] point);

    double[] gradient(double[] point);
}
