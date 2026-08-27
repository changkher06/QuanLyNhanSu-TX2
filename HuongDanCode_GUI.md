# Hướng dẫn chuyên sâu toàn diện: GUI.java

Lớp `GUI.java` là trái tim của ứng dụng. Kế thừa từ `JFrame`, nó tạo ra cửa sổ gốc bao phủ toàn màn hình và chứa toàn bộ cơ chế điều hướng chính.

## 1. Khởi tạo Giao diện và UIManager (Constructor)

Ngay khi khởi động, hàm tạo (constructor) của `GUI` thiết lập các thông số cốt lõi.

```java
public GUI() {
    super("Quản lý nhân sự công ty Thành Hưng");
    
    // UIManager cho phép can thiệp vào giao diện mặc định của Java Swing
    // Đổi màu nền của tất cả các Panel và Hộp thoại (JOptionPane)
    UIManager.put("Panel.background", BACKGROUND);
    UIManager.put("OptionPane.background", BACKGROUND);
    UIManager.put("OptionPane.messageFont", APP_FONT);
    
    // Ép SimpleDateFormat phải phân tích ngày tháng cực kỳ khắt khe (VD: Không có ngày 32/13/2020)
    DATE_FORMAT.setLenient(false);
    
    // Đóng cửa sổ là tắt luôn tiến trình chạy ngầm của Java
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    // Xây dựng các nút bấm, danh sách, bảng biểu...
    buildInterface();
    
    // Ép cửa sổ phóng to toàn màn hình (Maximize) ngay khi vừa mở lên
    setExtendedState(JFrame.MAXIMIZED_BOTH);
}
```

## 2. Quản lý Phòng Ban (JList & DefaultListModel)

Dữ liệu phòng ban không tự nhiên mà có trên màn hình. Nó được quản lý qua mô hình **Model-View**. `DefaultListModel` (Model) giữ dữ liệu thật, còn `JList` (View) chỉ là cái vỏ hiển thị.

```java
// Khai báo Model chứa các object PhongBan
private final DefaultListModel<PhongBan> phongBanModel = new DefaultListModel<>();
// Gắn Model vào JList
private final JList<PhongBan> phongBanList = new JList<>(phongBanModel);

// Bắt sự kiện: Mỗi khi người dùng click chọn 1 phòng ban khác nhau trên List
phongBanList.addListSelectionListener(event -> {
    // Tránh việc sự kiện bị gọi 2 lần (lúc nhấn chuột xuống và lúc nhả chuột ra)
    if (!event.getValueIsAdjusting()) {
        loadSelectedPhongBan();
    }
});

private void loadSelectedPhongBan() {
    // Lấy ra Object PhongBan đang được người dùng bôi đen
    PhongBan phongBan = phongBanList.getSelectedValue();
    if (phongBan == null) return;
    
    // Đẩy thông tin của phòng ban đó lên Form nhập liệu bên dưới
    tenPhongBanField.setText(phongBan.getTenPB());
    truongPhongField.setText(phongBan.getIdTruongPhong());
    
    // Đếm tổng số nhân viên của phòng ban và đẩy lên Form
    int tong = (phongBan.getNhanVien() != null) ? phongBan.getNhanVien().size() : 0;
    tongSoNhanVienField.setText(String.valueOf(tong));
    
    // Đồng thời ra lệnh cho Bảng Nhân Viên load danh sách người của phòng ban này
    refreshNhanVienTable();
}
```

## 3. Khối lệnh Bảng Nhân Viên (JTable & Inline-Editing)

Đây là nơi khởi tạo "Khối dữ liệu" cho Bảng Danh sách Nhân Viên. Code sử dụng kĩ thuật *Anonymous Class* (Lớp nặc danh) để ghi đè hàm trực tiếp.

