# Hướng dẫn chuyên sâu toàn diện: GUINhanVien.java

`GUINhanVien.java` là một khối JDialog cực kỳ phức tạp dùng để hiển thị và chỉnh sửa toàn bộ 3 vùng dữ liệu của nhân viên: **Thông tin cá nhân**, **Bằng cấp/Trình độ**, và **Lịch sử chức vụ**.

## 1. Thiết kế JDialog Modal & Kỹ thuật JTabbedPane

Trái ngược với Cửa sổ Tìm Kiếm, Cửa sổ Hồ Sơ Nhân Viên mang tính chất "Buộc phải xử lý xong mới được làm việc khác" (Modal = true).

```java
public class GUINhanVien extends JDialog {
    // Thuộc tính JTabbedPane được khai báo toàn cục để chia màn hình làm nhiều tab
    private final JTabbedPane tabs = new JTabbedPane();

    public GUINhanVien(JFrame owner, NhanVien nhanVien, SimpleDateFormat dateFormat, Runnable onSaved) {
        // Tham số "true" ở hàm super ép cửa sổ thành chế độ Modal.
        // Khi cửa sổ này mở lên, màn hình chính ở dưới sẽ bị đóng băng.
        super(owner, "Hồ sơ nhân viên: " + nhanVien.getFullName(), true);
        
        // Nhận dữ liệu truyền từ GUI.java sang (Bản copy của NhanVien, format ngày tháng...)
        this.nhanVien = nhanVien;
        this.dateFormat = dateFormat;
        this.onSaved = onSaved; // Hàm sẽ được gọi lại sau khi Lưu thành công!
        
        // ... (Khởi tạo các thành phần đồ họa)
    }

    // Cơ chế điều hướng linh hoạt (Navigation)
    // Các file khác có thể gọi dialog.setSelectedTab(2) để "nhảy" thẳng 
    // vào tab Lịch Sử Chức Vụ ngay khi cửa sổ vừa bật lên.
    public void setSelectedTab(int index) {
        if (index >= 0 && index < tabs.getTabCount()) {
            tabs.setSelectedIndex(index);
        }
    }
}
```

## 2. Giao diện Tab Thông tin cá nhân & Quê quán

Mọi dữ liệu không còn là Text tự do nữa, mà được thiết kế ép theo chuẩn.

```java
private JPanel createBasicInfoPanel() {
    // Tạo lưới 9 hàng, 2 cột (Một cột chứa Tên nhãn, một cột chứa Khung nhập liệu)
    JPanel form = new JPanel(new GridLayout(9, 2, 8, 8));
    
    // addField là một hàm tiện ích tự viết để gộp Label và Field vào Lưới trong 1 dòng lệnh.
    addField(form, "Mã nhân viên:", idField);
    addField(form, "Quê quán:", queQuanField); // Field Quê Quán mới được bổ sung
    
    // Dùng JComboBox thả xuống thay vì JTextField để chống việc người dùng nhập 
    // sai chính tả chữ "Nghỉ việc" thành "nghĩ diệt" gây sụp đổ CSDL.
    addField(form, "Trạng thái:", statusBox);
    
    // Đặc biệt: Khóa chết ô Vị trí. 
    // Vì Vị trí không được phép sửa bằng tay, nó BẮT BUỘC phải ăn theo Lịch Sử Chức Vụ.
    positionField.setEditable(false);
    return form;
}
```

## 3. Hàm Save: Cập nhật dữ liệu từ View vào Object NhanVien

Khi bấm nút Lưu, Java phải thực hiện Validate (kiểm tra an toàn) cực kỳ gắt gao trước khi ghi đè thuộc tính.

