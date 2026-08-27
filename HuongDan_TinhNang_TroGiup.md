# Hướng Dẫn Code: 10 Tính Năng Trong Menu Trợ Giúp

Tài liệu này bám sát vào 10 tính năng được liệt kê trong màn hình **Trợ giúp > Danh sách tính năng** của chương trình. Dưới đây là giải thích và các đoạn code cơ bản đại diện cho từng tính năng đó.

---

## 1. Quản lý phòng ban (Xem, thêm, sửa, xóa)
Sử dụng `DefaultListModel` kết hợp `JList` để quản lý danh sách. Thêm vào đó, hệ thống tự động đếm và hiển thị "Tổng số nhân viên" của từng phòng ban thông qua hàm lấy `size()` của mảng.
```java
// Tự động đếm nhân viên và hiển thị lên Giao diện List (CellRenderer)
int tongNhanVien = (phongBan.getNhanVien() != null) ? phongBan.getNhanVien().size() : 0;
return "<html><b>" + phongBan.getTenPB() + "</b><br>Trưởng phòng: " 
        + tenTruongPhong + "<br>Tổng nhân viên: " + tongNhanVien + "</html>";

// Thêm phòng ban mới
PhongBan pb = new PhongBan(ten, ma, new ArrayList<>());
phongBanModel.addElement(pb);
```

---

## 2. Quản lý nhân viên (Hiển thị và Thống kê tổng)
Hệ thống tự động duyệt qua tất cả phòng ban để đếm tổng số nhân viên toàn công ty, và hiển thị danh sách nhân viên của phòng ban đang được chọn.
```java
// Hàm vòng lặp quét qua tất cả phòng ban để cộng dồn nhân viên
int total = 0;
for (int i = 0; i < phongBanModel.getSize(); i++) {
    PhongBan pb = phongBanModel.getElementAt(i);
    if (pb.getNhanVien() != null) total += pb.getNhanVien().size();
}
tongSoCongTyLabel.setText("Tổng số nhân viên toàn công ty: " + total);

// Bắt sự kiện click vào Danh sách Phòng ban để tải nhân viên lên Bảng
phongBanList.addListSelectionListener(event -> {
    if (!event.getValueIsAdjusting()) {
        PhongBan phongBan = phongBanList.getSelectedValue();
        if (phongBan != null) {
            for (NhanVien nv : phongBan.getNhanVien()) {
                nhanVienModel.addRow(new Object[]{ nv.getId(), nv.getFullName(), ... });
            }
        }
    }
});
```

---

## 3. Hồ sơ chi tiết: Tự động điền 'Ngày kết thúc' cho chức vụ cũ
Khi thêm chức vụ mới, hệ thống mò lại chức vụ gần nhất và tự động chốt ngày kết thúc.
```java
List<ChucVu> history = nhanVien.getLichSuChucVu();
if (!history.isEmpty()) {
    ChucVu last = history.get(history.size() - 1);
    
    // Nếu ghế cũ chưa bị mất (chưa có ngày kết thúc)
    if (last.getNgayKetthuc() == null) {
        // Lấy ngày nhận chức mới (start) đắp vào ngày mất chức cũ
        last.setNgayKetthuc(start); 
    }
}
```

---

## 4. Tính ngày công & Phân loại tự động
Tính toán tỷ lệ phần trăm (Số ngày đã làm / Số ngày chuẩn) để đưa ra xếp loại.
```java
private void phanLoaiNgayCong() {
    double tyLe = (double) soNgayDaLam / soNgayChuan;
    String phanLoai = "Kém";
    if (tyLe >= 1.0) phanLoai = "Xuất sắc";
    else if (tyLe >= 0.9) phanLoai = "Giỏi";
    else if (tyLe >= 0.75) phanLoai = "Khá";
    else if (tyLe >= 0.5) phanLoai = "Trung bình";
    
    ketQuaLabel.setText(String.format("Tỷ lệ: %.1f%% - Đánh giá: %s", tyLe * 100, phanLoai));
}
```

---

