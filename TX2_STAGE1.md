# TX2 giai đoạn 1: repository và service độc lập Swing

> Tài liệu lịch sử của lần triển khai đầu tiên, khi còn yêu cầu sửa validation.
> Mục tiêu đã đổi sang bảo toàn hành vi TX1. Trạng thái hiện tại và kết quả mới nằm
> trong [TX2_TX1_BEHAVIOR_PRESERVATION.md](TX2_TX1_BEHAVIOR_PRESERVATION.md).
> Các số liệu 62 unit PASS / 55 GUI PASS bên dưới là số liệu của phiên bản trước,
> không phải kết quả của phiên bản bảo toàn TX1.

Thực hiện trên nhánh `feature/tx2-rest-api`, từ commit TX1 `3312331`.
Chưa thêm Spring Boot, REST controller hoặc database. Không commit/push.
Các test TX1 trong `gui-test/GUIBlackBox58Test.java` và `test/unit/JUnit6Test.java`
được giữ nguyên nội dung.

## Cấu trúc và file thay đổi

```text
src/
  model/NgayCong.java                         sửa
  repository/NhanSuRepository.java            mới
  repository/InMemoryNhanSuRepository.java    mới
  service/NhanVienService.java                mới
  service/TimKiemNhanVienService.java          mới
  service/NgayCongService.java                 mới
  view/GUI.java                               sửa
  view/GUINhanVien.java                        sửa
  view/TimKiemNhanVienDialog.java              sửa
test/
  model/NgayCongTest.java                      mới
  repository/InMemoryNhanSuRepositoryTest.java mới
  service/NhanVienServiceTest.java             mới
  service/TimKiemNhanVienServiceTest.java       mới
  service/NgayCongServiceTest.java             mới
  unit/JUnit6Test.java                         giữ nguyên
gui-test/
  GUIBlackBox58Test.java                       giữ nguyên
  ServiceSwingIntegrationTest.java            mới
pom.xml                                       sửa
TX2_STAGE1.md                                 mới
```

Các model và thành phần Swing khác được giữ nguyên. Không chạy Ant hoặc đóng gói
vào `build/` và `dist/`; Maven tạo sản phẩm trong `target/`.
Workspace ban đầu đã có các thay đổi chưa commit ở sản phẩm build, cấu hình
NetBeans riêng và `KetQuaKiemThuHopDen.txt`; không dùng reset, checkout hoặc clean
để loại bỏ các thay đổi đó. Trạng thái ban đầu được lưu tại
`target/tx2-stage1-20261119/initial-git-status.txt`.

## Luồng dữ liệu

```text
Swing -> service -> NhanSuRepository -> cây PhongBan/NhanVien hiện có
```

`InMemoryNhanSuRepository` giữ danh sách phòng ban và quản lý nhân viên ngay trong
`PhongBan.getNhanVien()`. Không tạo model nhân viên hoặc danh sách dữ liệu nghiệp vụ
độc lập thứ hai. Các danh sách trả về là snapshot không sửa được, nhưng phần tử vẫn
là chính model đang được GUI sử dụng. Swing list/table model chỉ phục vụ trình bày.

`new GUI()` vẫn hoạt động như TX1. `GUI.create(repository)` cho phép truyền cùng
instance repository cho GUI và controller ở giai đoạn sau. Factory được dùng để giữ
khả năng biên dịch lời gọi `GuiActionRunner.execute(GUI::new)` trong test TX1.
Dialog tìm kiếm và hồ sơ lấy service từ owner GUI, hoặc nhận service qua constructor.
Constructor tương thích dành cho dialog độc lập sử dụng chính model được truyền
vào; muốn kiểm tra mã trên toàn công ty phải truyền service của kho công ty.

Các thao tác thêm/xóa phòng, thêm/xóa nhân viên, nạp mẫu, sửa mã, sửa ngày sinh/ngày
vào làm, lưu thông tin cá nhân, tìm kiếm, cập nhật/reset ngày công được nối vào
repository/service. Mẫu dữ liệu cố định TX1 vẫn được dựng khi bấm nút nạp mẫu rồi
thay thế nguyên tử vào repository. Nhân viên NV002 vẫn có ngày công `null` để giữ
trạng thái "Chưa tính" của fixture TX1.

