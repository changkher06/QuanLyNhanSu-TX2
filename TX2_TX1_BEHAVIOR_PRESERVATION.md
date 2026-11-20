# TX2: tái cấu trúc bảo toàn hành vi TX1

Ngày thực hiện: 19/11/2026. Nhánh: `feature/tx2-rest-api`. Baseline: `3312331`.
Không commit, push, reset hoặc clean. Chưa thêm Spring Boot hay REST Controller.

Mục tiêu là chuyển nghiệp vụ khỏi Swing để dùng lại qua API, giữ cả những khiếm
khuyết đang tồn tại của TX1. Test đặc tả vẫn đánh giá theo yêu cầu đúng; test bảo
toàn đánh giá sự tương đương với baseline. Không ép số lượng PASS/FAIL.

## Bảo vệ thay đổi chưa commit

Trước khi chỉnh sửa đã sao lưu tất cả file modified/untracked do Git liệt kê,
bao gồm source/test giai đoạn trước và các sản phẩm build đang có thay đổi:

`target/tx1-behavior-preservation-20261119/before/`

Các báo cáo và ảnh GUI đang có được sao lưu trong `previous-results/`. Source Java
nguyên bản đọc bằng `git show 3312331:...` nằm trong `baseline-source/`. Có lưu trạng
thái Git ban đầu và SHA-256 của toàn bộ test đã có, `NhanVien.java`, `ChucVu.java`.
Kết quả kiểm tra cuối: các file được bảo vệ không đổi. Không sửa `pom.xml` thêm
trong lần này; vẫn dùng cấu hình profile đã có của giai đoạn trước.

## File chỉnh sửa trong lần bảo toàn hành vi

| File | Thay đổi |
|---|---|
| `src/repository/NhanSuRepository.java` | Thêm tra cứu UUID và danh sách kết quả theo mã |
| `src/repository/InMemoryNhanSuRepository.java` | Bỏ cưỡng chế mã duy nhất; thêm metadata UUID theo identity model |
| `src/service/NhanVienService.java` | Bỏ kiểm tra tuổi 18; giữ Date.before và thứ tự ghi của TX1 |
| `src/service/NgayCongService.java` | Khôi phục Double.parseDouble và ba điều kiện giới hạn nguyên bản |
| `src/service/TimKiemNhanVienService.java` | Chuyển trực tiếp hai luồng lọc TX1, giữ int, fallback và Calendar |
| `src/model/NgayCong.java` | Khôi phục field/công thức/phân loại/setter double |
| `src/view/GUI.java` | Khôi phục chọn lại viewIndex và trim ở đường chỉnh sửa nhanh mã |
| `src/view/TimKiemNhanVienDialog.java` | Gọi service theo đúng luồng Áp dụng/lọc lại TX1 |
| `TX2_STAGE1.md` | Đánh dấu tài liệu trước là lịch sử, dẫn đến báo cáo hiện tại |

File mới:

- `test/compatibility/Tx1ServiceBehaviorTest.java`: 23 trường hợp characterization.
- `test/compatibility/RepositoryRecordIdentityTest.java`: 6 test định danh/kiến trúc.
- `gui-test/Tx1SwingBehaviorCompatibilityTest.java`: 47 test so sánh GUI với mã TX1 thật.
- `TX2_TX1_BEHAVIOR_PRESERVATION.md`: báo cáo này.

`GUINhanVien.java` đã gọi service từ giai đoạn trước nên không cần sửa thêm trong
lần này; thứ tự cập nhật TX1 được khôi phục ở service. Các phương thức bằng cấp
và lịch sử chức vụ trong view không khác baseline. `NhanVien.java` và `ChucVu.java`
được giữ nguyên. Các file test đã có, bao gồm 58 GUI test TX1 và các test bổ sung
giai đoạn trước, giữ nguyên cả assertion, expected và dữ liệu.

## Hành vi đã đối chiếu với 3312331

**Thêm nhân viên**

- Cho phép trùng mã trong cùng phòng và giữa các phòng; không ghi đè bản ghi cũ.
- Mã/tên trống và ngày bắt buộc được kiểm tra như luồng thêm của TX1.
- Kiểm tra ngày vào làm trước ngày sinh bằng `Date.before`, giữ cả ý nghĩa timestamp.
- Không kiểm tra đủ 18 tuổi. Ngày sinh bằng ngày vào làm vẫn được chấp nhận.
- Nhân viên thêm mới vẫn được gán ngày công 0/26; fixture NV002 vẫn có ngày công null.

