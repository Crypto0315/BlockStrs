package entity;

public class PolynomialDivisionResult {
    private Long[] quotient;
    private Long[] remainder;

    public PolynomialDivisionResult(Long[] quotient, Long[] remainder) {
        this.quotient = quotient;
        this.remainder = remainder;
    }

    public Long[] getQuotient() {
        return quotient;
    }

    public Long[] getRemainder() {
        return remainder;
    }
}