## Quy tắc nghiệp vụ

- Mã duy nhất toàn công ty, so sánh sau khi trim và không phân biệt hoa/thường.
  Kiểm tra cả khi thêm, đổi mã, lưu hồ sơ và nạp danh sách phòng có sẵn nhân viên.
  Đổi mã trưởng phòng cũng cập nhật tham chiếu `idTruongPhong`.
- Kiểm tra tuổi đủ 18 bằng ngày sinh cộng 18 năm, đối chiếu **ngày vào làm**.
  Không dùng tuổi tại ngày chạy máy. Ngày được chuyển sang `LocalDate` với `ZoneId`;
  có thể truyền múi giờ vào service. Sinh ngày 29/02 dùng mốc `LocalDate.plusYears`
  (28/02 trong năm không nhuận).
- Ngày công được đọc trực tiếp từ chuỗi sang `BigDecimal`, tối đa hai chữ số thập
  phân có nghĩa sau dấu phẩy theo các test TX1 `NC_12`, `NC_16`. Các số 0 tận cùng
  không làm dữ liệu bị từ chối, ví dụ `19.5000` tương đương `19.5`.
- Từ chối chuỗi rỗng, số không hợp lệ, NaN/Infinity, số âm, ngày chuẩn <= 0 hoặc
  ngày đã làm > ngày chuẩn. Từ chối dữ liệu ngoài miền biểu diễn hữu hạn của getter
  double cũ. Dữ liệu sai không thay thế ngày công trước đó.
- Phân loại dùng so sánh nhân chéo chính xác: `đã làm * 100` với `chuẩn * ngưỡng`.
  Các ngưỡng là 100/90/75/50. Không làm tròn trước khi phân loại. Getter phần trăm
  vẫn trả double để tương thích GUI; `String.format` chỉ làm tròn phần hiển thị.
- Tìm kiếm giữ bảy tiêu chí và phạm vi một phòng ban. Hỗ trợ chuỗi, mã dạng số hoặc
  khoảng; từ chối số âm/khoảng đảo. Tuổi/thâm niên dùng năm tròn và `Clock` có thể
  cố định trong test. Dùng chung predicate khi áp dụng bộ lọc và tải lại dữ liệu.

Repository và các thao tác service phối hợp khóa trên cùng repository; kiểm tra
mã rồi thêm là nguyên tử kể cả khi dùng hai service khác nhau. Unit test có kiểm
tra hai yêu cầu thêm trùng mã đồng thời.

## Chạy Maven trên Java 17

