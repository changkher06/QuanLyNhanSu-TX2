package repository;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import model.NhanVien;
import model.PhongBan;
import org.junit.jupiter.api.Test;

class InMemoryNhanSuRepositoryTest {
    private NhanVien employee(String id) {
        return new NhanVien("An", null, "", true, id, "Đang làm", "", "");
    }

    @Test void snapshotsCannotChangeStoreAndKeepOriginalModels() {
        var repository = new InMemoryNhanSuRepository();
        var nv = employee("NV001");
        var pb = new PhongBan("Kỹ thuật", "", new ArrayList<>(List.of(nv)));
        repository.themPhongBan(pb);
        var snapshot = repository.danhSachNhanVien(pb);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        assertThrows(UnsupportedOperationException.class, () -> repository.danhSachPhongBan().clear());
        assertSame(pb, repository.danhSachPhongBan().get(0));
        assertSame(nv, snapshot.get(0));
        repository.themNhanVien(pb, employee("NV002"));
        assertEquals(1, snapshot.size());
        assertEquals(2, repository.danhSachNhanVien(pb).size());
    }

    @Test void replacingSampleDataIsAtomicAndRejectsCrossDepartmentDuplicates() {
        var repository = new InMemoryNhanSuRepository();
        var first = new PhongBan("Một", "", new ArrayList<>(List.of(employee("NV001"))));
        repository.themPhongBan(first);
        var second = new PhongBan("Hai", "", new ArrayList<>(List.of(employee(" nv001 "))));
        assertThrows(IllegalArgumentException.class, () -> repository.thayThePhongBan(List.of(first, second)));
        assertEquals(List.of(first), repository.danhSachPhongBan());
        assertThrows(IllegalArgumentException.class, () -> repository.themPhongBan(second));
        assertEquals(1, repository.tatCaNhanVien().size());
    }

    @Test void deletingDepartmentsRemovesTheirEmployeesAndAllowsCodeReuse() {
        var repository = new InMemoryNhanSuRepository();
        var first = new PhongBan("Một", "", new ArrayList<>(List.of(employee("NV001"))));
        var second = new PhongBan("Hai", "", new ArrayList<>());
        repository.thayThePhongBan(List.of(first, second));
        repository.xoaPhongBan(first);
        assertTrue(repository.timTheoMa("NV001").isEmpty());
        repository.themNhanVien(second, employee("NV001"));
        assertEquals(1, repository.tatCaNhanVien().size());
    }

    @Test void rejectsDeletingEmployeeFromWrongDepartment() {
        var repository = new InMemoryNhanSuRepository();
        var nv = employee("NV001");
        var first = new PhongBan("Một", "", new ArrayList<>(List.of(nv)));
        var second = new PhongBan("Hai", "", new ArrayList<>());
        repository.thayThePhongBan(List.of(first, second));
        assertThrows(IllegalArgumentException.class, () -> repository.xoaNhanVien(second, nv));
        assertSame(nv, repository.timTheoMa("NV001").orElseThrow());
        repository.xoaNhanVien(first, nv);
        assertTrue(repository.tatCaNhanVien().isEmpty());
    }
}
