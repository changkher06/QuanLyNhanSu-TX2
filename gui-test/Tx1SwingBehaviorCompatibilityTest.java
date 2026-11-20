import static org.junit.jupiter.api.Assertions.*;

import java.awt.Window;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import view.GUI;

/**
 * Differential test: biên dịch chính mã 3312331 đọc từ Git vào target, nạp bằng
 * classloader riêng rồi chạy cùng action với GUI hiện tại. Không giả lập lỗi TX1.
 * Git và JDK compiler là điều kiện bắt buộc; thiếu chúng thì test báo lỗi, không skip.
 */
class Tx1SwingBehaviorCompatibilityTest {
    private static URLClassLoader baseline;
    private static Class<?> baselineGui;

    private static byte[] git(String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        byte[] output = process.getInputStream().readAllBytes();
        if (process.waitFor() != 0) throw new IOException("Git failed: " + command);
        return output;
    }

    @BeforeAll static void compileOriginalCommit() throws Exception {
        String commit = new String(git("rev-parse", "3312331^{commit}"), StandardCharsets.UTF_8).trim();
        Path work = Files.createTempDirectory(Path.of("target"), "tx1-oracle-");
        Path classes = Files.createDirectory(work.resolve("classes"));
        List<Path> sources = new ArrayList<>();
        for (String name : new String(git("ls-tree", "-r", "--name-only", commit, "src"), StandardCharsets.UTF_8).split("\\R")) {
            if (!name.endsWith(".java")) continue;
            Path source = work.resolve(name);
            Files.createDirectories(source.getParent());
            Files.write(source, git("show", commit + ":" + name));
            sources.add(source);
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Cần JDK để biên dịch baseline thật");
        assertEquals(11, sources.size(), "Snapshot phải chứa đúng các lớp TX1 đã phân tích");
        try (var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            assertTrue(compiler.getTask(null, manager, null,
                    List.of("--release", "17", "-encoding", "UTF-8", "-d", classes.toString()),
                    null, manager.getJavaFileObjectsFromPaths(sources)).call(), "Baseline phải biên dịch thành công");
        }
        Files.writeString(work.resolve("commit.txt"), commit, StandardCharsets.UTF_8);
        baseline = new URLClassLoader(new java.net.URL[] { classes.toUri().toURL() }, ClassLoader.getPlatformClassLoader());
        baselineGui = baseline.loadClass("view.GUI");
    }

    @AfterAll static void closeLoader() throws Exception { if (baseline != null) baseline.close(); }

    @AfterEach void closeWindows() {
        GUIBlackBox58Test.edt(() -> { for (Window window : Window.getWindows()) window.dispose(); });
    }

    private static JFrame frame(boolean original) {
        JFrame gui = GUIBlackBox58Test.read(() -> {
            try { return original ? (JFrame) baselineGui.getConstructor().newInstance() : new GUI(); }
            catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        });
        GUIBlackBox58Test.click(gui, "Nạp dữ liệu mẫu");
        return gui;
    }

    private static Object invoke(Object target, String method) {
        try { return target.getClass().getMethod(method).invoke(target); }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private static void calendar(Object owner, String field, String text) {
        GUIBlackBox58Test.edt(() -> {
            Object calendar = GUIBlackBox58Test.fld(owner, field);
            try {
                calendar.getClass().getMethod("setDate", Date.class).invoke(calendar,
                        text == null || text.equals("NULL") ? null : GUIBlackBox58Test.date(text));
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        });
    }

    private static List<Object> rows(JTable table) {
        List<Object> rows = new ArrayList<>();
        for (int r = 0; r < table.getRowCount(); r++) {
            List<Object> row = new ArrayList<>();
            for (int c = 0; c < table.getColumnCount(); c++) row.add(table.getValueAt(r, c));
            rows.add(row);
        }
        return rows;
    }

    private static Map<String, Object> snapshot(JFrame gui, String message) {
        return GUIBlackBox58Test.read(() -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", message);
            DefaultListModel<?> departments = GUIBlackBox58Test.fld(gui, "phongBanModel");
            List<Object> data = new ArrayList<>();
            for (int p = 0; p < departments.size(); p++) {
                for (Object nv : (List<?>) invoke(departments.get(p), "getNhanVien")) {
                    List<Object> record = new ArrayList<>();
                    record.add(p);
                    for (String getter : List.of("getId", "getFullName", "getBirthDate", "getNgayVaoLam", "getTrangThai", "getViTri")) {
                        record.add(invoke(nv, getter));
                    }
                    Object work = invoke(nv, "getNgayCong");
                    if (work == null) record.add(null);
                    else {
                        for (String getter : List.of("getSoNgayDaLam", "getSoNgayChuan", "tinhPhanTramHoanThanh")) {
                            record.add(Double.doubleToLongBits((Double) invoke(work, getter)));
                        }
                        record.add(invoke(work, "getPhanLoai"));
                    }
                    data.add(record);
                }
            }
            result.put("employees", data);
            JTable table = GUIBlackBox58Test.fld(gui, "nhanVienTable");
            result.put("table", rows(table));
            result.put("selection", table.getSelectedRow());
            result.put("employeeIdInput", ((JTextField) GUIBlackBox58Test.fld(gui, "maNhanVienField")).getText());
            result.put("total", ((JLabel) GUIBlackBox58Test.fld(gui, "tongSoCongTyLabel")).getText());
            result.put("workLabel", ((JLabel) GUIBlackBox58Test.fld(gui, "ketQuaNgayCongLabel")).getText());
            return result;
        });
    }

    private static Map<String, Object> add(boolean original, int department, String id, String birth, String hire) {
        JFrame gui = frame(original);
        GUIBlackBox58Test.edt(() -> ((JList<?>) GUIBlackBox58Test.fld(gui, "phongBanList")).setSelectedIndex(department));
        GUIBlackBox58Test.type(gui, "maNhanVienField", id.equals("EMPTY") ? "" : id);
        GUIBlackBox58Test.type(gui, "hoTenField", "Người thử");
        calendar(gui, "ngaySinhField", birth);
        calendar(gui, "ngayVaoLamField", hire);
        String message = GUIBlackBox58Test.optionalPopup(() -> GUIBlackBox58Test.click(gui, "Thêm nhân viên"));
        return snapshot(gui, message);
    }

    @ParameterizedTest
    @CsvSource({"0,NV900,01/01/2000,01/08/2025", "0,NV001,01/01/2000,01/08/2025",
            "1,NV001,01/01/2000,01/08/2025", "0,NV901,08/11/2008,07/11/2026",
            "0,NV908,07/11/2026,07/11/2026", "0,NV902,01/01/2000,31/12/1999",
            "0,EMPTY,01/01/2000,01/08/2025", "0,NV903,NULL,01/08/2025", "0,NV904,01/01/2000,NULL"})
    void addingHasSameStateAndMessagesAsOriginal(int department, String id, String birth, String hire) {
        assertEquals(add(true, department, id, birth, hire), add(false, department, id, birth, hire));
    }

    private static Map<String, Object> work(boolean original, String done, String standard) {
        JFrame gui = frame(original);
        GUIBlackBox58Test.edt(() -> ((JTable) GUIBlackBox58Test.fld(gui, "nhanVienTable")).setRowSelectionInterval(0, 0));
        GUIBlackBox58Test.type(gui, "ngayCongField", done.equals("EMPTY") ? "" : done);
        GUIBlackBox58Test.type(gui, "ngayChuanField", standard);
        String message = GUIBlackBox58Test.optionalPopup(() -> GUIBlackBox58Test.click(gui, "Tính ngày công"));
        return snapshot(gui, message);
    }

    @ParameterizedTest
    @CsvSource({"0,26", "26,26", "19.49,26", "19.5,26", "23.4,26", "12.345,26", "13,25.123",
            "NaN,26", "Infinity,26", "13,NaN", "13,Infinity", "Infinity,Infinity", "-Infinity,26",
            "-0.01,26", "26.01,26", "5,0", "abc,26", "EMPTY,26", "1e1,26", "0x1.0p0,26"})
    void workdaysHaveSameBitsClassificationMessagesAndUi(String done, String standard) {
        assertEquals(work(true, done, standard), work(false, done, standard));
    }

    private static Map<String, Object> search(boolean original, String criterion, String value) {
        JFrame gui = frame(original);
        JDialog dialog = GUIBlackBox58Test.read(() -> {
            try {
                DefaultListModel<?> model = GUIBlackBox58Test.fld(gui, "phongBanModel");
                Class<?> type = original ? baseline.loadClass("view.TimKiemNhanVienDialog") : view.TimKiemNhanVienDialog.class;
                return (JDialog) type.getConstructor(Window.class, model.get(0).getClass(), java.text.SimpleDateFormat.class,
                        String.class, Runnable.class).newInstance(gui, model.get(0), new java.text.SimpleDateFormat("dd/MM/yyyy"),
                        criterion, (Runnable) () -> {});
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        });
        GUIBlackBox58Test.type(dialog, "valueField", value.equals("EMPTY") ? "" : value);
        String message = GUIBlackBox58Test.optionalPopup(() -> GUIBlackBox58Test.click(dialog, "Áp dụng"));
        Map<String, Object> result = snapshot(gui, message);
        GUIBlackBox58Test.edt(() -> {
            result.put("searchRows", rows(GUIBlackBox58Test.fld(dialog, "table")));
            result.put("searchCount", ((JLabel) GUIBlackBox58Test.fld(dialog, "ketQuaLocLabel")).getText());
        });
        return result;
    }

    @ParameterizedTest
    @CsvSource({"Họ tên,aN", "Họ tên,EMPTY", "Họ tên,không tồn tại", "Mã nhân viên,2",
            "Mã nhân viên,1-2", "Mã nhân viên,3-1", "Mã nhân viên,NV00", "Mã nhân viên,9999999999999999999999",
            "Tuổi,-1", "Tuổi,30-18", "Tuổi,abc", "Tuổi,18-30", "Thâm niên,1", "Trạng thái,đANG", "Vị trí,Nhân viên"})
    void filtersHaveSameFallbackErrorsAndResults(String criterion, String value) {
        assertEquals(search(true, criterion, value), search(false, criterion, value));
    }

    private static Map<String, Object> profile(boolean original, String hire) {
        JFrame gui = frame(original);
        JDialog dialog = GUIBlackBox58Test.read(() -> {
            try {
                DefaultListModel<?> departments = GUIBlackBox58Test.fld(gui, "phongBanModel");
                Object nv = ((List<?>) invoke(departments.get(0), "getNhanVien")).get(0);
                Class<?> type = original ? baseline.loadClass("view.GUINhanVien") : view.GUINhanVien.class;
                return (JDialog) type.getConstructor(Window.class, nv.getClass(), java.text.SimpleDateFormat.class, Runnable.class)
                        .newInstance(gui, nv, new java.text.SimpleDateFormat("dd/MM/yyyy"), (Runnable) () -> {});
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        });
        GUIBlackBox58Test.type(dialog, "idField", "NV002");
        calendar(dialog, "hireDateField", hire);
        String message = GUIBlackBox58Test.optionalPopup(() -> GUIBlackBox58Test.click(dialog, "Lưu hồ sơ"));
        return snapshot(gui, message);
    }

    @ParameterizedTest
    @CsvSource({"31/12/1999", "31/12/2017"})
    void profileKeepsDuplicateCodesAgeAndPartialUpdateOrder(String hire) {
        assertEquals(profile(true, hire), profile(false, hire));
    }

    @Test void inlineDateAndDuplicateCodeSettersMatchOriginal() {
        List<Map<String, Object>> results = new ArrayList<>();
        for (boolean original : List.of(true, false)) {
            JFrame gui = frame(original);
            GUIBlackBox58Test.edt(() -> {
                JTable table = GUIBlackBox58Test.fld(gui, "nhanVienTable");
                table.getModel().setValueAt("NV002", 0, 0);
                table.getModel().setValueAt("31/12/1999", 0, 3);
            });
            results.add(snapshot(gui, null));
        }
        assertEquals(results.get(0), results.get(1));
    }
}
