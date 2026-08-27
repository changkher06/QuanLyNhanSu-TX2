# Tổng Hợp Tính Năng & Mã Nguồn Tiêu Biểu

Tài liệu này tổng hợp danh sách các **Tính năng nổi bật nhất** của chương trình Quản Lý Nhân Sự, đi kèm với **Đoạn mã nguồn (Code) đại diện** làm nên linh hồn của tính năng đó. Đây là tài liệu tham khảo nhanh rất tốt để bạn ôn lại thuật toán.

---

## 1. Tìm Kiếm & Lọc Dữ Liệu Thời Gian Thực (Real-time Filter)
**Mô tả:** Không cần bấm nút "Tìm", hệ thống sẽ tự động gạn lọc bảng ngay trong khoảnh khắc bạn gõ chữ từ bàn phím. Khả năng lọc cả theo một khoảng số (Ví dụ độ tuổi: "20-30").

**Mã nguồn tiêu biểu (DocumentListener & Khoảng Số):**
```java
// Bắt sự kiện gõ phím trực tiếp vào Textbox
filterInput.getDocument().addDocumentListener(new DocumentListener() {
    public void insertUpdate(DocumentEvent e) { updateFilterText(); }
    public void removeUpdate(DocumentEvent e) { updateFilterText(); }
    public void changedUpdate(DocumentEvent e) { updateFilterText(); }
});

// Thuật toán băm chuỗi để nhận diện khoảng số: "20-30"
if (lowerFilter.contains("-")) {
    String[] parts = lowerFilter.split("-"); // Cắt đôi thành "20" và "30"
    if (parts.length == 2) {
        int min = Integer.parseInt(parts[0].trim());
        int max = Integer.parseInt(parts[1].trim());
        return age >= min && age <= max; // Trả về hợp lệ nếu tuổi nằm giữa 20 và 30
    }
}
```

---

## 2. Sửa Chữ Trực Tiếp Trên Bảng (Inline-Editing)
**Mô tả:** Cho phép click đúp vào các ô trên bảng (Mã NV, Họ tên...) để gõ và sửa chữ trực tiếp giống y hệt như dùng Excel, thậm chí có cả Menu Thả Xuống (Dropdown) trong lòng bảng.

**Mã nguồn tiêu biểu (Ghi đè DefaultTableModel):**
```java
private final DefaultTableModel nhanVienModel = new DefaultTableModel(...) {
    // Chỉ định cột từ 0 đến 6 mới được phép click đúp để sửa
    @Override
    public boolean isCellEditable(int row, int column) {
        return column >= 0 && column <= 6; 
    }

    // Khi gõ xong và bấm Enter, hàm này tự động kích hoạt
    @Override
    public void setValueAt(Object aValue, int row, int column) {
        NhanVien nv = displayedNhanViens.get(row);
        String val = aValue.toString().trim();
        
        switch (column) {
            case 0: nv.setID(val); break;       // Đổi ID
            case 1: nv.setFullName(val); break; // Đổi Tên
        }
        super.setValueAt(aValue, row, column); // Đẩy chữ mới lên bảng
    }
};

// Nhúng cái Menu thả xuống "Nam / Nữ" vào cái bảng
JComboBox<String> cbGioiTinh = new JComboBox<>(new String[]{"Nam", "Nữ"});
nhanVienTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(cbGioiTinh));
```

---

## 3. Chỉnh Sửa Hàng Loạt (Bulk-Editing)
**Mô tả:** Bôi đen cùng lúc 10 nhân viên, sau đó click chuột phải chọn "Sửa trạng thái" để cho cả 10 người cùng "Nghỉ việc" chỉ với 1 cú click. Thuật toán khắc phục được cả lỗi khi người dùng bấm sắp xếp (Sort) bảng.

