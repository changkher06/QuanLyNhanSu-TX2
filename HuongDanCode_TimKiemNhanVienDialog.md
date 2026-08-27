# Hướng dẫn chuyên sâu toàn diện: TimKiemNhanVienDialog.java

`TimKiemNhanVienDialog.java` là một cửa sổ bật lên (JDialog) hỗ trợ hệ thống tìm kiếm siêu nhanh. Nó không trực tiếp làm thay đổi danh sách phòng ban, mà nó mượn dữ liệu của phòng ban để hiển thị một bản nháp đã được bộ lọc sàng lọc.

## 1. Khai báo JDialog (Modal vs Non-Modal)

Cửa sổ này được tạo ra không phải là dạng khóa màn hình (Modal = false).

```java
// Kế thừa JDialog
public class TimKiemNhanVienDialog extends JDialog {
    
    public TimKiemNhanVienDialog(JFrame owner, PhongBan phongBan) {
        // false ở đây có nghĩa là Non-Modal: Bạn vừa có thể bấm cửa sổ tìm kiếm, 
        // vừa có thể click lại chuột vào cửa sổ màn hình chính (GUI.java) mà không bị kẹt.
        super(owner, "Tìm kiếm nhân viên - " + phongBan.getTenPB(), false);
        
        // Tạo các khối giao diện
        buildInterface();
    }
}
```

## 2. DocumentListener - Tìm kiếm Real-time

Khác với việc phải gõ chữ rồi bấm nút "Tìm", công cụ này áp dụng kĩ thuật Real-time. Cứ gõ một ký tự là nó lọc ngay lập tức.

```java
// Gắn một Máy nghe (Listener) vào hộp nhập liệu tìm kiếm
filterInput.getDocument().addDocumentListener(new DocumentListener() {
    // 3 hàm này bao quát mọi trường hợp: Gõ thêm chữ, Xóa chữ, Dán (Paste) chữ
    @Override
    public void insertUpdate(DocumentEvent e) { updateFilterText(); }
    @Override
    public void removeUpdate(DocumentEvent e) { updateFilterText(); }
    @Override
    public void changedUpdate(DocumentEvent e) { updateFilterText(); }
});

private void updateFilterText() {
    // Lấy nội dung mới nhất
    filterText = filterInput.getText();
    // Gọi hàm Load lại Bảng. Hàm refreshTable sẽ xài Filter để gạn lọc dữ liệu.
    refreshTable(this::matchesCurrentFilter);
}
```

## 3. Thuật toán Bộ Lọc Khoảng Số (Range Filtering)

Hàm `matchesCurrentFilter(NhanVien nv)` sẽ trả về `true` (Giữ lại) hoặc `false` (Bỏ đi). Chiêu thức tinh túy nằm ở đoạn Regex nhận diện dấu trừ (`-`) để tìm kiếm theo độ tuổi.

```java
private boolean matchesCurrentFilter(NhanVien nv) {
    if (criterion == null || filterText == null || filterText.isEmpty()) return true;
    
    // Đưa chữ về In Thường toàn bộ để khi so sánh chữ "A" và "a" sẽ coi là như nhau.
    String lowerFilter = filterText.toLowerCase();

    switch (criterion) {
        case "Tuổi": {
            // Lấy chuỗi tuổi hiện tại của nhân viên (Ví dụ: "28")
            String t = calculateAge(nv); 
            if (t.isEmpty()) return false;
            int age = Integer.parseInt(t);

            // BẮT ĐẦU NHẬN DIỆN KHOẢNG SỐ
            // Nếu chữ nhập vào có chứa dấu gạch ngang (Ví dụ: "20-30")
            if (lowerFilter.contains("-")) {
                // Lệnh split chặt chuỗi làm 2 phần: parts[0] = "20" và parts[1] = "30"
                String[] parts = lowerFilter.split("-");
                if (parts.length == 2) {
                    try {
                        int min = Integer.parseInt(parts[0].trim()); // 20
                        int max = Integer.parseInt(parts[1].trim()); // 30
                        
                        // Nếu tuổi nhân viên là 28 -> return true -> Giữ lại!
                        return age >= min && age <= max;
                    } catch (NumberFormatException e) {
                        return false; // Gõ sai cú pháp (VD: "20-AA") thì loại luôn.
                    }
                }
            }
            // Nếu chỉ nhập 1 con số (VD: "28"), nó sẽ dùng lệnh contains để tìm.
            return t.contains(lowerFilter);
        }
        case "Họ tên":
            return nv.getFullName().toLowerCase().contains(lowerFilter);
    }
    return true;
}
```

## 4. Quản lý Sắp xếp (TableRowSorter) và Chỉ số Ảo

Mọi bảng trong Swing đều có "Lớp hiển thị ảo" (View). Khi bạn click vào tiêu đề, Java sẽ tráo thứ tự của Lớp hiển thị ảo đi, nhưng Mảng dữ liệu ở dưới (Model) thì không hề nhúc nhích.

```java
// Kích hoạt Sort
TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
table.setRowSorter(sorter);
```
**Hậu quả:** Anh nhân viên "Nguyễn Văn A" ở vị trí thứ 0 trong Mảng, nhưng bị Sort văng xuống dòng thứ 5 trên màn hình. Nếu bạn lấy chuột Click đúp vào dòng số 5, hệ thống sẽ lấy `get(5)` và sửa nhầm thành "Trần Văn B"!

**Cách khắc phục:**
```java
private void quickEditCell(int viewRow, int viewCol) {
    // Ép vị trí hiển thị dòng 5 (viewRow = 5) 
    // lùi về đúng bản chất vị trí trong mảng dữ liệu gốc (modelRow = 0)
    int modelRow = table.convertRowIndexToModel(viewRow);
    int modelCol = table.convertColumnIndexToModel(viewCol);
    
    // An tâm xài biến modelRow!
    NhanVien nv = displayedEmployees.get(modelRow);
}
```

## 5. Bắt sự kiện Chuột phải Thông minh (Smart Context Menu)

Làm sao để click chuột phải nhưng vẫn bảo toàn được tập hợp các dòng đang bôi đen từ trước?

```java
private void showContextMenu(java.awt.event.MouseEvent event) {
    if (!event.isPopupTrigger()) return; // Chỉ chạy khi là Click Phải
    
    // Tọa độ mũi tên chuột đang chiếu vào dòng nào?
    int row = table.rowAtPoint(event.getPoint());
    
    // KIỂM TRA THÔNG MINH:
    // Nếu chuột phải đang click TRÚNG vào 1 dòng đã bị bôi đen màu xanh rồi...
    // Thì LỆNH IF DƯỚI ĐÂY BỊ BỎ QUA. (Tức là vẫn giữ nguyên khối 10 dòng đang bôi đen)
    // Nếu chuột phải click TRƯỢT ra ngoài (vào 1 dòng màu trắng)...
    if (!table.isRowSelected(row)) {
        // ...Thì nó xóa bôi đen của khối cũ, và chỉ bôi đen dòng màu trắng đó thôi!
        table.setRowSelectionInterval(row, row);
    }

    lastClickedRow = row; // Ghi nhớ dòng bị click để tý nữa Mở Hồ Sơ / Sửa
    contextMenu.show(table, event.getX(), event.getY());
}
```