```java
private void saveBasicInfo() {
    try {
        // Kiểm tra xem trường Mã Nhân Viên có bị xóa trắng không?
        String idStr = idField.getText().trim();
        if (idStr.isEmpty()) throw new Exception("Mã nhân viên không được để trống!");
        
        // Kiểm tra tính hợp lệ của Ngày Sinh bằng hàm parseRequiredDate (do ta tự viết)
        // Nếu ngày sinh nhập linh tinh "Abcxyz", hàm parseRequiredDate sẽ quăng Exception.
        nhanVien.setBirthDate(parseRequiredDate(birthDateField.getDate(), "Ngày sinh"));
        
        // Set dữ liệu Text bình thường
        nhanVien.setQueQuan(queQuanField.getText().trim());
        
        // Đóng hộp thoại lại
        dispose(); 
        
        // ĐÁNH THỨC GUI CHÍNH: Gọi hàm Runable (Callback) truyền từ GUI.java 
        // để bảo GUI.java hãy refresh lại bảng, vì tôi sửa dữ liệu xong rồi!
        if (onSaved != null) onSaved.run();
        
    } catch (Exception ex) {
        // Nếu quá trình lấy dữ liệu có 1 bước lỗi, lập tức văng Popup cảnh báo đỏ, từ chối Lưu.
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
    }
}
```

## 4. Thuật toán Thông minh: Tự chốt sổ Lịch sử Chức vụ

Thuật toán vĩ đại nhất của class này là tính năng "Tự Động Kết Thúc Cũ, Khởi Đầu Mới". Khi thêm chức vụ Trưởng Phòng, hệ thống tự động chốt ngày kết thúc cho chức vụ Nhân Viên trước đó bằng đúng ngày được lên chức!

```java
private void editChucVu(int row) {
    // Bỏ qua phần tạo giao diện nhập ngày tháng...
    Date start = startField.getDate(); 
    Date end = endField.getDate(); 
    
    // Nếu row < 0 tức là chức năng TẠO MỚI.
    if (row < 0) {
        java.util.List<ChucVu> history = nhanVien.getLichSuChucVu();
        
        if (!history.isEmpty()) {
            // "Chụp" lấy chiếc ghế chức vụ gần đây nhất (Phần tử cuối mảng)
            ChucVu last = history.get(history.size() - 1);
            
            // Trí tuệ nhân tạo: Nếu chiếc ghế cũ đó chưa có "Ngày Mất Chức" (End Date đang NULL)
            if (last.getNgayKetthuc() == null) {
                // Hãy lấy "Ngày Lên Chức" (Start) của chiếc ghế mới, 
                // phang thẳng vào "Ngày Mất Chức" của chiếc ghế cũ!
                last.setNgayKetthuc(start);
                
                // Đồng bộ thay đổi đó lên cái Bảng Đồ Họa 
                // Cột thứ 3 (index=2) là ô Ngày Kết Thúc. Cập nhật nó ngay.
                chucVuModel.setValueAt(formatDate(start), history.size() - 1, 2);
            }
        }
        
        // Sau khi "đảo chính" xong, đường đường chính chính thêm chiếc ghế mới vào danh sách.
        history.add(current);
        chucVuModel.addRow(new Object[] { current.getTenChucVu(), formatDate(start), formatDate(end) });
    }
}
```

## 5. Đệ quy trang trí Đồ Họa Hàng Loạt (styleComponents)

Thay vì phải đi tìm từng nút bấm để gọi lệnh `setBackground`, tác giả viết ra hàm Đệ Quy quét như virus lan truyền qua mọi ngóc ngách của Cửa Sổ.

```java
private void styleComponents(Container container) {
    // Quét từng linh kiện con trực tiếp
    for (Component component : container.getComponents()) {
        
        if (component instanceof JButton button) {
            // Nhuộm màu
            button.setBackground(buttonColor(button.getText()));
            button.setForeground(Color.BLACK);
            
        } else if (component instanceof JTable table) {
            // Trang trí bảng và KHÓA KÉO THẢ CỘT
            table.setRowHeight(28);
            table.getTableHeader().setReorderingAllowed(false);
        }
        
        // PHÉP ĐỆ QUY KINH ĐIỂN:
        // Nếu linh kiện con này bản thân nó lại là một cái Hộp chứa (Container)
        if (component instanceof Container child) {
            // Gọi lại chính hàm styleComponents để nó chui vào cái hộp đó quét tiếp!
            // Quá trình sẽ lặp lại liên tục cho đến khi chạm tới linh kiện sâu nhất trong form.
            styleComponents(child);
        }
    }
}
```
