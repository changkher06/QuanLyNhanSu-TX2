package compatibility;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import model.NhanVien;
import model.PhongBan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import repository.InMemoryNhanSuRepository;
import service.NgayCongService;
import service.NhanVienService;
import service.TimKiemNhanVienService;

/** Characterization của các nhánh tại 3312331, khác test đặc tả QT ở giai đoạn 1. */
class Tx1ServiceBehaviorTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private InMemoryNhanSuRepository repository;
    private NhanVienService employees;
    private NgayCongService work;
    private TimKiemNhanVienService search;
    private PhongBan first;
    private PhongBan second;

    private Date date(String text) { return Date.from(LocalDate.parse(text).atStartOfDay(ZONE).toInstant()); }

    private NhanVien employee(String id, String birth, String hire) {
        return new NhanVien("An", date(birth), "", true, id, "Đang làm", "Nhóm-1", "", date(hire), null);
    }

    @BeforeEach void setup() {
        repository = new InMemoryNhanSuRepository();
        employees = new NhanVienService(repository, ZONE);
        work = new NgayCongService(repository);
        search = new TimKiemNhanVienService(repository,
                Clock.fixed(date("2026-11-19").toInstant(), ZONE));
        first = new PhongBan("Một", "", new ArrayList<>());
        second = new PhongBan("Hai", "", new ArrayList<>());
        repository.thayThePhongBan(List.of(first, second));
    }

    @Test void duplicateCodesWithinAndAcrossDepartmentsRemainSeparateModels() {
        NhanVien one = employees.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        NhanVien two = employees.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        NhanVien three = employees.themNhanVien(second, employee("NV001", "2000-01-01", "2024-01-01"));
        assertEquals(List.of(one, two, three), repository.timTatCaTheoMa("NV001"));
        assertEquals(2, first.getNhanVien().size());
        assertNotSame(one, two);
        assertNotEquals(repository.idNhanVien(one), repository.idNhanVien(two));
    }

    @ParameterizedTest
    @CsvSource({"2008-11-08,2026-11-07", "2026-11-07,2026-11-07", "2008-02-29,2026-02-27"})
    void addChecksDateOrderButDoesNotCheckMinimumAge(String birth, String hire) {
        NhanVien nv = employees.themNhanVien(first, employee("NV001", birth, hire));
        assertSame(nv, first.getNhanVien().get(0));
        assertEquals(0, nv.getNgayCong().getSoNgayDaLam());
        assertEquals(26, nv.getNgayCong().getSoNgayChuan());
    }

    @Test void addStillRejectsHireBeforeBirthIncludingTimeOfDay() {
        NhanVien nv = employee("NV001", "2000-01-01", "2000-01-01");
        nv.setBirthDate(new Date(nv.getBirthDate().getTime() + 1));
        assertThrows(IllegalArgumentException.class, () -> employees.themNhanVien(first, nv));
        assertTrue(first.getNhanVien().isEmpty());
    }

    @Test void inlineDateAndCodeUpdatesKeepSetterBehaviorAndManagerCode() {
        NhanVien nv = employees.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        first.setIdTruongPhong("NV001");
        Date newHire = date("1990-01-01");
        employees.capNhatNgayVaoLam(nv, newHire);
        assertSame(newHire, nv.getNgayVaoLam());
        employees.doiMaNhanVien(nv, "");
        assertEquals("", nv.getId());
        assertEquals("NV001", first.getIdTruongPhong());
    }

    @Test void profileChangesCodeBeforeDateValidationAsInTx1() {
        NhanVien nv = employees.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        var id = repository.idNhanVien(nv);
        NhanVien input = employee("NV003", "2000-01-01", "1999-01-01");
        input.setFullName("Tên khác");
        assertThrows(IllegalArgumentException.class, () -> employees.capNhatThongTin(nv, input));
        assertEquals("NV003", nv.getId());
        assertEquals("An", nv.getFullName());
        assertEquals(date("2024-01-01"), nv.getNgayVaoLam());
        assertEquals(id, repository.idNhanVien(nv));
    }

    @ParameterizedTest
    @CsvSource({"NaN,26", "13,NaN", "Infinity,Infinity", "13,Infinity", "12.345,26", "13,25.123"})
    void acceptsValuesThatPassTheThreeOriginalComparisons(String done, String standard) {
        NhanVien nv = employees.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        var result = work.capNhatNgayCong(nv, done, standard);
        assertEquals(Double.doubleToLongBits(Double.parseDouble(done)), Double.doubleToLongBits(result.getSoNgayDaLam()));
        assertEquals(Double.doubleToLongBits(Double.parseDouble(standard)), Double.doubleToLongBits(result.getSoNgayChuan()));
        assertSame(result, nv.getNgayCong());
    }

    @ParameterizedTest
    @CsvSource({"Infinity,26", "-Infinity,26", "-1,26", "27,26", "13,0", "abc,26"})
    void rejectedInputLeavesPreviousWorkdaysUntouched(String done, String standard) {
        NhanVien nv = employees.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        var previous = nv.getNgayCong();
        assertThrows(IllegalArgumentException.class, () -> work.capNhatNgayCong(nv, done, standard));
        assertSame(previous, nv.getNgayCong());
    }

    @Test void floatingPointClassificationAndDisplayHaveOriginalDistinctResults() {
        NhanVien nv = employees.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        var result = work.capNhatNgayCong(nv, "23.4", "26");
        assertEquals(Double.doubleToLongBits(23.4 / 26 * 100), Double.doubleToLongBits(result.tinhPhanTramHoanThanh()));
        assertEquals("Khá", result.getPhanLoai());
        assertEquals("90.0", String.format(Locale.US, "%.1f", result.tinhPhanTramHoanThanh()));
    }

    @Test void inverseIdRangeFallsBackToTextWhileNegativeAgeFallsBackToPosition() {
        NhanVien nv = employees.themNhanVien(first, employee("NV3-1", "2000-01-01", "2024-01-01"));
        assertEquals(List.of(nv), search.timKiem(first, "Mã nhân viên", "3-1"));
        assertEquals(List.of(nv), search.timKiem(first, "Tuổi", "-1"));
        assertThrows(IllegalArgumentException.class, () -> search.timKiem(first, "Tuổi", "30-18"));
    }

    @Test void applyAndReloadKeepTheirDifferentEmptyInputSemantics() {
        NhanVien nv = employees.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        assertThrows(IllegalArgumentException.class, () -> search.timKiem(first, "Họ tên", " "));
        assertTrue(search.khopBoLocHienTai(nv, "Họ tên", " "));
        assertThrows(IllegalArgumentException.class, () -> search.timKiem(first, "Tuổi", "abc"));
        assertFalse(search.khopBoLocHienTai(nv, "Tuổi", "abc"));
    }

    @Test void yearsUseOriginalDayOfYearComparisonAcrossLeapYears() {
        NhanVien nv = employees.themNhanVien(first, employee("NV001", "2000-03-01", "2024-01-01"));
        var leapSearch = new TimKiemNhanVienService(repository,
                Clock.fixed(date("2026-03-01").toInstant(), ZONE));
        assertEquals(List.of(nv), leapSearch.timKiem(first, "Tuổi", "25"));
        assertTrue(leapSearch.timKiem(first, "Tuổi", "26").isEmpty());
    }
}
