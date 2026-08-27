# Tổng Quan Ứng Dụng Quản Lý Nhân Sự Công Ty Thành Hưng

Tài liệu này cung cấp cái nhìn toàn cảnh về ứng dụng, bao gồm mục đích ra đời, các tính năng cốt lõi (dựa trên tài liệu Trợ Giúp tích hợp sẵn trong phần mềm), và cách thức sử dụng cơ bản dành cho người mới bắt đầu.

---

## 1. Mục Đích Của Ứng Dụng

Ứng dụng **Quản Lý Nhân Sự** được xây dựng bằng ngôn ngữ Java (giao diện Swing) với mục đích:
- **Số hóa quy trình nhân sự:** Chuyển đổi việc quản lý hồ sơ giấy tờ cồng kềnh sang hệ thống máy tính trực quan, khoa học.
- **Tối ưu hóa thời gian:** Cho phép bộ phận HR tra cứu, tìm kiếm, lọc và chỉnh sửa thông tin của hàng ngàn nhân viên chỉ trong chớp mắt.
- **Tự động hóa thông minh:** Hệ thống được tích hợp các trí tuệ nhân tạo cơ bản giúp tự động tính toán ngày công, tự động chốt sổ lịch sử làm việc, giảm thiểu sai sót do con người nhập liệu.

---

## 2. Các Tính Năng Nổi Bật (Dựa trên Menu Trợ Giúp)

Dưới đây là 10 tính năng ưu việt nhất đã được tích hợp hoàn chỉnh trong phần mềm:

1. **Quản lý phòng ban toàn diện:** Cho phép xem, thêm, sửa, xóa phòng ban. Hệ thống tích hợp bộ đếm tự động để luôn hiển thị chính xác **Tổng số nhân viên của toàn công ty** và tổng nhân sự của từng phòng.
2. **Quản lý nhân viên theo cây phân cấp:** Hiển thị danh sách nhân viên đồng bộ theo từng phòng ban với đầy đủ các thông tin chi tiết trên một Bảng dữ liệu trung tâm.
3. **Hồ sơ chi tiết & Tự động hóa lịch sử:** Quản lý chuyên sâu các thông tin như bằng cấp, lịch sử chức vụ và quê quán. **Đặc biệt:** Vị trí hiện tại của nhân viên được phần mềm tự động chẩn đoán dựa trên lịch sử chức vụ. Khi thêm chức vụ mới, hệ thống tự động điền 'Ngày kết thúc' cho chức vụ cũ.
4. **Tính ngày công & Phân loại tự động:** Tự động tính toán tỷ lệ % hoàn thành ngày công của nhân viên, từ đó phần mềm tự động gán nhãn phân loại xếp hạng năng lực: *Kém, Trung bình, Khá, Giỏi, Xuất sắc*.
5. **Tìm kiếm nâng cao & Thống kê thời gian thực:** Cỗ máy tìm kiếm linh hoạt theo mọi tiêu chí (họ tên, mã NV, trạng thái, vị trí, giới tính, tuổi, thâm niên). **Đặc biệt:** Hỗ trợ cú pháp tìm kiếm theo khoảng (ví dụ gõ: `20-30` để tìm người từ 20 đến 30 tuổi) và hiển thị chính xác số lượng nhân viên thỏa mãn tiêu chí lọc.
6. **Sắp xếp thông minh & Cố định bảng:** Người dùng có thể click vào bất kỳ tiêu đề cột nào để thuật toán tự động sắp xếp (A-Z). Khung xương của bảng được khóa cố định để chống việc kéo thả cột làm hỏng cấu trúc dữ liệu.
7. **Chỉnh sửa siêu tốc ngay trên bảng (Inline-Editing):** Nhấn đúp chuột vào bất kỳ ô nào trên Bảng để sửa trực tiếp mà không cần mở Hồ Sơ. Các cột Trạng Thái / Giới Tính được nhúng sẵn Menu Thả Xuống (Dropdown) để chọn rất tiện lợi.
8. **Lối tắt chỉnh sửa Vị trí:** Bảng dữ liệu có tính năng nhảy tắt (Shortcut). Khi click đúp vào cột Vị trí, phần mềm tự động hiểu ý người dùng, mở ngay cửa sổ Hồ Sơ và nhảy vọt sang tab 'Lịch sử chức vụ'.
9. **Thao tác chuột phải đa năng (Context Menu):** Cung cấp Menu chuột phải để xử lý nhanh: Mở hồ sơ, xóa nhân viên, hoặc chỉnh sửa trực tiếp. Thuật toán chuột phải đủ thông minh để không làm mất vùng dữ liệu đang bôi đen.
10. **Sửa trạng thái hàng loạt (Bulk-Editing):** Tính năng bôi đen hàng chục nhân viên cùng lúc, sau đó click chuột phải chọn 'Sửa trạng thái hàng loạt' để thay đổi dữ liệu của tất cả mọi người chỉ bằng 1 cú click. Thuật toán có khả năng chống lỗi tráo đổi dòng khi bảng đang bị sắp xếp.

---

## 3. Hướng Dẫn Sử Dụng Cơ Bản

Dành cho người mới tiếp cận ứng dụng lần đầu:

### Bước 1: Khởi tạo dữ liệu mẫu (Dành cho thử nghiệm)
Nếu phần mềm chưa có dữ liệu, hãy **Click chuột phải** vào khoảng trống màu trắng ở khu vực danh sách Phòng Ban (cột bên trái màn hình) -> Chọn **"Tạo dữ liệu mẫu"**. Hệ thống sẽ tự động sinh ra hàng chục nhân viên với tên, quê quán, ngày công ngẫu nhiên để bạn trải nghiệm.

### Bước 2: Thao tác với Phòng Ban
- **Thêm:** Ở khung "Thông tin phòng ban", điền Tên phòng ban và Mã trưởng phòng, sau đó ấn **Lưu**.
- **Sửa:** Click chọn 1 phòng ban bên trái, sửa nội dung trong khung thông tin rồi ấn **Lưu**.
- **Xóa:** Click chọn 1 phòng ban rồi ấn nút **Xóa**.

### Bước 3: Thao tác với Nhân Viên
- **Thêm mới:** Chọn 1 phòng ban. Điền các thông tin vào khung "Thông tin nhân viên" ở phía dưới (Mã NV, Họ tên, Quê quán, Ngày sinh...). Sau đó ấn **Lưu**. Nhân viên sẽ được chèn vào Bảng.
- **Mở hồ sơ:** Bấm chuột phải vào 1 dòng nhân viên trên Bảng, chọn **Mở hồ sơ**. Tại đây bạn có thể thêm Quê quán ở tab *Thông tin cá nhân*, hay thêm bằng cấp ở tab *Trình độ học vấn*.
- **Cập nhật trạng thái nghỉ việc hàng loạt:** Giữ phím `Ctrl` hoặc kéo chuột để bôi đen nhiều nhân viên. Bấm chuột phải vào vùng bôi đen màu xanh, chọn **Chỉnh sửa trạng thái hàng loạt** -> Chọn *Nghỉ việc*.

### Bước 4: Chức năng Tìm Kiếm
- Ở thanh Menu trên cùng cùng của phần mềm, bấm **Tìm kiếm nhân viên** -> **Hiển thị tất cả**.
- Ở cửa sổ bật lên, bạn chọn "Tìm theo: Tuổi".
- Nhập vào thanh tìm kiếm chữ `25-35`. Bảng sẽ lập tức lọc ra những người thuộc độ tuổi này, và góc dưới bên phải màn hình sẽ báo cáo "Số lượng đã lọc: ...".

