# Bàn giao và kiểm thử API TX2 — 20/11/2026

Nhánh `feature/tx2-rest-api`, HEAD/baseline vẫn
`3312331bcc5ef2796590734e4d74faef4abf2aba`. Không commit/push/reset/clean.
Môi trường: Windows desktop, Temurin JDK 17.0.21, Maven 3.10.0,
Spring Boot 3.5.16. Thời gian theo Asia/Bangkok.

## Kết quả cuối

| Bộ kiểm tra | Tổng | PASS | FAIL | ERROR/SKIP |
|---|---:|---:|---:|---:|
| API integration qua TCP, random port | 46 | 46 | 0 | 0/0 |
| Unit/service/characterization hiện có | 91 | 57 | 34 | 0/0 |
| GUI TX1 gốc | 58 | 45 | 13 | 0/0 |
| ServiceSwingIntegrationTest giai đoạn trước | 5 | 1 | 4 | 0/0 |
| GUI differential so trực tiếp commit TX1 | 47 | 47 | 0 | 0/0 |
| HTTP smoke từ Python tới JAR process riêng | 32 | 32 | 0 | — |

Không gộp HTTP smoke vào tổng JUnit. JUnit tổng 247: 196 PASS, 51 FAIL,
không ERROR hoặc SKIP. Đối chiếu từng tên/status test với kết quả giai đoạn
`target/tx1-behavior-preservation-20261119`: **không có test cũ thay đổi kết quả**.
Không chỉ đối chiếu tỷ lệ PASS/FAIL.

Lệnh thực tế:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
mvn -B -ntp compile
mvn -B -ntp -Papi-tests '-Dtest.reportsDirectory=target/tx2-api-20261120/api-reports-release' package
mvn -B -ntp '-Dtest.reportsDirectory=target/tx2-api-20261120/unit-reports' test
mvn -B -ntp -Pgui-tests '-Dtest.reportsDirectory=target/tx2-api-20261120/gui-reports' test
```

- Compile: BUILD SUCCESS.
- Package profile API: BUILD SUCCESS, 46/46 test PASS, không skip test.
- Unit và GUI: Maven BUILD FAILURE do assertion đã tồn tại; không dùng
  `testFailureIgnore`, không đổi/xóa/vô hiệu hóa test để build xanh.
- Không khẳng định `mvn package` mặc định sẽ thành công: lệnh đó chạy các test
  unit/spec còn FAIL. Profile chọn tập test riêng được công khai trong README.

JAR xuất ra: `target/QuanLyNhanSu-1.0-SNAPSHOT-api.jar` và JAR thường
`target/QuanLyNhanSu-1.0-SNAPSHOT.jar`. API JAR đã khởi động bằng JDK 17 với
`-Djava.awt.headless=true`, `--server.port=18080`, `--tx2.test-data.enabled=true`.
HTTP smoke gọi thật tất cả 5 endpoint yêu cầu và reset; không dùng MockMvc.
Process kiểm thử được dừng sau khi thu thập bằng chứng.

## Phân loại FAIL

13 test TX1 vẫn FAIL: `CV_04`, `CV_07`, `CV_11`, `NC_06`, `NC_11`, `NC_12`,
`NC_16`, `TK_06`, `TK_15`, `TN_09`, `TN_10`, `TN_11`, `TN_14`.
Đây là các lỗi đặc tả đã có trong baseline, gồm chức vụ, ngày công, tìm kiếm,
tuổi/ngày/mã trùng. Không sửa nghiệp vụ để thay kết quả.

34 FAIL unit/service và 4 FAIL ServiceSwingIntegrationTest đều giữ nguyên từ
lần tái cấu trúc bảo toàn hành vi. Các test này được viết ở giai đoạn đặt yêu cầu
validation chặt hơn TX1; một số giả định BigDecimal chính xác/scale cũng không
phù hợp model double TX1. Trong 34 FAIL unit có 9 assertion chỉ khác scale
BigDecimal. Phân tích chi tiết trước đó nằm ở
[báo cáo bảo toàn](../TX2_TX1_BEHAVIOR_PRESERVATION.md).
Không thay expected/assertion/dữ liệu test để đạt PASS.

Trong quá trình phát triển API, test âm mới phát hiện Jackson chuyển JSON number
thành String (`hoTen:123` trả 201). Hợp đồng HTTP quy định kiểu sai phải 400;
đã sửa riêng cấu hình coercion Jackson, không thay test và không thêm validation
nghiệp vụ. Kết quả phát hiện ban đầu **45 PASS/1 FAIL trên 46 test** được giữ ở
`target/tx2-api-20261120/api-reports-final` và `api-package-final.log`.
Lần xác minh sau sửa là `api-reports-release`/`api-package-release.log`: 46 PASS.
Không phát hiện lỗi API mới còn tồn tại trong phạm vi các test đã chạy.

## Hành vi đã kiểm chứng qua HTTP

- Thêm, đọc lại và tìm kiếm cùng dữ liệu của repository bean; API không tạo kho
  nhân viên riêng. Location/UUID đúng; mã trùng liên phòng có UUID khác nhau.
- Cập nhật UUID thứ hai không làm thay ngày công của nhân viên cùng mã thứ nhất.
- Tuổi dưới 18/ngày sinh bằng ngày vào làm được chấp nhận, thứ tự ngày sai bị từ
  chối theo Service. Sai định dạng ngày là lỗi HTTP/JSON riêng.
- Tìm văn bản/số/khoảng và khoảng đảo dùng nhánh TX1; không khớp trả danh sách rỗng.
- Double boundary 23.4/26, hơn hai chữ số lẻ, số mũ/hex float, NaN/Infinity được
  xử lý theo Service. Giá trị âm/quá chuẩn/chuỗi lỗi bị từ chối và giữ dữ liệu cũ.
- JSON số đặc biệt dùng chuỗi, ratio không hữu hạn cũng dùng chuỗi; response đọc
  lại hợp lệ, không tham chiếu vòng và không gọi getter BigDecimal không hữu hạn.
- Lỗi parse, kiểu, UUID, trường thiếu, unknown field, 404/405/415 trả JSON lỗi
  rõ ràng, không stack trace. 406/500 có handler nhưng chưa tạo test riêng.
- Fixture xác định, NV002 chưa tính; reset khôi phục 4 nhân viên và UUID cũ 404.
  Integration test riêng xác nhận reset mặc định tắt.
- Context API thực sự headless, không JFrame; API test reset mỗi case, không chạy
  song song trên cùng vùng nhớ. Hai context kiểm tra reset bật/tắt dùng cổng riêng.

## File thay đổi/tạo trong giai đoạn này

File có sẵn sửa: **`pom.xml`** — Boot BOM/web/test dependencies, resources,
executable JAR có classifier `api`, profile test API. Java release vẫn 17;
profile unit/GUI và test cũ giữ nguyên. Model, Repository, Service và Swing
không sửa trong giai đoạn API.

Mã mới:

```text
src/api/
  QuanLyNhanSuApiApplication.java   bootstrap API-only
  ApiConfiguration.java            bean Repository/Service và hợp đồng JSON
  ApiDtos.java                     request/response records
  ApiMapping.java                  snapshot DTO, Date và số đặc biệt
  ApiException.java                lỗi HTTP có chủ đích
  ApiErrorHandler.java             JSON lỗi an toàn
  NhanSuController.java            năm endpoint nghiệp vụ
  SampleData.java                  fixture loader vào Repository chung
  TestDataController.java          endpoint reset opt-in
