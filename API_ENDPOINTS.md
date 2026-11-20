# Hợp đồng HTTP/JSON — QuanLyNhanSu TX2

Base URL mặc định: `http://127.0.0.1:8080`. Request có body dùng
`Content-Type: application/json`. Response thành công/lỗi là JSON, ngoại trừ
các lỗi kết nối hoặc request bị servlet container từ chối trước DispatcherServlet.
UUID là định danh kỹ thuật; `maNv` là thuộc tính nghiệp vụ có thể trùng.

## Endpoint

| Method | Path | Thành công | Ý nghĩa |
|---|---|---|---|
| POST | `/api/nhan-vien` | 201 + Employee, Location | Thêm qua NhanVienService |
| GET | `/api/nhan-vien?maNv=...` | 200 + Employee[] | Lọc theo mã TX1 trên từng phòng ban |
| GET | `/api/nhan-vien/{id}` | 200 + Employee | Tra đúng UUID |
| PUT | `/api/nhan-vien/{id}/ngay-cong` | 200 + Employee | Cập nhật qua NgayCongService |
| GET | `/api/phong-ban` | 200 + Department[] | Danh sách theo thứ tự Repository |
| POST | `/api/test-data/reset` | 200 + ResetResult | Chỉ tồn tại khi `tx2.test-data.enabled=true` |

## Thêm nhân viên

Lấy UUID phòng ban trước rồi gửi:

```json
{
  "phongBanId": "<UUID từ GET /api/phong-ban>",
  "maNv": "NV001",
  "hoTen": "Nhân viên TX2",
  "ngaySinh": "2010-01-01",
  "ngayVaoLam": "2020-01-01",
  "soDT": "0901234567",
  "gioiTinh": true,
  "queQuan": "Hà Nội",
  "trangThai": "Đang làm",
  "viTri": "Nhân viên"
}
```

`phongBanId` phải có UUID hợp lệ và tồn tại. `maNv`, `hoTen`, ngày sinh, ngày vào
làm được Service kiểm tra như TX1: trim mã/tên, không để trống, ngày vào làm không
trước ngày sinh. Ví dụ trên **được chấp nhận**, cả khi mã đã tồn tại và chưa đủ 18
tuổi. Không trả 409 cho mã trùng. Ngày sinh bằng ngày vào làm cũng được chấp nhận.

Các trường text còn lại và `gioiTinh` có thể bỏ qua/null; không thêm luật điện
thoại, giới tính hoặc tuổi. `trangThai` null/bỏ qua mặc định `Đang làm` giống thao
tác thêm của GUI. Ngày JSON phải là chuỗi ISO có ngày thực, không có giờ;
map sang `java.util.Date` tại 00:00 Asia/Bangkok. Thiếu ngày đúng kiểu được chuyển
cho Service để trả 422; ngày sai định dạng là 400. Trường không biết hoặc sai kiểu
(ví dụ `hoTen:123`, `gioiTinh:"true"`) là 400, không ép kiểu ngầm.

Response Employee:

```json
{
  "id": "<UUID nhân viên mới>",
  "phongBanId": "<UUID phòng ban>",
  "maNv": "NV001",
  "hoTen": "Nhân viên TX2",
  "ngaySinh": "2010-01-01",
  "ngayVaoLam": "2020-01-01",
  "soDT": "0901234567",
  "gioiTinh": true,
  "queQuan": "Hà Nội",
  "trangThai": "Đang làm",
  "viTri": "Nhân viên",
  "ngayCong": {
    "soNgayDaLam": 0.0,
    "soNgayChuan": 26.0,
    "phanTramHoanThanh": 0.0,
    "phanLoai": "Kém"
  }
}
```

Header `Location: /api/nhan-vien/<UUID mới>`. Các chuỗi `<...>` trong tài liệu là
placeholder, phải thay bằng UUID thật. Không serialize model trực tiếp: response
không chứa `NhanVien.ngayCong.nhanVien`, getter BigDecimal hoặc lịch sử chức vụ.

## Tìm kiếm

`maNv` bắt buộc; encode URL nếu có khoảng trắng/ký tự đặc biệt. Dùng
`TimKiemNhanVienService.timKiem(pb, "Mã nhân viên", maNv)` cho từng phòng ban rồi
ghép danh sách theo thứ tự phòng ban/nhân viên. **Không phải** phép equality duy
nhất theo mã, và không dùng tiện ích `timTheoMa` chỉ trả bản ghi đầu tiên.

- `maNv=NV001`: văn bản chứa, không phân biệt hoa/thường, trả tất cả mã trùng.
- `maNv=1`: nhánh số TX1, phần chữ số của mã NV001 là 1.
- `maNv=1-3`: nhánh khoảng số hợp lệ.
- Khoảng đảo, số âm hoặc overflow đi theo nhánh fallback sẵn có của Service;
  không sửa lỗi hoặc bổ sung validation tìm kiếm.
