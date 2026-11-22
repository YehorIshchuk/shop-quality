package fx;

public record Money(long cents, String currency) {
    public static Money zero() { return new Money(0, "UAH"); }
}