Trên máy hiện tại, `java -version` là Java 17 nhưng Maven mặc định chọn Java 27.
Đã đặt `JAVA_HOME` cho riêng từng tiến trình kiểm thử, không sửa cấu hình môi trường
toàn máy:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
mvn -version
mvn package
mvn -Pgui-tests test
```

- Mặc định Maven chạy test trong `test/` ở chế độ headless.
- Profile `gui-tests` chạy toàn bộ `gui-test/`, cần desktop hoạt động.
- Hai profile dùng thư mục class/report riêng để không chạy nhầm test còn sót
  từ lần chạy profile kia. Không vô hiệu hóa hoặc ignore failure của test GUI.
- Profile GUI mở module `java.base/java.util` cho AssertJ Swing cũ để tránh lỗi
  reflection vào `TimerTask`; không sửa assertion hoặc expected của test.
- Có thể mở GUI từ JAR bằng
  `java -cp target/QuanLyNhanSu-1.0-SNAPSHOT.jar view.GUI`.

## Kết quả thực tế ngày 19/11/2026

Môi trường: Java Temurin 17.0.21, Maven 3.10.0, Windows 11.

| Lần kiểm tra | Kết quả |
|---|---|
| Baseline TX1 trước khi sửa | 58 test: 45 PASS, 13 FAIL, 0 ERROR, 0 SKIP |
| `mvn package` sau thay đổi | BUILD SUCCESS; 62 unit test PASS, 0 FAIL/ERROR/SKIP |
| Bộ 58 GUI test TX1 sau thay đổi | 55 PASS, 3 FAIL, 0 ERROR, 0 SKIP |
| 5 test tích hợp Swing mới | 5 PASS, 0 FAIL/ERROR/SKIP |
| Toàn bộ profile `gui-tests` | 63 test: 60 PASS, 3 FAIL, 0 ERROR/SKIP; lệnh trả exit code 1 |

62 unit test gồm 61 trường hợp mới và một smoke test TX1 được giữ nguyên.
Năm test tích hợp mới xác minh thêm qua form với cùng repository, chặn đổi mã
trùng trên bảng chính, chặn mã từ phòng khác trên bảng tìm kiếm, lưu hồ sơ lỗi
không đổi một phần dữ liệu, và cập nhật/giữ lựa chọn đúng nhân viên sau sắp xếp.

10 lỗi baseline được xử lý:
`TN_09`, `TN_10`, `TN_11`, `TN_14`, `NC_06`, `NC_11`, `NC_12`, `NC_16`,
`TK_06`, `TK_15`.

Ba lỗi baseline còn tồn tại, thuộc lịch sử chức vụ ngoài phạm vi ba service:

- `CV_04`: ngày kết thúc bằng ngày nhận vẫn được chấp nhận.
- `CV_07`: chưa từ chối khoảng chức vụ chồng lấn.
- `CV_11`: xóa chức vụ hiệu lực chưa cập nhật lại vị trí hiện tại.

Vì ba lỗi này, lệnh chạy toàn bộ GUI test vẫn trả mã lỗi; không tuyên bố toàn bộ
dự án đã xanh. Lỗi biên dịch trung gian do overload constructor GUI đã được sửa
bằng factory; bộ test TX1 đã biên dịch lại thành công.

Log và báo cáo:

- `target/tx2-stage1-20261119/baseline.log`, `baseline-reports/`, `baseline-evidence/`.
- Báo cáo có sẵn trước khi chạy được sao lưu ở `previous-reports/`, `previous-evidence/`.
- `target/tx2-stage1-20261119/package-final.log`.
- `target/tx2-stage1-20261119/gui-final.log`.
- `target/surefire-reports-unit/` và `target/surefire-reports-gui/`.
- JAR: `target/QuanLyNhanSu-1.0-SNAPSHOT.jar`.

## Giới hạn và việc tiếp theo

- Chưa xác minh thủ công toàn bộ tương tác chuột phải, shortcut, sửa trạng thái
  hàng loạt, CRUD phòng ban, bằng cấp và nút Hủy; các chức năng này vẫn được giữ
  trong view. Chưa chạy lại Ant/NetBeans build để tránh tác động sản phẩm build
  đang có thay đổi chưa commit.
- Lịch sử chức vụ/bằng cấp và một số setter trình bày vẫn thao tác model mutable.
  Không được coi repository hiện tại là snapshot bất biến hoàn toàn hoặc tự động
  an toàn cho mọi thao tác ngoài service. Code mới phải đi qua repository/service,
  không sửa trực tiếp danh sách/model trả về.
- Dữ liệu vẫn chỉ nằm trong RAM và mất khi thoát; nạp mẫu thay thế dữ liệu hiện tại.
  Mỗi nhân viên vẫn chỉ có một NgayCong hiện tại, chưa có kỳ tháng/năm.
- Chưa có cơ chế thông báo cho mọi cửa sổ tự tải lại khi dữ liệu bị thay đổi từ
  bên ngoài Swing. Trước khi chạy API cùng GUI cần bổ sung đồng bộ refresh trên EDT.
- Bước kế tiếp: thêm Spring Boot, cấu hình bean dùng chung repository/service;
  DTO/request/response và ánh xạ ngày ISO; controller thêm/tìm/cập nhật ngày công;
  exception handler với mã HTTP; định danh phòng ban ổn định; test HTTP và test
  API-GUI dùng chung dữ liệu. Không trả model có vòng NhanVien-NgayCong trực tiếp
  thành JSON. Quyết định lưu bền vững và phạm vi sửa ba lỗi chức vụ riêng.