## 5. Tìm kiếm nâng cao (Hỗ trợ khoảng số VD: '20-30') & Thống kê
Nhận diện dấu gạch ngang `-` trong chuỗi tìm kiếm để thực hiện phép toán `Lớn hơn Min` và `Nhỏ hơn Max`. Đồng thời tự động đếm "Số lượng đã lọc" hiển thị ngay trên màn hình.
```java
// Logic đếm số lượng nhân viên thỏa mãn bộ lọc
ketQuaLocLabel.setText("Số lượng đã lọc: " + displayedEmployees.size());

// Logic nhận diện khoảng số
String filterText = "20-30"; // Người dùng nhập
if (filterText.contains("-")) {
    String[] parts = filterText.split("-");
    int min = Integer.parseInt(parts[0].trim()); // 20
    int max = Integer.parseInt(parts[1].trim()); // 30
    
    if (age >= min && age <= max) {
        return true; // Giữ lại nhân viên này trên bảng tìm kiếm
    }
}
```

---

## 6. Sắp xếp thông minh & Cố định bảng
Kích hoạt thuật toán Sort (A-Z, 0-9) cho JTable, đồng thời khóa chết tiêu đề cột để tránh người dùng kéo thả làm vỡ cấu trúc thuật toán.
```java
// Bật tính năng nhấp vào tiêu đề để sắp xếp
TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(nhanVienModel);
nhanVienTable.setRowSorter(sorter);

// BẮT BUỘC KHÓA CỘT: Chống kéo thả cột Mã NV văng ra sau cột Giới Tính
nhanVienTable.getTableHeader().setReorderingAllowed(false);
```

---

## 7. Chỉnh sửa ngay trên bảng (Inline-Editing) & JComboBox
Cho phép click đúp vào ô để nhập liệu, và biến ô nhập liệu thành Menu thả xuống (Trạng thái, Giới tính).
```java
// Giới hạn cột 0 đến cột 6 mới được phép click đúp để sửa
public boolean isCellEditable(int row, int column) {
    return column >= 0 && column <= 6;
}

// Nhúng ComboBox vào cột số 5
JComboBox<String> cbGioiTinh = new JComboBox<>(new String[]{"Nam", "Nữ"});
nhanVienTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(cbGioiTinh));
```

---

## 8. Chỉnh sửa nhanh Vị trí (Nhảy Tab)
Trong bảng GUI, nếu người dùng click đúp vào cột Vị trí (cột số 7), phần mềm chặn không cho gõ chữ mà lập tức mở cửa sổ Hồ sơ, nhảy vọt tới Tab số 3 (Lịch sử chức vụ).
```java
// Lấy chỉ số cột thật sự trong Mảng dữ liệu (tránh lỗi lệch khi Sort)
int modelCol = table.convertColumnIndexToModel(viewCol);

if (modelCol == 7) { 
    // Mở cửa sổ Hồ Sơ Nhân Viên
    GUINhanVien dialog = new GUINhanVien(owner, nhanVien);
    // Nhảy tới Tab thứ 3 (Index = 2)
    dialog.setSelectedTab(2);
    dialog.setVisible(true);
}
```

---

## 9. Thao tác chuột phải đa năng
Bắt sự kiện Click phải, tự động bôi đen dòng được click nếu người dùng click ra ngoài vùng xanh.
```java
private void showContextMenu(MouseEvent event) {
    if (event.isPopupTrigger()) {
        int row = table.rowAtPoint(event.getPoint());
        
        // Nếu chuột phải click vào dòng CHƯA ĐƯỢC BÔI ĐEN
        if (!table.isRowSelected(row)) {
            // Xóa hết bôi đen cũ, chỉ bôi đen dòng hiện tại
            table.setRowSelectionInterval(row, row);
        }
        contextMenu.show(table, event.getX(), event.getY());
    }
}
```

---

## 10. Sửa trạng thái hàng loạt (Bulk-Edit)
Quét toàn bộ các dòng đang bôi đen. Chú ý lệnh `convertRowIndexToModel` là bắt buộc để ép số thứ tự Đồ Họa về số thứ tự Mảng Dữ Liệu gốc (Phòng trường hợp đang bấm Sắp xếp bảng).
```java
int[] selectedRows = nhanVienTable.getSelectedRows(); // VD: bôi đen dòng 2, 4, 6

for (int viewRow : selectedRows) {
    // Ép vị trí ảo trên giao diện về vị trí thực trong Mảng ArrayList
    int modelRow = nhanVienTable.convertRowIndexToModel(viewRow);
    
    // Cập nhật trạng thái
    NhanVien nv = displayedNhanViens.get(modelRow);
    nv.setTrangThai("Nghỉ việc");
}
```

