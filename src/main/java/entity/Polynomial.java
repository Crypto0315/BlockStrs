package entity;

public class Polynomial {
    private Long[] coefficients;

    public Polynomial(Long[] coefficients) {
        this.coefficients = coefficients;
    }

    public Long[] getCoefficients() {
        return coefficients;
    }
}