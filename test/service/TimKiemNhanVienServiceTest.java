package service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import model.NhanVien;
import model.PhongBan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import repository.InMemoryNhanSuRepository;

class TimKiemNhanVienServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private InMemoryNhanSuRepository repository;
    private TimKiemNhanVienService service;
    private PhongBan pb;
    private NhanVien one;
    private NhanVien two;

    private Date date(String text) { return Date.from(LocalDate.parse(text).atStartOfDay(ZONE).toInstant()); }

    private NhanVien employee(String id, String name, String birth, String hire, boolean male) {
        return new NhanVien(name, date(birth), "", male, id, "Đang làm", "Trưởng nhóm", "", date(hire), null);
    }

    @BeforeEach void setup() {
        repository = new InMemoryNhanSuRepository();
        Clock clock = Clock.fixed(LocalDate.of(2026, 11, 19).atStartOfDay(ZONE).toInstant(), ZONE);
        service = new TimKiemNhanVienService(repository, clock);
        one = employee("NV001", "Nguyễn An", "2000-11-19", "2020-11-19", true);
        two = employee("NV002", "Lê Lan", "2000-11-20", "2020-11-20", false);
        pb = new PhongBan("Một", "", new ArrayList<>(List.of(one, two)));
        repository.thayThePhongBan(List.of(pb, new PhongBan("Hai", "", new ArrayList<>(List.of(
                employee("NV100", "Nguyễn Mai", "1995-01-01", "2020-01-01", false))))));
    }

    @Test void textSearchIsCaseInsensitiveAndScopedToDepartment() {
        assertEquals(List.of(one), service.timKiem(pb, "Họ tên", " nGUYỄN "));
        assertEquals(List.of(one, two), service.timKiem(pb, "Trạng thái", "đANG"));
        assertEquals(List.of(one, two), service.timKiem(pb, "Vị trí", "trƯỞNG"));
        assertEquals(List.of(two), service.timKiem(pb, "Giới tính", "Nữ"));
        assertTrue(service.timKiem(pb, "Họ tên", "không tồn tại").isEmpty());
    }

    @Test void supportsIdNumberRangeAndTextWithoutLosingLeadingZeroMatching() {
        assertEquals(List.of(two), service.timKiem(pb, "Mã nhân viên", "2"));
        assertEquals(List.of(one, two), service.timKiem(pb, "Mã nhân viên", "1-2"));
        assertEquals(List.of(one, two), service.timKiem(pb, "Mã nhân viên", "NV00"));
    }

    @Test void ageAndSeniorityUseFullYearsAtInjectedClock() {
        assertEquals(List.of(one), service.timKiem(pb, "Tuổi", "26"));
        assertEquals(List.of(two), service.timKiem(pb, "Tuổi", "25"));
        assertEquals(List.of(one), service.timKiem(pb, "Thâm niên", "6"));
        assertEquals(List.of(one, two), service.timKiem(pb, "Tuổi", "25-26"));
    }

    @ParameterizedTest
    @CsvSource({"Mã nhân viên,3-1", "Tuổi,-1", "Tuổi,30-18", "Thâm niên,abc", "Tuổi,1-2-3", "Tuổi,1.5"})
    void rejectsInvalidNumericFiltersWithoutChangingData(String criterion, String value) {
        assertThrows(IllegalArgumentException.class, () -> service.timKiem(pb, criterion, value));
        assertEquals(List.of(one, two), repository.danhSachNhanVien(pb));
    }

    @Test void filterAndSearchShareTheSameRuleAfterEditing() {
        var filter = service.taoBoLoc("Họ tên", "An");
        assertTrue(filter.test(one));
        one.setFullName("Bình");
        assertFalse(filter.test(one));
        assertEquals(List.of(two), service.timKiem(pb, "Họ tên", "An"));
    }

    @Test void rejectsBlankUnknownCriterionAndInvalidGender() {
        assertThrows(IllegalArgumentException.class, () -> service.timKiem(pb, "Họ tên", " "));
        assertThrows(IllegalArgumentException.class, () -> service.timKiem(pb, "khác", "An"));
        assertThrows(IllegalArgumentException.class, () -> service.timKiem(pb, "Giới tính", "khác"));
        assertThrows(IllegalArgumentException.class, () -> service.timKiem(new PhongBan(), "Họ tên", "An"));
    }

    @Test void largeIdNumberDoesNotOverflowAndNullDatesDoNotMatchAge() {
        one.setID("NV9999999999999999999999999999");
        assertEquals(List.of(one), service.timKiem(pb, "Mã nhân viên", "9999999999999999999999999999"));
        two.setBirthDate(null);
        assertTrue(service.timKiem(pb, "Tuổi", "0").isEmpty());
    }

    @Test void futureDatesDoNotMatchZeroYears() {
        one.setBirthDate(date("2026-11-20"));
        one.setNgayVaoLam(date("2026-11-20"));
        assertTrue(service.timKiem(pb, "Tuổi", "0").isEmpty());
        assertTrue(service.timKiem(pb, "Thâm niên", "0").isEmpty());
    }
}