**Mã nguồn tiêu biểu (Vòng lặp và Quy đổi Index):**
```java
// Bốc ra danh sách STT các dòng đang bị bôi đen màu xanh
int[] selectedRows = nhanVienTable.getSelectedRows();

for (int viewRow : selectedRows) {
    // Thuật toán sinh tử: Quy đổi số thứ tự Đồ họa về đúng số thứ tự của Dữ liệu 
    // Tránh bị sửa nhầm người do Bảng bị tráo đổi vị trí lúc Sort (A-Z)
    int modelRow = nhanVienTable.convertRowIndexToModel(viewRow);
    
    // Rút hồ sơ của người đó ra và đổi hàng loạt trạng thái
    NhanVien nv = displayedNhanViens.get(modelRow);
    nv.setTrangThai(newStatus);
}
refreshNhanVienTable(); // Cập nhật lại giao diện
```

---

## 4. Chuột Phải Thông Minh (Smart Context Menu)
**Mô tả:** Bắt sự kiện Click chuột phải. Nếu bạn click ra ngoài khoảng đang bôi đen thì hệ thống tự động bôi đen dòng mới đó. Nếu click Trúng vào phần đang bôi đen thì nó Vẫn Giữ Nguyên khối lượng đã bôi đen để phục vụ Bulk-Edit.

**Mã nguồn tiêu biểu (MouseAdapter):**
```java
private void showContextMenu(java.awt.event.MouseEvent event) {
    if (!event.isPopupTrigger()) return; // Chỉ cho phép Click Phải
    
    // Tìm ra mũi tên chuột đang chĩa vào dòng thứ mấy
    int row = table.rowAtPoint(event.getPoint());
    
    // Cực kỳ tinh tế: Nếu mũi tên chuột không chĩa vào vùng đã bôi đen
    if (!table.isRowSelected(row)) {
        // Thì hủy bôi đen cũ, bôi đen cái dòng mới này thôi
        table.setRowSelectionInterval(row, row); 
    }
    
    lastClickedRow = row;
    contextMenu.show(table, event.getX(), event.getY()); // Bật Menu lên
}
```

---

## 5. Tự Động Kết Thúc Chức Vụ Cũ (Auto-Closing Timeline)
**Mô tả:** Khi bạn phong chức "Trưởng phòng" mới cho một người. Phần mềm đủ thông minh để tự động mò lại chức "Nhân viên" cũ và chốt cái "Ngày kết thúc" bằng chính cái ngày mà họ lên chức.

**Mã nguồn tiêu biểu (Truy xuất Lịch Sử):**
```java
// row < 0 nghĩa là người dùng đang tạo 1 chức vụ mới toanh
if (row < 0) {
    java.util.List<ChucVu> history = nhanVien.getLichSuChucVu();
    
    // Nếu họ đã từng có chức vụ trong quá khứ
    if (!history.isEmpty()) {
        // Móc ra cái chức vụ gần đây nhất của họ
        ChucVu last = history.get(history.size() - 1);
        
        // Nếu cái ghế cũ đó chưa có Ngày Kết Thúc
        if (last.getNgayKetthuc() == null) {
            // Lấy Ngày Nhận (start) của ghế mới, nhét vào Ngày Kết Thúc của ghế cũ!
            last.setNgayKetthuc(start);
            chucVuModel.setValueAt(formatDate(start), history.size() - 1, 2);
        }
    }
    history.add(current); // Chính thức thêm chức mới vào danh sách
}
```

---

## 6. Đệ Quy Nhuộm Màu (Recursive Styling)
**Mô tả:** Thay vì gọi lệnh setBackground cho từng cái nút (gây phình to Code), lập trình viên viết 1 hàm đệ quy tự động quét qua như Virus: Chui vào mọi ngóc ngách của Cửa Sổ và tự đổi màu đồng bộ.

**Mã nguồn tiêu biểu (Đệ quy UI):**
```java
private void styleComponents(Container container) {
    for (Component component : container.getComponents()) {
        if (component instanceof JButton button) {
            // Biến mọi nút bấm thành màu xanh ngọc
            button.setBackground(new Color(0, 150, 136)); 
        }
        
        // ĐỆ QUY: Nếu bản thân linh kiện này cũng là một cái hộp chứa (Container)
        if (component instanceof Container child) {
            // Tự gọi lại chính mình để chui tọt vào bên trong quét tiếp!
            styleComponents(child); 
        }
    }
}
```