- Không khớp: 200 và `[]`; thiếu tham số: 400; giá trị trống: 422 từ Service.

Phạm vi GUI gốc là từng phòng ban; endpoint mở rộng toàn công ty bằng cách gọi
Service lần lượt, giữ nguyên ngữ nghĩa và fallback trong mỗi phòng ban.

## Cập nhật ngày công và số đặc biệt

```json
{"soNgayDaLam":23.4,"soNgayChuan":26}
```

Cả hai trường bắt buộc, nhận JSON number hoặc string. String được chuyển nguyên
sang adapter `Double.parseDouble` của Service: hỗ trợ cú pháp số TX1, kể cả số mũ,
hex float và số đặc biệt. Boolean/object/array/null/thiếu trường là lỗi giao tiếp
400; chuỗi không parse được là lỗi 422 từ Service. Không giới hạn số lẻ.

Service giữ nguyên ba điều kiện `done < 0 || standard <= 0 || done > standard`.
Không thêm `isFinite`: `"NaN"/26`, `13/"Infinity"`,
`"Infinity"/"Infinity"` được chấp nhận như TX1; `"Infinity"/26` bị từ chối.
HTTP response vẫn là 200 khi Service chấp nhận, không tạo lỗi giả cho test.

Số **hữu hạn** trả JSON number. Số **không hữu hạn** trả chính xác một trong các
chuỗi `"NaN"`, `"Infinity"`, `"-Infinity"` ở từng trường số. Quy tắc áp dụng cả
`soNgayDaLam`, `soNgayChuan`, `phanTramHoanThanh`. Client cần xử lý kiểu
`number | special-number-string`; không đổi số đặc biệt thành 0 hoặc null.

```json
{"soNgayDaLam":"Infinity","soNgayChuan":"Infinity"}
```

Phần `ngayCong` của response trên:

```json
{
  "soNgayDaLam":"Infinity",
  "soNgayChuan":"Infinity",
  "phanTramHoanThanh":"NaN",
  "phanLoai":"Kém"
}
```

JSON chuẩn không có literal `NaN`/`Infinity` không đặt dấu nháy: dạng đó bị từ
chối 400. Chỉ lớp ánh xạ quyết định cách biểu diễn; phép tính và phân loại vẫn gọi
model TX1. `23.4/26` giữ tỷ lệ double `89.99999999999999` và phân loại `Khá`;
response không làm tròn. Employee fixture NV002 có `ngayCong:null` vì chưa tính.

## Phòng ban và reset

Department có dạng:

```json
{"id":"<UUID>","tenPhongBan":"Kỹ thuật","maTruongPhong":"","soNhanVien":3}
```

Reset không cần body, trả `{"soPhongBan":2,"soNhanVien":4}`. Toàn bộ UUID cũ hết
hiệu lực sau reset/restart. Endpoint mặc định tắt; khi tắt trả 404. Không chạy các
test ghi/reset song song trên cùng một server. Repository là singleton trong
process; không có dữ liệu nhân viên riêng trong Controller.

## Lỗi

| Status | Trường hợp |
|---|---|
| 400 | JSON hỏng, raw NaN, trường không biết, kiểu sai, UUID/ngày sai, thiếu maNv/phongBanId, cấu trúc ngày công thiếu/null |
| 404 | UUID đúng định dạng nhưng không tồn tại; endpoint không tồn tại/reset bị tắt |
| 405 | Method chưa hỗ trợ; giữ header Allow do framework tạo |
| 406 | Accept không tương thích JSON |
| 415 | Content-Type không hỗ trợ |
| 422 | Service từ chối mã/tên/ngày trống, thứ tự ngày, ngày công hoặc tìm kiếm |
| 500 | Lỗi ngoài dự kiến; chi tiết chỉ ghi log phía server |

```json
{
  "status":422,
  "code":"BUSINESS_RULE",
  "message":"Số ngày công không hợp lệ.",
  "path":"/api/nhan-vien/<UUID>/ngay-cong"
}
```

Code có thể là `BUSINESS_RULE`, `INVALID_REQUEST`, `NOT_FOUND`, `HTTP_400` (hoặc
status HTTP tương ứng), `INTERNAL_ERROR`. Message nghiệp vụ lấy từ Service;
lỗi parser/framework dùng hướng dẫn tiếng Việt, không trả stack trace/class Java.
Nếu request đồng thời sai nhiều phần, ưu tiên lỗi có thể phụ thuộc thứ tự parse,
lookup và Service; chỉ cam kết hành vi trên cho request đơn lỗi.