**Sửa thông tin liên quan đến ba chức năng**

- Đổi mã là gán setter, cho phép mã trùng và không tự cập nhật `idTruongPhong`.
- Inline/quick edit ngày vẫn chỉ parse ngày ở view rồi gán, không thêm kiểm tra tuổi
  hoặc quan hệ giữa hai ngày.
- Lưu hồ sơ kiểm tra mã/tên trước, đổi mã, rồi mới kiểm tra ngày. Khi ngày không hợp
  lệ, mã có thể đã đổi nhưng các trường khác chưa đổi, giống thứ tự TX1.
- Không sửa logic lịch sử chức vụ hoặc bằng cấp để làm các test CV PASS.

**Ngày công**

- Đọc chuỗi bằng `Double.parseDouble`; chỉ kiểm tra:
  `đã làm < 0 || chuẩn <= 0 || đã làm > chuẩn`.
- Không thêm kiểm tra `Double.isFinite` hoặc giới hạn hai chữ số thập phân.
- NaN đi qua các phép so sánh như TX1. Infinity ở ngày đã làm với chuẩn 26 bị từ
  chối; chuẩn Infinity và cặp Infinity/Infinity xử lý theo đúng ba phép so sánh cũ.
- Số 12.345 hoặc chuẩn 25.123 được lưu theo double, không làm tròn trước khi lưu.
- Phần trăm là `đã làm / chuẩn * 100`; phân loại so sánh trực tiếp tỷ lệ double
  với 100/90/75/50. Ví dụ 23.4/26 hiển thị 90.0% nhưng vẫn là Khá như TX1.
- Đầu vào bị từ chối không thay thế NgayCong đang có. Giữ reset toàn phòng về 0/26.
- Các chữ ký BigDecimal được thêm ở giai đoạn trước còn tồn tại để không làm test
  mất khả năng biên dịch. Chúng là adapter sang double, không áp đặt độ chính xác
  BigDecimal lên công thức TX1. Getter có hậu tố ChinhXac chỉ chuyển double hữu hạn
  sang BigDecimal; tên cũ không có nghĩa dữ liệu vẫn lưu thập phân chính xác.

**Tìm kiếm**

- Giữ nguyên phân biệt giữa áp dụng bộ lọc và khớp lại sau chỉnh sửa. Luồng áp dụng
  dùng callback dữ liệu/lỗi không phụ thuộc Swing để giữ cả phạm vi try/catch cũ;
  API có thể gọi `timKiem` để lấy List trực tiếp.
- Khoảng mã đảo 3-1 rơi xuống tìm chuỗi; không bổ sung lỗi validation.
- Tuổi -1 có thể rơi xuống nhánh chuỗi/vị trí như TX1. Khoảng tuổi 30-18 vẫn đi vào
  nhánh lỗi đã có. Chuỗi trống ở Áp dụng báo lỗi, còn khớp lại với chuỗi trống trả true.
- Giữ `Integer.parseInt`, nhánh bắt NumberFormatException, cách chuyển chữ thường
  theo locale mặc định, và cách đọc lại tiêu chí/ô nhập tại thời điểm refresh.
- Tuổi/thâm niên dùng Calendar.YEAR và DAY_OF_YEAR như baseline, bao gồm khác biệt
  qua năm nhuận. Clock chỉ giúp truyền ngày/múi giờ trong test, không thay bằng Period.

## Định danh kỹ thuật cho API

Repository giữ nguyên một cây model PhongBan/NhanVien. Hai IdentityHashMap chỉ lưu
metadata model -> UUID, không chứa bản sao hồ sơ nhân viên. UUID được cấp cho cả
phòng ban lẫn nhân viên, độc lập tên phòng và mã nhân viên.

- `idNhanVien`, `idPhongBan`: lấy UUID của model đang được quản lý.
- `timNhanVienTheoId`, `timPhongBanTheoId`: tra đúng đối tượng để thao tác qua API.
- `timTatCaTheoMa`: trả mọi bản ghi có cùng mã nghiệp vụ, giữ thứ tự danh sách.
- `timTheoMa` được giữ làm tiện ích đọc tương thích giai đoạn trước: kết quả đầu
  tiên với so sánh trim/không phân biệt hoa thường. Không dùng nó để định danh hoặc
  cập nhật một bản ghi qua API. Đây không phải thuật toán tìm kiếm GUI TX1.

