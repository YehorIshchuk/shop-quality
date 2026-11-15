package com.kira.shop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Допоміжні операції з грошовими сумами. */
public final class MoneyUtils {

    private MoneyUtils() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentOf(BigDecimal value, int percent) {
        return round(value.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }

    public static String format(BigDecimal value) {
        return round(value).toPlainString() + " UAH";
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
