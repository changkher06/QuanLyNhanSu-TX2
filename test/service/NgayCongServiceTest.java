package service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import model.NgayCong;
import model.NhanVien;
import model.PhongBan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import repository.InMemoryNhanSuRepository;

class NgayCongServiceTest {
    private InMemoryNhanSuRepository repository;
    private NgayCongService service;
    private NhanVien nv;
    private PhongBan first;
    private NhanVien other;

    @BeforeEach void setup() {
        repository = new InMemoryNhanSuRepository();
        service = new NgayCongService(repository);
        nv = new NhanVien("An", null, "", true, "NV001", "Đang làm", "", "");
        other = new NhanVien("Lan", null, "", false, "NV002", "Đang làm", "", "");
        first = new PhongBan("Một", "", new ArrayList<>(List.of(nv)));
        repository.thayThePhongBan(List.of(first,
                new PhongBan("Hai", "", new ArrayList<>(List.of(other)))));
        nv.setNgayCong(new NgayCong(nv, 10, 26));
        other.setNgayCong(new NgayCong(other, 20, 26));
    }

    @ParameterizedTest
    @CsvSource({"0,26,Kém", "12.99,26,Kém", "13,26,Trung bình", "19.49,26,Trung bình",
            "19.50,26,Khá", "23.39,26,Khá", "23.40,26,Giỏi", "25.99,26,Giỏi", "26,26,Xuất sắc",
            "0.09,0.10,Giỏi", "13.05,26.10,Trung bình"})
    void classifiesAtExactDecimalBoundaries(String done, String standard, String grade) {
        NgayCong result = service.capNhatNgayCong(nv, done, standard);
        assertSame(result, nv.getNgayCong());
        assertSame(nv, result.getNhanVien());
        assertEquals(grade, result.getPhanLoai());
        assertEquals(new BigDecimal(done), result.getSoNgayDaLamChinhXac());
        assertEquals(new BigDecimal(standard), result.getSoNgayChuanChinhXac());
    }

    @ParameterizedTest
    @CsvSource({"NaN,26", "Infinity,26", "-Infinity,26", "-0.01,26", "26.01,26", "5,0",
            "5,-1", "abc,26", "13,NaN", "13,Infinity", "12.345,26", "13,25.123", "1e1000,1e1001"})
    void rejectsInvalidInputWithoutReplacingPreviousValue(String done, String standard) {
        NgayCong previous = nv.getNgayCong();
        assertThrows(IllegalArgumentException.class, () -> service.capNhatNgayCong(nv, done, standard));
        assertSame(previous, nv.getNgayCong());
    }

    @Test void rejectsNonFiniteDoublesOnBothInputs() {
        NgayCong previous = nv.getNgayCong();
        for (double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> service.capNhatNgayCong(nv, value, 26));
            assertThrows(IllegalArgumentException.class, () -> service.capNhatNgayCong(nv, 13, value));
        }
        assertSame(previous, nv.getNgayCong());
    }

    @Test void rejectsNullAndBlankValues() {
        NgayCong previous = nv.getNgayCong();
        assertThrows(IllegalArgumentException.class, () -> service.capNhatNgayCong(nv, (String) null, "26"));
        assertThrows(IllegalArgumentException.class, () -> service.capNhatNgayCong(nv, " ", "26"));
        assertThrows(IllegalArgumentException.class, () -> service.capNhatNgayCong(nv, (BigDecimal) null, BigDecimal.TEN));
        assertSame(previous, nv.getNgayCong());
    }

    @Test void rejectsUnregisteredEmployeeAndResetsOnlySelectedDepartment() {
        assertThrows(IllegalArgumentException.class, () -> service.capNhatNgayCong(new NhanVien(), "13", "26"));
        service.resetPhongBan(first);
        assertEquals(0, nv.getNgayCong().getSoNgayDaLam());
        assertEquals(26, nv.getNgayCong().getSoNgayChuan());
        assertEquals(20, other.getNgayCong().getSoNgayDaLam());
    }

    @Test void acceptsTrailingZerosWithoutRoundingAndDoubleCompatibility() {
        assertEquals("Khá", service.capNhatNgayCong(nv, " 19.5000 ", "26.000").getPhanLoai());
        assertEquals("Giỏi", service.capNhatNgayCong(nv, 23.4, 26).getPhanLoai());
    }
}
