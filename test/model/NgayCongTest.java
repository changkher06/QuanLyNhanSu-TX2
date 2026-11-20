package model;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class NgayCongTest {
    @Test void doubleConstructorKeepsExistingGuiApiAndFixesNinetyPercent() {
        NgayCong result = new NgayCong(null, 23.4, 26);
        assertEquals(90, result.tinhPhanTramHoanThanh());
        assertEquals("Giỏi", result.getPhanLoai());
    }

    @Test void displayRoundingDoesNotChangeClassification() {
        NgayCong result = new NgayCong(null, 19.49, 26);
        assertEquals("75.0", String.format(Locale.US, "%.1f", result.tinhPhanTramHoanThanh()));
        assertEquals("Trung bình", result.getPhanLoai());
    }

    @Test void arbitrarilyCloseBoundaryUsesCrossMultiplicationInsteadOfRoundedDivision() {
        NgayCong result = new NgayCong(null,
                new BigDecimal("89.999999999999999999999999999999999999999"), new BigDecimal("100"));
        assertEquals("Khá", result.getPhanLoai());
    }

    @Test void defaultObjectAndLegacySettersStillWork() {
        NgayCong result = new NgayCong();
        assertEquals(0, result.tinhPhanTramHoanThanh());
        result.setSoNgayChuan(26);
        result.setSoNgayDaLam(23.4);
        assertEquals("Giỏi", result.getPhanLoai());
        assertThrows(IllegalArgumentException.class, () -> result.setSoNgayDaLam(Double.NaN));
        assertEquals(23.4, result.getSoNgayDaLam());
    }
}
