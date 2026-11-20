# QuanLyNhanSu — REST API TX2

API-only dùng Java 17, Maven và Spring Boot **3.5.16**. Phiên bản này hỗ trợ Java 17
theo [tài liệu Spring Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
Entry point `api.QuanLyNhanSuApiApplication` không tạo JFrame; Controller gọi đúng
Repository/Service của giai đoạn tái cấu trúc. Model, nghiệp vụ và test cũ không đổi.

## Build và chạy

Tại thư mục dự án, dùng JDK 17 cho cả Maven và lệnh java:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -version
mvn -B -ntp compile
mvn -B -ntp -Papi-tests package
java -Djava.awt.headless=true -jar target/QuanLyNhanSu-1.0-SNAPSHOT-api.jar
```

Thay đường dẫn JDK theo máy. API mặc định: `http://127.0.0.1:8080`.
Đổi cổng bằng `--server.port=18080`. Ctrl+C để dừng.
JAR thường vẫn có tại `target/QuanLyNhanSu-1.0-SNAPSHOT.jar`;
GUI có thể mở bằng `java -cp target/QuanLyNhanSu-1.0-SNAPSHOT.jar view.GUI`.
GUI và API khi chạy thành hai process có hai vùng nhớ riêng. Chế độ kết hợp trong
cùng JVM và tự cập nhật GUI khi API ghi dữ liệu chưa được triển khai.

Profile `api-tests` chỉ chọn thư mục `api-test`; không có thao tác bỏ qua hoặc
vô hiệu hóa assertion cũ. **Package thành công ở profile API không đồng nghĩa toàn
bộ test của dự án PASS.** Các lệnh mặc định/GUI bên dưới vẫn chạy và trả mã lỗi
khi test đặc tả đang xung đột với hành vi TX1.

## Mở bằng VS Code/Eclipse và chọn JDK

Mở thư mục chứa `pom.xml` như một dự án Maven. `.project` đã có Maven nature,
`.classpath` dùng Maven Dependencies và JavaSE-17, không tham chiếu ngược JAR
Swing đã build trong `dist`. Cấu hình trong `.settings` giữ Java 17/UTF-8 và bật
profile `ide-tests` riêng cho m2e để IDE nhận cả `test`, `gui-test`, `api-test`.
Output IDE là `target/test-classes-ide`, tách khỏi output từng profile Maven.

`ide-tests` dùng để đăng ký và biên dịch các nguồn test trong IDE; các lệnh chạy
test chính thức vẫn là ba lệnh ở mục Kiểm thử. Có thể kiểm tra toàn bộ nguồn test
bằng `mvn -Pide-tests test-compile`; không dùng profile này để chạy chung các test
GUI headful và API headless trong một JVM test.

Maven Toolchains tự tìm JDK 17 đã cài và dùng nó cho compiler/Surefire, kể cả khi
Maven launcher đang dùng JDK mới hơn. Nếu không tìm thấy, cài JDK 17 hoặc khai báo
đường dẫn trong `~/.m2/toolchains.xml`; không đổi Java của dự án lên 27 để né lỗi.
Các thành phần JUnit được đồng bộ qua BOM 5.11.4, giữ phiên bản JUnit của TX1.

Trên máy hiện tại, `.vscode/settings.json` cấu hình runtime/terminal về JDK 17
và tự cập nhật Maven. File này là cấu hình cục bộ được Git ignore; trên máy khác
chọn JavaSE-17 qua `Java: Configure Java Runtime`. JDK dùng để chạy Java language
server có thể mới hơn JDK của dự án; không ép language server về JDK 17.

Nếu IDE còn giữ Problems cũ sau khi Maven đã compile được, chạy
`Java: Reload Projects` hoặc `Developer: Reload Window` trong Command Palette.
Không tắt diagnostics để che lỗi. Kết quả kiểm tra cấu hình gần nhất ở
[báo cáo Problems](reports/IDE_PROBLEMS_FIX.md).

## Dữ liệu mẫu và reset

[resources/sample-data.json](resources/sample-data.json) chứa hai phòng ban:

| Phòng ban | Mã nhân viên | Ngày công ban đầu |
|---|---|---|
| Kỹ thuật | NV001, NV002, NV003 | 0/26; NV002 chưa tính (`null`) |
| Kinh doanh | NV100 | 0/26 |

Mỗi lần khởi động, fixture được nạp vào **một** repository trong bộ nhớ, bên ngoài
Controller. Các giá trị nghiệp vụ và thứ tự xác định; UUID kỹ thuật được cấp mới.
Tra UUID phòng ban bằng `GET /api/phong-ban`, UUID nhân viên bằng tìm kiếm.
Không lưu dữ liệu qua lần khởi động. Thêm/cập nhật gọi Service; fixture import
dùng Repository để giữ trạng thái mẫu “chưa tính” của NV002.

Server kiểm thử riêng có thể bật reset:

```powershell
java -Djava.awt.headless=true -jar target/QuanLyNhanSu-1.0-SNAPSHOT-api.jar --tx2.test-data.enabled=true
Invoke-RestMethod -Method Post http://127.0.0.1:8080/api/test-data/reset
```

Reset thay toàn bộ cây dữ liệu, trả `{"soPhongBan":2,"soNhanVien":4}` và làm UUID
cũ hết hiệu lực. Mặc định endpoint reset không tồn tại (404). Server chỉ bind
loopback; chưa có xác thực, không dùng reset/server này cho dữ liệu thật.
Test HTTP chạy tuần tự và reset trước mỗi test, dùng cổng ngẫu nhiên và context
riêng; tránh chạy nhiều test cùng ghi vào một server dùng chung.

## Kiểm thử và bằng chứng

```powershell
mvn -B -ntp -Papi-tests test
mvn -B -ntp test
mvn -B -ntp -Pgui-tests test
```

GUI test cần Windows desktop đang hoạt động. Các profile chọn lần lượt API,
unit/service/characterization và Swing; cần chạy cả ba để đánh giá đầy đủ.
Không kết hợp profile `api-tests` và `gui-tests` trong cùng một lệnh.
Log/Surefire mặc định ở `target/surefire-reports-api`, `...-unit`, `...-gui`.

Kiểm thử HTTP từ process bên ngoài với server reset đã bật:

```powershell
python scripts/http_smoke.py --base-url http://127.0.0.1:8080 --output target/http-smoke
```

Script dùng Python standard library, reset ở đầu/cuối luồng thành công và xuất
request/response JSON thật. Nếu thất bại giữa chừng, bằng chứng vẫn được xuất;
hãy reset lại server trước lượt tiếp theo. Script trả exit code 1 nếu có lỗi.

Import [Postman Collection](postman/QuanLyNhanSu_TX2.postman_collection.json),
đặt collection variable `baseUrl` rồi chạy theo thứ tự. Collection tự lấy UUID,
kiểm tra status và reset đầu/cuối; không dùng UUID của lần chạy cũ.
Collection đã kiểm tra JSON; chưa chạy bằng ứng dụng Postman/Newman.

Kết quả thực tế và phân loại: [API_TEST_RESULTS.md](reports/API_TEST_RESULTS.md).
Hợp đồng JSON, status và tìm kiếm: [API_ENDPOINTS.md](API_ENDPOINTS.md).

## Hành vi TX1 được giữ

- Mã nhân viên có thể trùng; UUID xác định một bản ghi.
- Service thêm chỉ kiểm tra thứ tự ngày, không tự bổ sung điều kiện đủ 18 tuổi.
- Ngày công dùng `double`, ba phép so sánh cũ; NaN/Infinity có thể được chấp nhận.
- Phân loại dùng phép tính TX1, ví dụ 23.4/26 có thể là `Khá` dù hiển thị tròn 90%.
- `maNv` dùng bộ lọc TX1, gồm số, khoảng, văn bản và nhánh fallback.

Đây là baseline phục vụ kiểm thử phần mềm. Chưa sửa 13 lỗi đặc tả TX1.
Chuyển JSON/date/UUID và HTTP status là hợp đồng giao tiếp mới, không thêm
Unique/MinAge/Digits vào nghiệp vụ. Ngày API là ISO `yyyy-MM-dd`, quy đổi đầu ngày
theo `Asia/Bangkok`; GUI vẫn dùng cơ chế nhập ngày hiện có.

## Phạm vi và giới hạn

Mã mới ở `src/api`, resources, `api-test`, scripts, postman và tài liệu/report;
file cũ duy nhất sửa trong giai đoạn API là `pom.xml`. Chi tiết bàn giao trong
báo cáo kết quả. Không commit/push; các thay đổi chưa commit trước đó được giữ.

Chưa có database, xác thực/phân quyền, phân trang hay chế độ GUI/API cùng JVM.
Khóa repository bao trọn lookup–service–mapping cho API/reset, chưa thay đổi cơ
chế mutable model của Swing. Giới hạn dialog giữ phòng ban đã bị xóa/nạp lại từ
giai đoạn trước vẫn còn. Ant/NetBeans build chưa được cấu hình classpath Spring;
hướng chạy được kiểm chứng là Maven/JDK 17. Chưa kiểm tra thủ công toàn bộ menu,
lịch, lịch sử chức vụ hoặc tải đồng thời nhiều client.
