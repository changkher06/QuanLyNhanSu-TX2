import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.*;
import model.NhanVien;
import model.PhongBan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryNhanSuRepository;
import view.*;

/** Kiểm tra wiring mới qua các action Swing, không thay bộ 58 test TX1. */
class ServiceSwingIntegrationTest {
    private GUI gui;
    private InMemoryNhanSuRepository repository;
    private PhongBan department;
    private NhanVien first;
    private NhanVien second;

    private static Date date(String text) {
        return Date.from(LocalDate.parse(text).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static NhanVien employee(String id, String name) {
        return new NhanVien(name, date("2000-01-01"), "", true, id,
                "Đang làm", "Nhân viên", "", date("2024-01-01"), null);
    }

    @BeforeEach void setup() throws Exception {
        repository = new InMemoryNhanSuRepository();
        first = employee("NV001", "Zoe");
        second = employee("NV002", "An");
        department = new PhongBan("Kỹ thuật", "", new ArrayList<>(List.of(first, second)));
        repository.themPhongBan(department);
        gui = edt(() -> GUI.create(repository));
    }

    @AfterEach void cleanup() throws Exception {
        edt(() -> {
            for (Window window : Window.getWindows()) window.dispose();
            return null;
        });
    }

    private static <T> T edt(Supplier<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action::get);
        SwingUtilities.invokeAndWait(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(owner);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private static JButton button(Container root, String title) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton b && b.getText().equals(title)) return b;
            if (child instanceof Container container) {
                JButton found = button(container, title);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Chờ và đóng đúng message dialog của action; luôn chuyển lỗi EDT về test. */
    private static String popup(Runnable action) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            try { action.run(); } catch (Throwable throwable) { error.set(throwable); }
            finally { finished.countDown(); }
        });
        String message = null;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (message == null && System.nanoTime() < deadline) {
            message = edt(() -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog && dialog.isShowing()) {
                        for (Component child : dialog.getContentPane().getComponents()) {
                            if (child instanceof JOptionPane pane) {
                                String value = String.valueOf(pane.getMessage());
                                pane.setValue(JOptionPane.OK_OPTION);
                                dialog.dispose();
                                return value;
                            }
                        }
                    }
                }
                return null;
            });
            if (message == null) Thread.sleep(10);
        }
        assertTrue(finished.await(5, TimeUnit.SECONDS), "Action Swing không kết thúc");
        if (error.get() != null) throw new AssertionError(error.get());
        assertNotNull(message, "Action phải hiển thị thông báo");
        return message;
    }

    @Test void injectedRepositoryIsTheSameStoreUsedByEmployeeForm() throws Exception {
        assertSame(repository, gui.getNhanVienService().getRepository());
        edt(() -> {
            ((JTextField) field(gui, "maNhanVienField")).setText("NV003");
            ((JTextField) field(gui, "hoTenField")).setText("Lan");
            ((CalendarDateField) field(gui, "ngaySinhField")).setDate(date("2000-01-01"));
            ((CalendarDateField) field(gui, "ngayVaoLamField")).setDate(date("2024-01-01"));
            button(gui, "Thêm nhân viên").doClick();
            return null;
        });
        assertEquals(3, repository.tatCaNhanVien().size());
        assertSame(department.getNhanVien().get(2), repository.timTheoMa("NV003").orElseThrow());
        assertEquals(3, edt(() -> ((JTable) field(gui, "nhanVienTable")).getRowCount()));
    }

    @Test void mainTableCannotRenameEmployeeToExistingCode() throws Exception {
        String message = popup(() -> {
            JTable table = field(gui, "nhanVienTable");
            table.getModel().setValueAt("NV002", 0, 0);
        });
        assertTrue(message.contains("đã tồn tại"));
        assertEquals("NV001", first.getId());
        assertEquals(2, repository.tatCaNhanVien().size());
    }

    @Test void searchTableUsesCompanyStoreAndRejectsCodeFromAnotherDepartment() throws Exception {
        NhanVien outside = employee("NV100", "Mai");
        repository.themPhongBan(new PhongBan("Kinh doanh", "", new ArrayList<>(List.of(outside))));
        TimKiemNhanVienDialog search = edt(() -> new TimKiemNhanVienDialog(gui, department,
                new SimpleDateFormat("dd/MM/yyyy"), "Họ tên", () -> {}));
        assertSame(repository, search.getNhanVienService().getRepository());
        String message = popup(() -> ((JTable) field(search, "table")).getModel().setValueAt("NV100", 0, 0));
        assertTrue(message.contains("đã tồn tại"));
        assertEquals("NV001", first.getId());
        assertSame(outside, repository.timTheoMa("NV100").orElseThrow());
    }

    @Test void profileValidationDoesNotPartiallyRenameOnInvalidDates() throws Exception {
        GUINhanVien profile = edt(() -> new GUINhanVien(gui, first,
                new SimpleDateFormat("dd/MM/yyyy"), () -> {}));
        edt(() -> {
            ((JTextField) field(profile, "idField")).setText("NV003");
            ((CalendarDateField) field(profile, "hireDateField")).setDate(date("2017-12-31"));
            return null;
        });
        assertTrue(popup(() -> button(profile, "Lưu hồ sơ").doClick()).contains("18 tuổi"));
        assertEquals("NV001", first.getId());
        assertEquals(date("2024-01-01"), first.getNgayVaoLam());
    }

    @Test void workdaysFollowEmployeeIdentityAfterSortingAndRefresh() throws Exception {
        edt(() -> {
            JTable table = field(gui, "nhanVienTable");
            table.getRowSorter().toggleSortOrder(1);
            assertEquals("NV002", table.getValueAt(0, 0));
            table.setRowSelectionInterval(0, 0);
            ((JTextField) field(gui, "ngayCongField")).setText("23.4");
            ((JTextField) field(gui, "ngayChuanField")).setText("26");
            return null;
        });
        assertTrue(popup(() -> button(gui, "Tính ngày công").doClick()).contains("Giỏi"));
        assertNull(first.getNgayCong());
        assertEquals(23.4, second.getNgayCong().getSoNgayDaLam());
        assertEquals("NV002", edt(() -> {
            JTable table = field(gui, "nhanVienTable");
            return table.getValueAt(table.getSelectedRow(), 0);
        }));
    }
}
