package service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import model.NhanVien;
import model.PhongBan;
import model.TrinhDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryNhanSuRepository;

class NhanVienServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private InMemoryNhanSuRepository repository;
    private NhanVienService service;
    private PhongBan first;
    private PhongBan second;

    static Date date(String value) {
        return Date.from(LocalDate.parse(value).atStartOfDay(ZONE).toInstant());
    }

    static NhanVien employee(String id, String birth, String hire) {
        return new NhanVien(" Nguyễn An ", date(birth), "0900000001", true,
                id, "Đang làm", "Nhân viên", "Hà Nội", date(hire), null);
    }

    @BeforeEach void setup() {
        repository = new InMemoryNhanSuRepository();
        service = new NhanVienService(repository, ZONE);
        first = new PhongBan("Kỹ thuật", "", new ArrayList<>());
        second = new PhongBan("Kinh doanh", "", new ArrayList<>());
        repository.thayThePhongBan(List.of(first, second));
    }

    @Test void addsSameModelInstanceAndInitializesWorkdays() {
        NhanVien nv = employee(" NV001 ", "2000-01-01", "2024-01-01");
        assertSame(nv, service.themNhanVien(first, nv));
        assertSame(nv, first.getNhanVien().get(0));
        assertSame(nv, repository.timTheoMa("nv001").orElseThrow());
        assertEquals("NV001", nv.getId());
        assertEquals("Nguyễn An", nv.getFullName());
        assertEquals(0, nv.getNgayCong().getSoNgayDaLam());
        assertEquals(26, nv.getNgayCong().getSoNgayChuan());
        assertSame(nv, nv.getNgayCong().getNhanVien());
    }

    @Test void rejectsDuplicateWithinAndAcrossDepartmentsIncludingCaseAndWhitespace() {
        service.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        for (PhongBan pb : List.of(first, second)) {
            NhanVien rejected = employee(" nv001 ", "2000-01-01", "2024-01-01");
            assertThrows(IllegalArgumentException.class, () -> service.themNhanVien(pb, rejected));
            assertNull(rejected.getNgayCong());
        }
        assertEquals(1, repository.tatCaNhanVien().size());
    }

    @Test void acceptsExactlyEighteenAtHireEvenWhenNotEighteenToday() {
        service.themNhanVien(first, employee("NV001", "2008-11-07", "2026-11-07"));
        service.themNhanVien(first, employee("NV002", "2020-11-07", "2038-11-07"));
        assertEquals(2, first.getNhanVien().size());
    }

    @Test void rejectsOneDayBeforeEighteenthBirthdayAtHire() {
        assertThrows(IllegalArgumentException.class,
                () -> service.themNhanVien(first, employee("NV001", "2008-11-08", "2026-11-07")));
        assertTrue(repository.tatCaNhanVien().isEmpty());
    }

    @Test void leapDayBirthUsesCalendarAnniversary() {
        assertThrows(IllegalArgumentException.class,
                () -> service.themNhanVien(first, employee("NV001", "2008-02-29", "2026-02-27")));
        service.themNhanVien(first, employee("NV001", "2008-02-29", "2026-02-28"));
    }

    @Test void rejectsMissingFieldsAndInvalidDateOrderWithoutWriting() {
        List<NhanVien> invalid = new ArrayList<>();
        invalid.add(null);
        invalid.add(employee(" ", "2000-01-01", "2024-01-01"));
        NhanVien noName = employee("NV001", "2000-01-01", "2024-01-01"); noName.setFullName(" "); invalid.add(noName);
        NhanVien noBirth = employee("NV001", "2000-01-01", "2024-01-01"); noBirth.setBirthDate(null); invalid.add(noBirth);
        NhanVien noHire = employee("NV001", "2000-01-01", "2024-01-01"); noHire.setNgayVaoLam(null); invalid.add(noHire);
        invalid.add(employee("NV001", "2000-01-01", "1999-12-31"));
        for (NhanVien nv : invalid) assertThrows(IllegalArgumentException.class, () -> service.themNhanVien(first, nv));
        assertTrue(repository.tatCaNhanVien().isEmpty());
    }

    @Test void rejectsDepartmentOutsideRepository() {
        assertThrows(IllegalArgumentException.class, () -> service.themNhanVien(new PhongBan(),
                employee("NV001", "2000-01-01", "2024-01-01")));
        assertTrue(repository.tatCaNhanVien().isEmpty());
    }

    @Test void renameChecksGlobalUniquenessAndKeepsManagerReference() {
        NhanVien one = service.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        service.themNhanVien(second, employee("NV002", "2000-01-01", "2024-01-01"));
        first.setIdTruongPhong("NV001");
        assertThrows(IllegalArgumentException.class, () -> service.doiMaNhanVien(one, "nv002"));
        assertEquals("NV001", one.getId());
        service.doiMaNhanVien(one, " NV003 ");
        assertTrue(repository.timTheoMa("NV001").isEmpty());
        assertSame(one, repository.timTheoMa("NV003").orElseThrow());
        assertEquals("NV003", first.getIdTruongPhong());
    }

    @Test void failedProfileUpdateDoesNotPartiallyRenameEmployee() {
        NhanVien one = service.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        NhanVien invalid = employee("NV003", "2008-11-08", "2026-11-07");
        assertThrows(IllegalArgumentException.class, () -> service.capNhatThongTin(one, invalid));
        assertEquals("NV001", one.getId());
        assertEquals(date("2000-01-01"), one.getBirthDate());
    }

    @Test void validProfileUpdatePreservesWorkdaysAndEducation() {
        NhanVien one = service.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        var work = one.getNgayCong();
        TrinhDo level = new TrinhDo("Cử nhân", "01/01/2020"); one.getTrinhDos().add(level);
        service.capNhatThongTin(one, employee("NV003", "1999-01-01", "2025-01-01"));
        assertSame(one, repository.timTheoMa("NV003").orElseThrow());
        assertSame(work, one.getNgayCong());
        assertSame(level, one.getTrinhDos().get(0));
    }

    @Test void inlineDateChangesCannotBypassAgeRule() {
        NhanVien one = service.themNhanVien(first, employee("NV001", "2000-01-01", "2024-01-01"));
        assertThrows(IllegalArgumentException.class, () -> service.capNhatNgaySinh(one, date("2010-01-01")));
        assertThrows(IllegalArgumentException.class, () -> service.capNhatNgayVaoLam(one, date("2017-12-31")));
        assertEquals(date("2000-01-01"), one.getBirthDate());
        assertEquals(date("2024-01-01"), one.getNgayVaoLam());
    }

    @Test void concurrentAddsThroughDifferentServicesAcceptOnlyOneDuplicate() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var tasks = List.of(first, second).stream().map(pb -> executor.submit(() -> {
                start.await();
                try {
                    new NhanVienService(repository, ZONE).themNhanVien(pb,
                            employee("NV001", "2000-01-01", "2024-01-01"));
                    return true;
                } catch (IllegalArgumentException exception) { return false; }
            })).toList();
            start.countDown();
            int accepted = 0;
            for (var task : tasks) if (task.get(5, TimeUnit.SECONDS)) accepted++;
            assertEquals(1, accepted);
            assertEquals(1, repository.tatCaNhanVien().size());
        } finally { executor.shutdownNow(); }
    }
}
