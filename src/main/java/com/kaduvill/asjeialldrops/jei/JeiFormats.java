package com.kaduvill.asjeialldrops.jei;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

final class JeiFormats {
    private JeiFormats() {}
    static String percentage(double probability) {
        double percentage = probability * 100;
        if (percentage > 0 && percentage < 0.1) {
            return BigDecimal.valueOf(probability).movePointRight(2)
                    .round(new MathContext(2, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString();
        }
        return String.format(Locale.ROOT, "%.1f", percentage);
    }
}
