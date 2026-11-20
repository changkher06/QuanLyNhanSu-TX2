package compatibility;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import model.NhanVien;
import model.PhongBan;
import org.junit.jupiter.api.Test;
import repository.InMemoryNhanSuRepository;
import service.NgayCongService;

class RepositoryRecordIdentityTest {
    private NhanVien employee() { return new NhanVien("An", null, "", true, "NV001", "Đang làm", "", ""); }
    private PhongBan department(NhanVien... employees) {
        return new PhongBan("Cùng tên", "", new ArrayList<>(List.of(employees)));
    }

    @Test void allRecordsHaveDifferentIdsEvenWithDuplicateEmployeeAndDepartmentNames() {
        var repository = new InMemoryNhanSuRepository();
        var one = employee(); var two = employee(); var three = employee();
        var first = department(one, two); var second = department(three);
        repository.thayThePhongBan(List.of(first, second));
        List<UUID> ids = List.of(repository.idNhanVien(one), repository.idNhanVien(two),
                repository.idNhanVien(three), repository.idPhongBan(first), repository.idPhongBan(second));
        assertEquals(5, ids.stream().distinct().count());
        assertEquals(3, repository.timTatCaTheoMa("NV001").size());
        assertSame(two, repository.timNhanVienTheoId(ids.get(1)).orElseThrow());
        assertSame(second, repository.timPhongBanTheoId(ids.get(4)).orElseThrow());
    }

    @Test void renameDoesNotChangeTechnicalIdentityOrOtherDuplicateRecord() {
        var repository = new InMemoryNhanSuRepository();
        var one = employee(); var two = employee(); var pb = department(one, two);
        repository.themPhongBan(pb);
        UUID id = repository.idNhanVien(two);
        repository.doiMaNhanVien(two, "NV002");
        new NgayCongService(repository).capNhatNgayCong(repository.timNhanVienTheoId(id).orElseThrow(), "12.345", "26");
        assertEquals(id, repository.idNhanVien(two));
        assertEquals("NV001", one.getId());
        assertNull(one.getNgayCong());
        assertEquals(12.345, two.getNgayCong().getSoNgayDaLam());
    }

    @Test void deletedIdDoesNotResolveToOtherRecordWithSameCode() {
        var repository = new InMemoryNhanSuRepository();
        var one = employee(); var two = employee(); var pb = department(one, two);
        repository.themPhongBan(pb);
        UUID firstId = repository.idNhanVien(one); UUID secondId = repository.idNhanVien(two);
        repository.xoaNhanVien(pb, one);
        assertTrue(repository.timNhanVienTheoId(firstId).isEmpty());
        assertSame(two, repository.timNhanVienTheoId(secondId).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> repository.idNhanVien(one));
    }

    @Test void replacementRetainsIdsForSameObjectsAndAssignsNewIdsForNewSampleObjects() {
        var repository = new InMemoryNhanSuRepository();
        var nv = employee(); var pb = department(nv);
        repository.themPhongBan(pb);
        UUID old = repository.idNhanVien(nv);
        repository.thayThePhongBan(List.of(pb));
        assertEquals(old, repository.idNhanVien(nv));
        var next = employee(); repository.thayThePhongBan(List.of(department(next)));
        assertTrue(repository.timNhanVienTheoId(old).isEmpty());
        assertNotEquals(old, repository.idNhanVien(next));
    }

    @Test void removingDepartmentInvalidatesItsAndItsEmployeesIds() {
        var repository = new InMemoryNhanSuRepository();
        var nv = employee(); var pb = department(nv); repository.themPhongBan(pb);
        UUID employeeId = repository.idNhanVien(nv); UUID departmentId = repository.idPhongBan(pb);
        repository.xoaPhongBan(pb);
        assertTrue(repository.timNhanVienTheoId(employeeId).isEmpty());
        assertTrue(repository.timPhongBanTheoId(departmentId).isEmpty());
        repository.themPhongBan(pb);
        assertNotEquals(employeeId, repository.idNhanVien(nv));
    }

    @Test void concurrentDuplicateAddsKeepBothRecordsWithIndependentIds() throws Exception {
        var repository = new InMemoryNhanSuRepository(); var pb = department(); repository.themPhongBan(pb);
        var executor = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var tasks = List.of(employee(), employee()).stream().map(nv -> executor.submit(() -> {
                start.await(); repository.themNhanVien(pb, nv); return repository.idNhanVien(nv);
            })).toList();
            start.countDown();
            assertNotEquals(tasks.get(0).get(5, TimeUnit.SECONDS), tasks.get(1).get(5, TimeUnit.SECONDS));
            assertEquals(2, repository.danhSachNhanVien(pb).size());
        } finally { executor.shutdownNow(); }
    }
}