```java
private final DefaultTableModel nhanVienModel = new DefaultTableModel(
        new Object[] { "Mã NV", "Họ tên", "Ngày sinh", "Ngày vào làm", "Số điện thoại", "Giới tính", "Trạng thái", "Vị trí", "Ngày công" }, 0) {
    
    // QUYẾT ĐỊNH CỘT NÀO ĐƯỢC SỬA BẰNG CÁCH CLICK ĐÚP
    @Override
    public boolean isCellEditable(int row, int column) {
        // Chỉ cột từ 0 tới 6 mới cho phép sửa trực tiếp trên bảng.
        // Cột 7 (Vị trí) tự tính từ lịch sử, Cột 8 (Ngày công) tự tính bằng công thức nên khóa lại.
        return column >= 0 && column <= 6;
    }

    // XỬ LÝ LƯU DỮ LIỆU KHI GÕ XONG (ENTER)
    @Override
    public void setValueAt(Object aValue, int row, int column) {
        if (row < 0 || row >= displayedNhanViens.size() || aValue == null) return;
        
        // Móc ra đúng NhanVien ứng với dòng đó
        NhanVien nv = displayedNhanViens.get(row);
        String val = aValue.toString().trim();
        boolean changed = false;
        
        switch (column) {
            case 0: nv.setID(val); changed = true; break;
            case 2: // Ngày sinh
                try {
                    nv.setBirthDate(DATE_FORMAT.parse(val));
                    changed = true;
                } catch (Exception e) {
                    showError("Ngày sinh không hợp lệ.");
                    return; // Văng ra ngoài, từ chối sửa
                }
                break;
            case 5: // Giới tính từ JComboBox (Trường hợp "Nam" thì true, "Nữ" thì false)
                nv.setGioiTinh("Nam".equals(val));
                changed = true;
                break;
        }
        
        // Cập nhật lại giao diện bảng nếu lưu thành công
        if (changed) super.setValueAt(aValue, row, column);
    }
};

// KHÓA KÉO THẢ CỘT: Tránh làm vỡ cấu trúc thuật toán index (0, 1, 2...) ở phía trên
nhanVienTable.getTableHeader().setReorderingAllowed(false);

// NHÚNG JCOMBOBOX (Menu thả xuống) vào bảng
javax.swing.JComboBox<String> cbGioiTinh = new javax.swing.JComboBox<>(new String[]{"Nam", "Nữ"});
nhanVienTable.getColumnModel().getColumn(5).setCellEditor(new javax.swing.DefaultCellEditor(cbGioiTinh));
```

## 4. Layout (Sắp xếp không gian)

Giao diện Form thêm nhân viên sử dụng `GridLayout` để tạo ô cờ, giúp các nhãn và ô nhập xếp thẳng hàng tăm tắp.

```java
// Tạo khung với cấu trúc Lưới (Grid) gồm: 4 hàng, 6 cột. Khoảng cách ngang dọc là 6px và 5px
JPanel fields = new JPanel(new GridLayout(4, 6, 6, 5));

// Việc add vào lưới sẽ đi theo thứ tự: Từ trái sang phải, xong hàng 1 sẽ nhảy xuống hàng 2
fields.add(new JLabel("Mã NV"));
fields.add(new JLabel("Họ tên"));
fields.add(new JLabel("Ngày sinh"));
fields.add(new JLabel("Ngày vào làm"));
fields.add(new JLabel("Số điện thoại"));
fields.add(new JLabel("Quê quán"));
// Hàng 2: Điền các ô nhập liệu tương ứng nằm ngay dưới các Nhãn (Labels)
fields.add(maNhanVienField);
fields.add(hoTenField);
// ...
```

## 5. Thuật toán Sửa Hàng Loạt (Bulk-Editing)

Sức mạnh của việc cho phép bôi đen nhiều dòng (MULTIPLE_INTERVAL_SELECTION).

```java
private void bulkEditStatus() {
    // 1. Quét ra mảng các chỉ số dòng đang bị bôi đen
    int[] selectedRows = nhanVienTable.getSelectedRows();
    if (selectedRows.length == 0) return;
    
    // 2. Hiện Hộp thoại (JOptionPane) xin ý kiến người dùng
    String[] statuses = {"Đang làm", "Nghỉ phép", "Nghỉ việc", "Thử việc"};
    String newStatus = (String) JOptionPane.showInputDialog(this, 
        "Chọn trạng thái mới:", "Sửa trạng thái", JOptionPane.QUESTION_MESSAGE, null, statuses, statuses[0]);
        
    if (newStatus != null) {
        for (int viewRow : selectedRows) {
            // Ép vị trí giao diện (view) về vị trí gốc trong Array (model)
            // CỰC KỲ QUAN TRỌNG VÌ: Bảng có thể đang bị User bấm sắp xếp (Sort) đảo lộn vị trí!
            int modelRow = nhanVienTable.convertRowIndexToModel(viewRow);
            
            // Tìm ra nhân viên và đổi trạng thái
            NhanVien nv = displayedNhanViens.get(modelRow);
            nv.setTrangThai(newStatus);
        }
        refreshNhanVienTable(); // Vẽ lại Bảng
    }
}
```