resources/
  application.properties
  sample-data.json
api-test/api/
  NhanSuApiIntegrationTest.java
  ResetDisabledIntegrationTest.java
scripts/http_smoke.py
postman/QuanLyNhanSu_TX2.postman_collection.json
README_API.md
API_ENDPOINTS.md
reports/
  API_TEST_RESULTS.md
  tx2-api-20261120/
    http-results.json
    http-results.md
    test-summary.json
    test-cases.csv
```

Trước sửa đã chụp Git status, SHA-256 các file có sẵn và sao lưu source/tests/pom
ở `target/tx2-api-20261120/before`. Đối chiếu SHA-256 sau thực hiện: chỉ pom đổi
trong các file có sẵn, gồm cả thay đổi chưa commit và test cũ trước lần làm này.
Build products bẩn đã có từ trước không bị dọn/xóa. HEAD vẫn baseline 3312331.

## Bằng chứng

- [32 request/response HTTP thật](tx2-api-20261120/http-results.json) và
  [bảng HTTP](tx2-api-20261120/http-results.md).
- [Tổng hợp JUnit và delta](tx2-api-20261120/test-summary.json).
- [Từng test và phân loại](tx2-api-20261120/test-cases.csv).
- Log đầy đủ, XML Surefire và bản sao trước chỉnh sửa ở
  `target/tx2-api-20261120/`; thư mục target được Git ignore.
- Kết quả HTTP cũ trước bổ sung check kiểu text vẫn được giữ ở `.../http/` (31
  PASS). Kết quả trên JAR cuối ở `.../http-release/` (32 PASS); bản giao reports
  lấy lần cuối. Không ghi đè bằng chứng lỗi phát hiện trong quá trình phát triển.

Postman Collection đã tạo và parse JSON hợp lệ, chưa chạy Postman/Newman;
kiểm chứng thực tế dùng Java HttpClient và Python urllib.

## Khác biệt/giới hạn còn lại

Không thay nghiệp vụ ba chức năng trong Service. Các khác biệt giao tiếp có chủ
đích: API dùng ngày ISO và Asia/Bangkok, UUID kỹ thuật, lỗi HTTP/JSON rõ ràng,
tìm trên toàn công ty bằng Service từng phòng, mặc định trạng thái khi thêm,
biểu diễn nonfinite thành chuỗi. Những lựa chọn này được ghi trong hợp đồng API.

Giới hạn cũ về dialog giữ tham chiếu phòng ban đã xóa/nạp lại vẫn còn; không sửa
lịch sử chức vụ. Chưa kiểm tra thủ công mọi menu/context menu, bulk edit hoặc
lịch. GUI regression tự động có kết quả như trên. Chưa chạy Ant/NetBeans build;
Spring classpath cần Maven hoặc cấu hình bổ sung cho Ant.

API-only đã sẵn sàng để dùng cho TX2 với Postman và integration test. Chưa có
database/persistence, xác thực, phân trang, chế độ GUI/API cùng JVM, refresh GUI
qua EDT hay load test nhiều client. Repository/model vẫn mutable như giai đoạn
trước; khóa quanh thao tác API bảo vệ snapshot trong server này. Các hạn chế này
không được coi là đã giải quyết chỉ vì API test PASS.
