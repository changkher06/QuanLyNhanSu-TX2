import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.*;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import java.awt.Rectangle;
import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import model.*;
import view.*;

/**
 * 58 kiểm thử GUI khớp TEST_CASES của TX1_2_2_BlackBox_58_TestCases_Theo_YeuCau.xlsx.
 * 15 TN + 16 NC + 15 TK + 12 CV. 10 quy tắc QT-01..QT-10 đã chốt.
 *
 * Điều kiện tiên quyết: dùng GUI.java phiên bản nạp dữ liệu mẫu cố định:
 * PB-KT = NV001/002/003; PB-KD = NV100. Mỗi test tạo GUI mới và bấm nút
 * "Nạp dữ liệu mẫu" -> bảo đảm độc lập. Nghiệp vụ thực hiện qua Swing GUI;
 * đọc model và dựng tình huống chuyên biệt chỉ dùng để setup/assert.
 *
 * IMPORTANT: thất bại do sai quy tắc nghiệp vụ phải giữ FAIL, không đổi expected.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class GUIBlackBox58Test {
    private Robot robot;
    private FrameFixture fixture;
    private GUI gui;
    private final List<JDialog> dialogs = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));

    /** Lưu ảnh màn hình khi có assertion FAIL, trước khi cleanup đóng GUI. */
    @RegisterExtension
    final TestExecutionExceptionHandler evidenceOnFailure = (context, error) -> {
        try {
            if (gui != null && gui.isShowing()) {
                Path dir = Path.of("target", "gui-evidence");
                Files.createDirectories(dir);
                String id = context.getRequiredTestMethod().getName().replace('_', '-');
                Point p = read(gui::getLocationOnScreen);
                Rectangle r = read(() -> new Rectangle(p.x,p.y,gui.getWidth(),gui.getHeight()));
                ImageIO.write(new java.awt.Robot().createScreenCapture(r),"png",dir.resolve(id+".png").toFile());
                System.err.println("[GUI EVIDENCE] " + dir.resolve(id + ".png"));
            }
        } catch (Exception screenshotError) {
            System.err.println("Không chụp được ảnh minh chứng: " + screenshotError.getMessage());
        }
        throw error;
    };

    @BeforeEach
    void setup() {
        fmt.setLenient(false);
        robot = BasicRobot.robotWithNewAwtHierarchy();
        gui = GuiActionRunner.execute(GUI::new);
        fixture = new FrameFixture(robot, gui);
        fixture.show();
        fixture.requireVisible();
        click(gui, "Nạp dữ liệu mẫu");
        assertEquals(2, deptCount(), "QT-10: GUI.java cần bản dữ liệu cố định 2 phòng ban");
        assertEquals(3, dept(0).getNhanVien().size(), "QT-10: PB-KT phải có 3 NV");
        assertEquals(1, dept(1).getNhanVien().size(), "QT-10: PB-KD phải có 1 NV");
        assertEquals("NV001", dept(0).getNhanVien().get(0).getId());
        assertEquals("NV100", dept(1).getNhanVien().get(0).getId());
        selectDept(0);
    }

    @AfterEach
    void cleanup() {
        for (JDialog d : dialogs) {
            try { edt(() -> d.dispose()); } catch (Exception ignored) { }
        }
        if (robot != null) robot.cleanUp();
    }

    // ===== Swing actions + read-only assertions =====
    static void edt(Runnable r) { GuiActionRunner.execute(() -> { r.run(); return null; }); }
    static <T> T read(Supplier<T> s) { return GuiActionRunner.execute(s::get); }
    @SuppressWarnings("unchecked")
    static <T> T fld(Object o, String name) {
        Class<?> cls = o.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return (T) f.get(o);
            } catch (NoSuchFieldException e) { cls = cls.getSuperclass(); }
            catch (Exception e) { throw new AssertionError("Không đọc được component " + name, e); }
        }
        throw new AssertionError("Không thấy component: " + o.getClass().getSimpleName() + "." + name);
    }
    static Date date(String s) {
        try {
            SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yyyy");
            sf.setLenient(false); return sf.parse(s);
        } catch (Exception e) { throw new AssertionError("Ngày fixture sai: " + s, e); }
    }
    static String dateText(Date d) {
        SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yyyy");
        return sf.format(d);
    }
    static String todayPlusYears(int years) {
        LocalDate x = LocalDate.now().plusYears(years);
        return String.format("%02d/%02d/%04d", x.getDayOfMonth(), x.getMonthValue(), x.getYear());
    }
    static int fullYears(Date d) {
        return Period.between(d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()).getYears();
    }
    static <T extends Component> List<T> find(Component root, Class<T> cls) {
        List<T> out = new ArrayList<>();
        if (cls.isInstance(root)) out.add(cls.cast(root));
        if (root instanceof Container) for (Component c : ((Container) root).getComponents()) out.addAll(find(c, cls));
        return out;
    }
    static JButton btn(Component root, String title) {
        return find(root, JButton.class).stream().filter(b -> title.equals(b.getText()))
                .findFirst().orElseThrow(() -> new AssertionError("Không tìm thấy JButton: " + title));
    }
    static void click(Component root, String title) { edt(() -> btn(root, title).doClick()); }
    static void type(Object owner, String f, String text) { edt(() -> ((JTextField) fld(owner, f)).setText(text)); }
    static void calendar(Object owner, String f, String val) {
        edt(() -> ((CalendarDateField) fld(owner, f)).setDate(val == null ? null : date(val)));
    }
    static void calendarText(Object owner, String f, String val) {
        edt(() -> {
            CalendarDateField cal = fld(owner, f);
            JTextField field = find(cal, JTextField.class).get(0);
            field.setText(val);
        });
    }
    static void select(Object owner, String f, String val) {
        edt(() -> ((JComboBox<?>) fld(owner, f)).setSelectedItem(val));
    }
    static String label(Object owner, String f) { return read(() -> ((JLabel)fld(owner,f)).getText()); }
    static int count(Object owner, String f) { return read(() -> ((JTable)fld(owner,f)).getRowCount()); }
    static String cell(Object owner, String f, int row, int col) {
        return String.valueOf(read(() -> ((JTable)fld(owner,f)).getValueAt(row,col)));
    }
    static Set<String> ids(Object owner, String f) {
        int n = count(owner,f); Set<String> out = new HashSet<>();
        for (int i=0;i<n;i++) out.add(cell(owner,f,i,0));
        return out;
    }
    @SuppressWarnings("unchecked")
    private DefaultListModel<PhongBan> deptModel() { return fld(gui,"phongBanModel"); }
    private int deptCount() { return read(() -> deptModel().getSize()); }
    private PhongBan dept(int i) { return read(() -> deptModel().get(i)); }
    private void selectDept(int i) { edt(() -> ((JList<?>)fld(gui,"phongBanList")).setSelectedIndex(i)); }
    private void unselectDept() { edt(() -> ((JList<?>)fld(gui,"phongBanList")).clearSelection()); }
    private void employeeRow(int row) { edt(() -> ((JTable)fld(gui,"nhanVienTable")).setRowSelectionInterval(row,row)); }
    private void unselectEmployee() { edt(() -> ((JTable)fld(gui,"nhanVienTable")).clearSelection()); }
    private NhanVien employee(String id) {
        for (int i=0;i<deptCount();i++) for (NhanVien n:dept(i).getNhanVien()) if (id.equals(n.getId())) return n;
        throw new AssertionError("Dữ liệu nền thiếu: " + id);
    }
    private int totalEmployees() {
        int sum=0; for (int i=0;i<deptCount();i++) sum+=dept(i).getNhanVien().size(); return sum;
    }
    private void employeeForm(String id, String name, String birth, String hire) {
        type(gui,"maNhanVienField",id);
        type(gui,"hoTenField",name);
        calendar(gui,"ngaySinhField",birth);
        calendar(gui,"ngayVaoLamField",hire);
        type(gui,"soDienThoaiField","0912345678");
        type(gui,"queQuanField","Quảng Ninh");
        type(gui,"viTriField","Nhân viên");
        select(gui,"gioiTinhBox","Nam");
        select(gui,"trangThaiBox","Đang làm");
    }

    /**
     * Bắt mọi JOptionPane khi click, kể cả thông báo lỗi hoặc thông báo thành công.
     * Đóng modal trên EDT để action gốc không bị treo. Không báo lỗi nếu không có popup.
     */
    static String optionalPopup(Runnable action, int choice) {
        AtomicReference<String> message = new AtomicReference<>();
        AtomicReference<Throwable> watcherErr = new AtomicReference<>();
        AtomicBoolean finished = new AtomicBoolean(false);
        Thread watcher = new Thread(() -> {
            while (!finished.get()) {
                try {
                    for (Window w : Window.getWindows()) {
                        if (!(w instanceof JDialog) || !w.isShowing() || !((JDialog)w).isModal()) continue;
                        JDialog d=(JDialog)w;
                        List<JOptionPane> panes=find(d,JOptionPane.class);
                        if (panes.isEmpty()) continue;
                        SwingUtilities.invokeAndWait(() -> {
                            if (!d.isShowing()) return;
                            try {
                                JOptionPane p=panes.get(0);
                                message.set(String.valueOf(p.getMessage()));
                                p.setValue(choice);
                                d.dispose();
                            } catch (Throwable t) { watcherErr.set(t); d.dispose(); }
                        });
                        return;
                    }
                    Thread.sleep(10);
                } catch (InterruptedException e) { return; }
                catch (Exception e) { watcherErr.set(e); return; }
            }
        }, "gui-modal-responder");
        watcher.setDaemon(true); watcher.start();
        try { action.run(); }
        finally {
            finished.set(true); watcher.interrupt();
            try { watcher.join(1000); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        if (watcherErr.get()!=null) throw new AssertionError("Không thể xử lý hộp thoại Swing",watcherErr.get());
        return message.get();
    }
    static String optionalPopup(Runnable action) { return optionalPopup(action,JOptionPane.OK_OPTION); }
    static String requiredPopup(Runnable action, int choice) {
        String msg=optionalPopup(action,choice);
        assertNotNull(msg,"Yêu cầu hiển thị hộp thoại nhưng không có hộp thoại");
        return msg;
    }
    static String requiredPopup(Runnable action) { return requiredPopup(action,JOptionPane.OK_OPTION); }
    static void expectError(String message, String token) {
        assertNotNull(message,"Không thấy thông báo từ chối đầu vào");
        assertTrue(message.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT)),
                "Nội dung thông báo không đúng: "+message);
    }
    private String addWithPopup() { return optionalPopup(() -> click(gui,"Thêm nhân viên")); }
    private void work(String done, String standard) { type(gui,"ngayCongField",done);type(gui,"ngayChuanField",standard); }
    private String calculate() { return requiredPopup(() -> click(gui,"Tính ngày công")); }
    private void expectWorkOk(String done,String standard,double expected,String grade) {
        employeeRow(0);
        work(done,standard);
        String msg=calculate();
        assertTrue(msg.replace(',', '.').contains(String.format(Locale.US,"%.1f%%",expected)), "Sai phần trăm: "+msg);
        assertTrue(msg.contains("Phân loại: " + grade),"Sai xếp loại: "+msg);
        NgayCong nc=employee("NV001").getNgayCong();
        assertEquals(expected,nc.tinhPhanTramHoanThanh(),0.0001);
        assertEquals(grade,nc.getPhanLoai());
        assertEquals(Double.parseDouble(done),nc.getSoNgayDaLam(),0.0001);
        assertEquals(Double.parseDouble(standard),nc.getSoNgayChuan(),0.0001);
    }
    private void expectWorkBad(String done,String standard) {
        employeeRow(0);
        double before=employee("NV001").getNgayCong().getSoNgayDaLam();
        work(done,standard);
        String msg=requiredPopup(() -> click(gui,"Tính ngày công"));
        assertTrue(msg.toLowerCase(Locale.ROOT).contains("không hợp lệ") ||
                   msg.toLowerCase(Locale.ROOT).contains("số hợp lệ"),"Phải từ chối dữ liệu: "+msg);
        double after=employee("NV001").getNgayCong().getSoNgayDaLam();
        assertEquals(before,after,0.0001,"Không được cập nhật ngày công sai");
        assertTrue(Double.isFinite(after),"Không được lưu NaN/Infinity");
    }

    // ===== F02 – THÊM NHÂN VIÊN (15 test) =====
    @Test void TN_01() {
        int before=totalEmployees();
        employeeForm("NV900","Nguyễn Minh","01/01/2000","01/08/2025");
        click(gui,"Thêm nhân viên");
        assertEquals(before+1,totalEmployees());
        assertEquals(4,dept(0).getNhanVien().size());
        assertEquals("NV900",dept(0).getNhanVien().get(3).getId());
        assertTrue(ids(gui,"nhanVienTable").contains("NV900"));
        assertEquals(0,dept(0).getNhanVien().get(3).getNgayCong().getSoNgayDaLam(),0.0001);
        assertEquals(26,dept(0).getNhanVien().get(3).getNgayCong().getSoNgayChuan(),0.0001);
        assertEquals("",read(() -> ((JTextField)fld(gui,"maNhanVienField")).getText()));
        assertTrue(label(gui,"tongSoCongTyLabel").contains("5"));
    }
    @Test void TN_02() {
        int before=totalEmployees();unselectDept();
        employeeForm("NV901","Minh","01/01/2000","01/08/2025");
        optionalPopup(() -> click(gui,"Thêm nhân viên"));
        assertEquals(before,totalEmployees());
        assertFalse(ids(gui,"nhanVienTable").contains("NV901"));
    }
    @Test void TN_03() {
        int before=totalEmployees();employeeForm("","Tên hợp lệ","01/01/2000","01/08/2025");
        expectError(addWithPopup(),"không được để trống");assertEquals(before,totalEmployees());
    }
    @Test void TN_04() {
        int before=totalEmployees();employeeForm("NV902","   ","01/01/2000","01/08/2025");
        expectError(addWithPopup(),"không được để trống");assertEquals(before,totalEmployees());
    }
    @Test void TN_05() {
        int before=totalEmployees();employeeForm("NV903","Minh",null,"01/08/2025");
        expectError(addWithPopup(),"Ngày sinh");assertEquals(before,totalEmployees());
    }
    @Test void TN_06() {
        int before=totalEmployees();employeeForm("NV904","Minh",null,"01/08/2025");
        calendarText(gui,"ngaySinhField","31/02/2000");
        expectError(addWithPopup(),"Ngày sinh");assertEquals(before,totalEmployees());
    }
    @Test void TN_07() {
        int before=totalEmployees();employeeForm("NV905","Minh","01/01/2000",null);
        calendarText(gui,"ngayVaoLamField","2025-08-01");
        expectError(addWithPopup(),"Ngày vào làm");assertEquals(before,totalEmployees());
    }
    @Test void TN_08() {
        int before=totalEmployees();employeeForm("NV906","Minh","01/01/2000","31/12/1999");
        expectError(addWithPopup(),"trước ngày sinh");assertEquals(before,totalEmployees());
    }
    @Test void TN_09() {
        selectDept(1);int before=totalEmployees();int beforeDept=dept(1).getNhanVien().size();
        employeeForm("NV001","Người khác phòng","01/01/2000","01/08/2025");
        addWithPopup();
        assertEquals(before,totalEmployees(),"QT-01: Trùng NV001 giữa 2 phòng phải bị từ chối");
        assertEquals(beforeDept,dept(1).getNhanVien().size());
    }
    @Test void TN_10() {
        int before=totalEmployees();employeeForm("NV001","Người khác","01/01/2000","01/08/2025");
        addWithPopup();
        assertEquals(before,totalEmployees(),"QT-01: Trùng NV001 cùng phòng phải bị từ chối");
        assertEquals(3,dept(0).getNhanVien().size());
    }
    @Test void TN_11() {
        int before=totalEmployees();employeeForm("NV901","Chưa đủ tuổi","08/11/2008","07/11/2026");
        addWithPopup();assertEquals(before,totalEmployees(),"QT-04: Chưa đủ 18 tuổi không được thêm");
    }
    @Test void TN_12() {
        int before=totalEmployees();employeeForm("NV901","Đúng 18 tuổi","07/11/2008","07/11/2026");
        click(gui,"Thêm nhân viên");
        assertEquals(before+1,totalEmployees(),"QT-04: Đúng sinh nhật 18 tuổi phải được chấp nhận");
    }
    @Test void TN_13() {
        employeeForm("NV907","Nguyễn Hương","01/01/2000","01/08/2025");
        select(gui,"gioiTinhBox","Nữ");select(gui,"trangThaiBox","Thử việc");
        click(gui,"Thêm nhân viên");NhanVien n=employee("NV907");
        assertFalse(Boolean.TRUE.equals(n.getGioiTinh()));assertEquals("Thử việc",n.getTrangThai());
    }
    @Test void TN_14() {
        int before=totalEmployees();employeeForm("NV908","Người mới sinh","07/11/2026","07/11/2026");
        addWithPopup();assertEquals(before,totalEmployees(),"QT-04: Ngày vào làm trùng ngày sinh nhưng chưa đủ tuổi phải bị từ chối");
    }
    @Test void TN_15() {
        int before=totalEmployees();employeeForm("NV908","Thiếu ngày làm","01/01/2000",null);
        expectError(addWithPopup(),"Ngày vào làm");assertEquals(before,totalEmployees());
    }

    // ===== F05 – TÍNH NGÀY CÔNG (16 test) =====
    @Test void NC_01() {
        unselectEmployee();work("13","26");
        expectError(requiredPopup(() -> click(gui,"Tính ngày công")),"chọn nhân viên");
        assertEquals(0,employee("NV001").getNgayCong().getSoNgayDaLam(),0.0001);
    }
    @Test void NC_02() { expectWorkOk("0","26",0,"Kém"); }
    @Test void NC_03() { expectWorkOk("26","26",100,"Xuất sắc"); }
    @Test void NC_04() { expectWorkOk("19.5","26",75,"Khá"); }
    @Test void NC_05() {
        expectWorkOk("19.49","26",19.49/26*100,"Trung bình");
        assertTrue(cell(gui,"nhanVienTable",0,8).replace(',', '.').contains("75.0%"),"Hiển thị làm tròn 75.0%");
    }
    @Test void NC_06() { expectWorkOk("23.4","26",90,"Giỏi"); }
    @Test void NC_07() { expectWorkBad("-0.01","26"); }
    @Test void NC_08() { expectWorkBad("26.01","26"); }
    @Test void NC_09() { expectWorkBad("5","0"); }
    @Test void NC_10() { expectWorkBad("abc","26"); }
    @Test void NC_11() {
        List<String> problems=new ArrayList<>();
        for (String value:Arrays.asList("NaN","Infinity")) {
            employeeRow(0);work(value,"26");
            String msg=optionalPopup(() -> click(gui,"Tính ngày công"));
            NgayCong after=employee("NV001").getNgayCong();
            boolean refused=msg!=null &&
                    (msg.toLowerCase(Locale.ROOT).contains("không hợp lệ") ||
                     msg.toLowerCase(Locale.ROOT).contains("số hợp lệ"));
            if (!refused || after==null || !Double.isFinite(after.getSoNgayDaLam()) ||
                    Math.abs(after.getSoNgayDaLam()) > 0.00001) problems.add(value+": "+msg);
        }
        assertTrue(problems.isEmpty(),"QT-06: Phải từ chối mọi số phi hữu hạn: "+problems);
    }
    @Test void NC_12() { expectWorkBad("12.345","26"); }
    @Test void NC_13() {
        assertNull(employee("NV002").getNgayCong(),"Dữ liệu nền NV002 phải chưa tính");
        assertTrue(cell(gui,"nhanVienTable",1,8).contains("Chưa tính"));
        employeeRow(1);
        assertEquals("26",read(() -> ((JTextField)fld(gui,"ngayChuanField")).getText()));
    }
    @Test void NC_14() {
        for (NhanVien n:dept(0).getNhanVien()) n.setNgayCong(new NgayCong(n,17,26));
        NhanVien other=employee("NV100");other.setNgayCong(new NgayCong(other,21,26));
        selectDept(1);selectDept(0); // refresh UI from fixture
        String msg=requiredPopup(() -> click(gui,"Reset ngày công phòng ban"),JOptionPane.YES_OPTION);
        assertTrue(msg.contains("reset") || msg.contains("Reset"),"Phải hỏi xác nhận reset");
        for (NhanVien n:dept(0).getNhanVien()) {
            assertEquals(0,n.getNgayCong().getSoNgayDaLam(),0.0001);
            assertEquals(26,n.getNgayCong().getSoNgayChuan(),0.0001);
        }
        assertEquals(21,other.getNgayCong().getSoNgayDaLam(),0.0001,"Không reset phòng khác");
    }
    @Test void NC_15() {
        for (NhanVien n:dept(0).getNhanVien()) n.setNgayCong(new NgayCong(n,17,26));
        selectDept(1);selectDept(0);
        requiredPopup(() -> click(gui,"Reset ngày công phòng ban"),JOptionPane.NO_OPTION);
        for (NhanVien n:dept(0).getNhanVien()) assertEquals(17,n.getNgayCong().getSoNgayDaLam(),0.0001);
    }
    @Test void NC_16() {
        expectWorkBad("13","xyz");
        expectWorkBad("13","25.123");
    }

    // ===== F06 – TÌM KIẾM NHÂN VIÊN (15 test) =====
    private TimKiemNhanVienDialog search() { return searchIn(dept(0),"Họ tên"); }
    private TimKiemNhanVienDialog searchIn(PhongBan pb,String criterion) {
        TimKiemNhanVienDialog d=read(() -> new TimKiemNhanVienDialog(gui,pb,fmt,criterion,()->{}));
        dialogs.add(d);edt(() -> d.setVisible(true));return d;
    }
    private void filter(TimKiemNhanVienDialog d,String criterion,String value) {
        select(d,"criterionBox",criterion);type(d,"valueField",value);click(d,"Áp dụng");
    }
    private void assertSearchResult(TimKiemNhanVienDialog d, String... expectedIds) {
        assertEquals(Set.of(expectedIds),ids(d,"table"),"Sai tập mã nhân viên lọc");
        assertEquals("Số lượng đã lọc: "+expectedIds.length,label(d,"ketQuaLocLabel"));
    }
    @Test void TK_01() {
        unselectDept();
        // Đi qua menu thật thay vì khởi tạo dialog độc lập để kiểm tra điều kiện chưa chọn PB.
        JMenuItem menu=read(() -> gui.getJMenuBar().getMenu(0).getItem(0));
        String msg=requiredPopup(() -> edt(menu::doClick));
        expectError(msg,"chọn phòng ban");
    }
    @Test void TK_02() {
        TimKiemNhanVienDialog d=search();filter(d,"Họ tên","aN");
        assertTrue(ids(d,"table").contains("NV001"));
        assertFalse(ids(d,"table").contains("NV002"));
        assertEquals("Số lượng đã lọc: "+count(d,"table"),label(d,"ketQuaLocLabel"));
    }
    @Test void TK_03() {
        TimKiemNhanVienDialog d=search();select(d,"criterionBox","Họ tên");type(d,"valueField","   ");
        expectError(requiredPopup(() -> click(d,"Áp dụng")),"Vui lòng nhập");
    }
    @Test void TK_04() { TimKiemNhanVienDialog d=search();filter(d,"Mã nhân viên","2");assertSearchResult(d,"NV002"); }
    @Test void TK_05() { TimKiemNhanVienDialog d=search();filter(d,"Mã nhân viên","1-2");assertSearchResult(d,"NV001","NV002"); }
    @Test void TK_06() {
        TimKiemNhanVienDialog d=search();select(d,"criterionBox","Mã nhân viên");type(d,"valueField","3-1");
        String msg=optionalPopup(() -> click(d,"Áp dụng"));
        assertNotNull(msg,"QT-05: Khoảng mã đảo 3-1 phải có thông báo lỗi");
        assertTrue(msg.toLowerCase(Locale.ROOT).contains("không hợp lệ") ||
                   msg.toLowerCase(Locale.ROOT).contains("khoảng"),"Sai thông báo khoảng đảo: "+msg);
        assertEquals(3,count(d,"table"),"Lỗi tìm kiếm không được lặng lẽ lọc dữ liệu");
    }
    @Test void TK_07() { TimKiemNhanVienDialog d=search();filter(d,"Mã nhân viên","NV00");assertSearchResult(d,"NV001","NV002","NV003"); }
    @Test void TK_08() {
        TimKiemNhanVienDialog d=search();select(d,"criterionBox","Giới tính");select(d,"genderBox","Nữ");
        click(d,"Áp dụng");assertSearchResult(d,"NV003");
    }
    @Test void TK_09() {
        TimKiemNhanVienDialog d=search();filter(d,"Tuổi","18-30");
        Set<String> expected=dept(0).getNhanVien().stream()
            .filter(n -> fullYears(n.getBirthDate())>=18 && fullYears(n.getBirthDate())<=30)
            .map(NhanVien::getId).collect(Collectors.toSet());
        assertEquals(expected,ids(d,"table"),"Tuổi tính theo năm tròn tại ngày chạy");
        assertEquals("Số lượng đã lọc: "+expected.size(),label(d,"ketQuaLocLabel"));
    }
    @Test void TK_10() {
        // Fixture bổ sung 1 nhân viên thâm niên đúng 1 năm, tránh test trống phụ thuộc năm máy chạy.
        NhanVien n=employee("NV003");
        edt(() -> n.setNgayVaoLam(date(todayPlusYears(-1))));
        TimKiemNhanVienDialog d=search();filter(d,"Thâm niên","1");
        assertTrue(ids(d,"table").contains("NV003"),"Phải tìm được thâm niên 1 năm tròn");
        for (NhanVien e:dept(0).getNhanVien()) if (ids(d,"table").contains(e.getId()))
            assertEquals(1,fullYears(e.getNgayVaoLam()));
    }
    @Test void TK_11() {
        TimKiemNhanVienDialog d=search();select(d,"criterionBox","Tuổi");type(d,"valueField","abc");
        expectError(requiredPopup(() -> click(d,"Áp dụng")),"số nguyên không âm");
    }
    @Test void TK_12() {
        TimKiemNhanVienDialog d=search();filter(d,"Trạng thái","đANG");
        assertSearchResult(d,"NV001","NV002");
        edt(() -> employee("NV001").setViTri("Trưởng nhóm")); // fixture riêng cho tìm vị trí
        filter(d,"Vị trí","trưởng");assertSearchResult(d,"NV001");
    }
    @Test void TK_13() {
        TimKiemNhanVienDialog d=search();filter(d,"Họ tên","zzz_khong_co");
        assertSearchResult(d);
        click(d,"Hiển thị tất cả");assertSearchResult(d,"NV001","NV002","NV003");
    }
    @Test void TK_14() {
        TimKiemNhanVienDialog d=search();filter(d,"Họ tên","Nguyễn");
        assertSearchResult(d,"NV001");
        assertFalse(ids(d,"table").contains("NV100"),"Không lọt NV100 từ PB-KD");
    }
    @Test void TK_15() {
        TimKiemNhanVienDialog d=search();select(d,"criterionBox","Tuổi");type(d,"valueField","-1");
        expectError(requiredPopup(() -> click(d,"Áp dụng")),"không âm");
        type(d,"valueField","30-18");
        expectError(requiredPopup(() -> click(d,"Áp dụng")),"khoảng");
    }

    // ===== F04 – LỊCH SỬ CHỨC VỤ (12 test) =====
    private NhanVien roleEmployee() {
        NhanVien n=employee("NV001");
        edt(() -> { n.getLichSuChucVu().clear(); n.setViTri("Chưa có chức vụ"); });
        return n;
    }
    private GUINhanVien history(NhanVien n) {
        GUINhanVien d=read(() -> new GUINhanVien(gui,n,fmt,()->{}));
        dialogs.add(d);
        edt(() -> d.setSelectedTab(2));
        SwingUtilities.invokeLater(() -> d.setVisible(true)); // dialog modal, không block thread test
        return d;
    }
    private static Thread closeWarningPopups() {
        Thread th=new Thread(() -> {
            long until=System.nanoTime()+5_000_000_000L;
            while (System.nanoTime()<until) {
                try {
                    for (Window w:Window.getWindows()) {
                        if (w instanceof JDialog && w.isShowing() &&
                                "Dữ liệu không hợp lệ".equals(((JDialog)w).getTitle())) {
                            SwingUtilities.invokeLater(w::dispose); return;
                        }
                    }
                    Thread.sleep(10);
                } catch(Exception e) { return; }
            }
        },"role-warning-responder");
        th.setDaemon(true);th.start();return th;
    }
    // Hệ điều phối duy nhất cho modal form lịch sử: nhập dữ liệu TRƯỚC khi chọn OK.
    private void roleDialogAction(GUINhanVien d,String button,String name,String start,String end,boolean warnExpected) {
        Thread warnings=warnExpected?closeWarningPopups():null;
        AtomicReference<Throwable> error=new AtomicReference<>();
        AtomicBoolean finished=new AtomicBoolean(false);
        Thread watcher=new Thread(() -> {
            long until=System.nanoTime()+8_000_000_000L;
            while(!finished.get() && System.nanoTime()<until) {
                try {
                    for(Window w:Window.getWindows()) {
                        if(!(w instanceof JDialog) || !w.isShowing() || !((JDialog)w).isModal()) continue;
                        JDialog popup=(JDialog)w;
                        if (!"Thêm chức vụ".equals(popup.getTitle()) && !"Sửa chức vụ".equals(popup.getTitle())) continue;
                        SwingUtilities.invokeAndWait(() -> {
                            try {
                                List<JTextField> ts=find(popup,JTextField.class);
                                if(ts.size()<3) throw new AssertionError("Form chức vụ thiếu 3 ô nhập, thấy "+ts.size());
                                ts.get(0).setText(name);
                                ts.get(1).setText(start==null?"":start);
                                ts.get(2).setText(end==null?"":end);
                                JOptionPane pane=find(popup,JOptionPane.class).get(0);
                                pane.setValue(JOptionPane.OK_OPTION);
                                popup.dispose();
                            } catch(Throwable t) { error.set(t);popup.dispose(); }
                        });
                        return;
                    }
                    Thread.sleep(10);
                } catch(InterruptedException e){return;}
                catch(Exception e){error.set(e);return;}
            }
            if(!finished.get() && error.get()==null) error.set(new AssertionError("Không thấy hộp thoại thêm/sửa chức vụ"));
        },"role-dialog-filler");
        watcher.setDaemon(true);watcher.start();
        try { click(d,button); }
        finally {
            finished.set(true);watcher.interrupt();
            try {watcher.join(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
            if (warnings!=null) warnings.interrupt();
        }
        if(error.get()!=null) throw new AssertionError("Không thể nhập form lịch sử chức vụ",error.get());
    }
    private void oldRole(NhanVien n, String name,String start,String end) {
        edt(() -> { n.getLichSuChucVu().add(new ChucVu(name,date(start),end==null?null:date(end)));
                    n.capNhatViTriTheoChucVuHienTai(); });
    }
    private void selectRole(GUINhanVien d,int row) {
        edt(() -> ((JTable)fld(d,"chucVuTable")).setRowSelectionInterval(row,row));
    }
    private void roleButton(GUINhanVien d,String button) {
        // Hai tab lịch sử có nút Sửa/Xóa: lấy nút cuối, tương ứng panel chức vụ.
        JButton b=read(() -> find(d,JButton.class).stream().filter(x -> button.equals(x.getText()))
                .reduce((a,c)->c).orElseThrow());
        edt(b::doClick);
    }
    @Test void CV_01() {
        NhanVien n=roleEmployee();GUINhanVien d=history(n);
        roleDialogAction(d,"Thêm chức vụ","Nhân viên","01/01/2024",null,false);
        assertEquals(1,n.getLichSuChucVu().size());
        assertEquals("Nhân viên",n.getLichSuChucVu().get(0).getTenChucVu());
        assertEquals("Nhân viên",n.getViTri());
    }
    @Test void CV_02() {
        NhanVien n=roleEmployee();GUINhanVien d=history(n);
        roleDialogAction(d,"Thêm chức vụ","  ","01/01/2024",null,false);
        assertEquals(0,n.getLichSuChucVu().size());
    }
    @Test void CV_03() {
        NhanVien n=roleEmployee();GUINhanVien d=history(n);
        roleDialogAction(d,"Thêm chức vụ","Trưởng nhóm",null,null,true);
        assertEquals(0,n.getLichSuChucVu().size(),"Thiếu ngày nhận phải bị từ chối");
    }
    @Test void CV_04() {
        NhanVien n=roleEmployee();GUINhanVien d=history(n);
        roleDialogAction(d,"Thêm chức vụ","Nhân viên","01/01/2025","01/01/2025",false);
        assertEquals(0,n.getLichSuChucVu().size(),"QT-03: Kết thúc phải SAU ngày nhận, không được bằng");
    }
    @Test void CV_05() {
        NhanVien n=roleEmployee();GUINhanVien d=history(n);
        roleDialogAction(d,"Thêm chức vụ","Nhân viên","02/01/2025","01/01/2025",true);
        assertEquals(0,n.getLichSuChucVu().size());
    }
    @Test void CV_06() {
        NhanVien n=roleEmployee();oldRole(n,"Nhân viên","01/01/2024",null);
        GUINhanVien d=history(n);
        roleDialogAction(d,"Thêm chức vụ","Trưởng nhóm","01/06/2025",null,false);
        assertEquals(2,n.getLichSuChucVu().size());
        assertEquals(date("01/06/2025"),n.getLichSuChucVu().get(0).getNgayKetthuc());
        assertEquals("Trưởng nhóm",n.getLichSuChucVu().get(1).getTenChucVu());
        assertEquals("Trưởng nhóm",n.getViTri());
    }
    @Test void CV_07() {
        NhanVien n=roleEmployee();oldRole(n,"Nhân viên","01/01/2024","01/06/2025");
        GUINhanVien d=history(n);
        roleDialogAction(d,"Thêm chức vụ","Trưởng nhóm","01/05/2025","01/07/2025",false);
        assertEquals(1,n.getLichSuChucVu().size(),"QT-07: Chức vụ chồng lấn phải bị từ chối");
        assertEquals(date("01/06/2025"),n.getLichSuChucVu().get(0).getNgayKetthuc());
    }
    @Test void CV_08() {
        NhanVien n=roleEmployee();oldRole(n,"Nhân viên","01/01/2024",null);
        GUINhanVien d=history(n);
        roleDialogAction(d,"Thêm chức vụ","Trưởng nhóm","01/06/2025",null,false);
        assertEquals(2,n.getLichSuChucVu().size());
        assertEquals(date("01/06/2025"),n.getLichSuChucVu().get(0).getNgayKetthuc());
        assertEquals(date("01/06/2025"),n.getLichSuChucVu().get(1).getNgayNhan());
    }
    @Test void CV_09() {
        NhanVien n=roleEmployee();oldRole(n,"Nhân viên","01/01/2024","01/06/2025");
        oldRole(n,"Trưởng nhóm","01/06/2025",null);
        GUINhanVien d=history(n);
        assertEquals("Trưởng nhóm",n.getViTri());
        JTextField pos=fld(d,"positionField");
        assertFalse(read(pos::isEditable),"Vị trí hiện tại trong hồ sơ phải chỉ đọc");
        assertEquals("Trưởng nhóm",read(pos::getText));
    }
    @Test void CV_10() {
        NhanVien n=roleEmployee();oldRole(n,"Nhân viên","01/01/2024","01/06/2025");
        oldRole(n,"Trưởng nhóm","01/06/2025",null);
        GUINhanVien d=history(n);selectRole(d,1);
        // Sửa nút chức vụ, không nhầm với nút Sửa của tab bằng cấp.
        JButton b=read(() -> find(d,JButton.class).stream().filter(x -> "Sửa".equals(x.getText()))
                .reduce((a,c)->c).orElseThrow());
        roleDialogActionWithButton(b,"Trưởng nhóm","02/06/2025",null);
        assertEquals(date("02/06/2025"),n.getLichSuChucVu().get(1).getNgayNhan());
        assertEquals(2,n.getLichSuChucVu().size());
        assertEquals("Trưởng nhóm",n.getViTri());
    }
    private void roleDialogActionWithButton(JButton b,String name,String start,String end) {
        AtomicReference<Throwable> error=new AtomicReference<>();
        Thread watcher=new Thread(() -> {
            long until=System.nanoTime()+6_000_000_000L;
            while(System.nanoTime()<until) {
                for(Window w:Window.getWindows()) {
                    if(w instanceof JDialog && w.isShowing() && "Sửa chức vụ".equals(((JDialog)w).getTitle())) {
                        JDialog popup=(JDialog)w;
                        try {SwingUtilities.invokeAndWait(() -> {
                            List<JTextField> ts=find(popup,JTextField.class);
                            if(ts.size()<3) throw new AssertionError("Thiếu ô nhập chức vụ");
                            ts.get(0).setText(name);ts.get(1).setText(start);ts.get(2).setText(end==null?"":end);
                            find(popup,JOptionPane.class).get(0).setValue(JOptionPane.OK_OPTION);
                            popup.dispose();
                        });} catch(Throwable t) {error.set(t);}
                        return;
                    }
                }
                try{Thread.sleep(10);}catch(InterruptedException e){return;}
            }
            error.set(new AssertionError("Không tìm thấy form sửa chức vụ"));
        },"role-edit-filler");watcher.setDaemon(true);watcher.start();
        edt(b::doClick);
        try{watcher.join(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        if(error.get()!=null)throw new AssertionError("Sửa chức vụ thất bại",error.get());
    }
    @Test void CV_11() {
        NhanVien n=roleEmployee();oldRole(n,"Nhân viên","01/01/2024","01/06/2025");
        oldRole(n,"Trưởng nhóm","01/06/2025",null);
        GUINhanVien d=history(n);selectRole(d,1);
        roleButton(d,"Xóa");
        assertEquals(1,n.getLichSuChucVu().size());
        assertEquals("Chưa có chức vụ",n.getViTri(),"QT-08: Xóa chức vụ hiệu lực phải cập nhật vị trí, không giữ chức vụ đã xóa");
    }
    @Test void CV_12() {
        NhanVien n=roleEmployee();oldRole(n,"Nhân viên","01/01/2024",null);
        GUINhanVien d=history(n);
        roleDialogAction(d,"Thêm chức vụ","Trưởng nhóm",todayPlusYears(1),null,false);
        assertEquals(2,n.getLichSuChucVu().size());
        assertEquals("Nhân viên",n.getViTri(),"Chức vụ chưa đến ngày nhận không được thành hiện tại");
    }
}