Đổi mã không đổi UUID; xóa bản ghi/phòng ban làm UUID tương ứng không còn tra được.
Nạp lại chính các model cũ giữ UUID, nạp mẫu bằng model mới cấp UUID mới. Các thao
tác cấp ID/thêm/xóa có khóa trên repository. Hai lần thêm mã trùng đồng thời giữ
cả hai đối tượng với UUID khác nhau. Cùng một object xuất hiện nhiều nơi là cùng
identity; UUID đại diện đối tượng hồ sơ, không đại diện số thứ tự dòng trên bảng.

UUID và dữ liệu chỉ bền trong vòng đời repository hiện tại, chưa được lưu ra đĩa.
Endpoint tương lai nên dùng UUID làm path ID, giữ maNhanVien là dữ liệu nghiệp vụ.

## Kết quả thực tế

Môi trường: Windows 11, Temurin JDK 17.0.21, Maven 3.10.0.
Đã đặt JAVA_HOME riêng cho tiến trình vì Maven mặc định trên máy chọn JDK 27.

| Bộ kiểm thử | Tổng | PASS | FAIL | ERROR/SKIP |
|---|---:|---:|---:|---:|
| 58 GUI test TX1 giữ nguyên | 58 | 45 | 13 | 0/0 |
| Unit test đã có từ giai đoạn trước | 62 | 28 | 34 | 0/0 |
| Integration Swing giai đoạn trước giữ nguyên | 5 | 1 | 4 | 0/0 |
| Characterization service mới | 23 | 23 | 0 | 0/0 |
| Test UUID/repository mới | 6 | 6 | 0 | 0/0 |
| Differential GUI với baseline thật | 47 | 47 | 0 | 0/0 |

Tổng bộ unit mặc định: 91 test, 57 PASS / 34 FAIL.
Tổng profile gui-tests: 110 test, 93 PASS / 17 FAIL.
Hai lệnh test đều trả exit code 1; không bật ignore failure, không skip test.

Biên dịch/đóng gói bằng `mvn compile test-compile jar:jar`: BUILD SUCCESS, tạo
`target/QuanLyNhanSu-1.0-SNAPSHOT.jar`. Lệnh này kiểm tra biên dịch/đóng gói, không
được diễn giải là toàn bộ test xanh. Không chạy `mvn clean`.

47 test differential đọc source trực tiếp từ commit bằng Git, biên dịch toàn bộ
11 lớp TX1 với JavaCompiler và classloader độc lập dưới `target/tx1-oracle-*`.
Chúng chạy cùng thao tác trên hai phiên bản rồi so sánh dữ liệu, thông báo, dòng
bảng, trạng thái chọn/form và bit double thực tế. Không sửa source oracle, không
trả response giả, không đặt target số FAIL; thiếu Git/JDK thì báo ERROR, không skip.

13 test GUI TX1 FAIL hiện tại trùng đúng danh sách ở báo cáo baseline đã lưu:

`CV_04`, `CV_07`, `CV_11`, `NC_06`, `NC_11`, `NC_12`, `NC_16`, `TK_06`, `TK_15`,
`TN_09`, `TN_10`, `TN_11`, `TN_14`.

Không có test TX1 chuyển từ PASS sang FAIL hoặc từ FAIL sang PASS. Đây là kết quả
quan sát, không phải điều kiện được hardcode vào chương trình.

## Phân loại các test giai đoạn trước đang FAIL

- Chặn mã trùng, chỉ nhận một mã khi thêm đồng thời, cập nhật mã trưởng phòng:
  yêu cầu sửa nghiệp vụ giai đoạn trước; TX1 không có các kiểm tra/tự động hóa này.
- Đủ 18 tuổi, validation inline, hồ sơ từ chối người dưới 18: đặc tả khác hành vi TX1.
- Hữu hạn, tối đa hai chữ số, tỷ lệ thập phân chính xác: đặc tả khác double TX1.
- Tuổi năm tròn và validation tiêu chí/khoảng/số âm: một số kỳ vọng khác Calendar
  hoặc fallback TX1. Đặc biệt ngày sinh năm nhuận có thể lệch theo DAY_OF_YEAR.
- Trong 34 unit FAIL có **9 trường hợp phụ thuộc biểu diễn BigDecimal/scale**,
  ví dụ BigDecimal.equals coi 26 khác 26.0. Không được gộp chúng thành lỗi tính toán
  mới. Phần còn lại phản ánh các hợp đồng validation/precision giai đoạn trước.
- Trong 4 integration FAIL, ba test chờ thông báo từ chối mã trùng/tuổi 18 nhưng
  TX1 chấp nhận nên không có popup. Test còn lại đòi Giỏi cho 23.4/26 và thất bại
  ngay assertion phân loại, chưa đi đến các assertion lựa chọn dòng phía sau.

Không sửa expected/assertion/test data để đổi kết quả. Test bảo toàn mới là bộ
riêng, không thay thế hoặc vô hiệu hóa test đặc tả. Chi tiết từng test và assertion
thất bại được xuất ra CSV/JSON trong thư mục kết quả.

## Bằng chứng và chạy lại

Thư mục kết quả: `target/tx1-behavior-preservation-20261119/`.

- `build.log`, `unit-final.log`, `gui.log`: log lệnh thực tế.
- `unit-reports/`, `gui-reports/`: XML/TXT Surefire.
- `verification-summary.md`, `verification-summary.json`: tổng hợp và đối chiếu baseline.
- `test-classification.csv`: từng test, kết quả, phân loại, nguyên nhân, failure message.
- `protected-sha256.json`: SHA-256 trước khi sửa của các test đã có và hai model lịch sử.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
mvn -B -ntp compile test-compile jar:jar
mvn -B -ntp test
mvn -B -ntp -Pgui-tests test
```

Các lệnh test thường lưu vào thư mục report theo pom.xml. Lần thực hiện này truyền
`-Dtest.reportsDirectory=target/tx1-behavior-preservation-20261119/unit-reports`
hoặc `.../gui-reports` để lưu bằng chứng riêng, không ghi đè báo cáo trước đó.

## Khác biệt còn lại và mức sẵn sàng REST

Không phát hiện khác biệt nghiệp vụ trong 47 trường hợp GUI đối chiếu trực tiếp
và 23 characterization service. Chưa coi đây là chứng minh tương đương cho mọi
chuỗi thao tác của toàn ứng dụng.

Các thay đổi kỹ thuật có chủ đích còn lại gồm UUID, snapshot danh sách không sửa
được, khóa repository và kiểm tra tham chiếu thuộc kho. Các phần tử model vẫn
mutable. Tình huống cửa sổ tìm kiếm còn mở khi phòng ban bị xóa/nạp lại mẫu có
guard dữ liệu thuộc repository, khác việc TX1 giữ tham chiếu phòng ban cũ; chưa có
test differential cho vòng đời dialog này. Chưa kiểm tra thủ công toàn bộ menu
chuột phải, bulk edit, lịch và đường đóng/hủy. Không chạy lại Ant/NetBeans build.

Repository/service đã có thể khởi tạo thành bean và dùng chung với Swing, tra bản
ghi bằng UUID mà không làm mất mã trùng. Các bước còn lại trước khi dùng REST:

1. Thêm Spring Boot, bootstrap/headless và chế độ GUI/API/kết hợp.
2. DTO và ánh xạ UUID/ngày; không trả model trực tiếp vì vòng NhanVien-NgayCong và
   các getter BigDecimal hữu hạn. JSON không có số NaN/Infinity chuẩn: cần hợp đồng
   biểu diễn chuỗi hoặc null kèm giá trị gốc, không thay đổi phép tính service.
3. Controller và exception handler HTTP, tránh vô tình thêm Unique/MinAge/Digits
   validation vào DTO khiến hành vi lại khác baseline.
4. Refresh GUI qua EDT khi API cập nhật, quy tắc vòng đời dialog và đọc/ghi đồng thời.
5. Test HTTP đặc tả riêng với test bảo toàn service; lưu bằng chứng cả PASS lẫn FAIL.

Các lỗi nghiệp vụ TX1 vẫn được giữ để phục vụ kiểm thử. Sửa chúng cần một hạng mục
riêng có phạm vi và kết quả kiểm thử được phân biệt với việc bổ sung API.
