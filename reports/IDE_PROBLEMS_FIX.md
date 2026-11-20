# Sửa cấu hình gây Problems — 20/11/2026

Đã sửa lỗi cấu hình khiến VS Code báo hàng loạt import/type không tồn tại.
Không thay hành vi nghiệp vụ TX1, không thay assertion hay dữ liệu test.

## Nguyên nhân và thay đổi

- `.project` trước đây chỉ có Java nature; `.classpath` chỉ có `src`, JRE mặc
  định và `dist/ChuongTrinhNhanSu.jar`. IDE đang coi đây là dự án Eclipse cũ,
  chưa nhận thư viện Spring/Jackson/Jakarta từ Maven. Đã bổ sung Maven nature,
  builder, Maven Dependencies và JavaSE-17; bỏ tham chiếu ngược JAR đầu ra cũ.
- Maven trên máy dùng JDK 27 trong khi lệnh `java` dùng JDK 17. Thêm Maven
  Toolchains 3.3.0, chọn JDK `[17,18)` cho compiler/Surefire. Lệnh kiểm chứng chạy
  bằng Maven hiện có, log xác nhận compiler và test JVM dùng Temurin 17.0.21.
- Trước đây các nguồn test thay đổi theo profile, IDE chỉ nhận được một nhóm.
  Thêm `ide-tests` với build-helper 3.6.1, bật riêng trong m2e preferences để IDE
  nhận cả ba thư mục. Output IDE riêng, không trộn class với profile unit/API/GUI.
- Đồng bộ toàn bộ JUnit bằng BOM 5.11.4, gồm Jupiter API/Params/Engine 5.11.4
  và Platform 1.11.4. Tránh phiên bản một phần do BOM Spring quản lý khác phiên
  bản JUnit khai báo trực tiếp trước đó. Không đổi logic/assertion test.
- Xóa hàm private `GUI.generateRandomNhanVien` không còn được gọi và import
  `TrinhDo` chỉ dùng trong hàm đó. Fixture GUI hiện tại vẫn giữ nguyên.
- Thêm `serialVersionUID` cho `ApiException`, bỏ hai import không dùng trong
  `Tx1SwingBehaviorCompatibilityTest`. Phần thân và assertion test không đổi.
- Cấu hình Java 17/UTF-8 trong `.settings`; cấu hình cục bộ VS Code cho JDK 17,
  terminal và tự cập nhật build. Không tắt diagnostics, không sửa test gốc để
  làm sạch kết quả kiểm thử.

Java language server đã ghi nhận runtime JavaSE-17, cập nhật cấu hình Maven
và báo `0 problems reported for /GUI.java`. Cấu hình `.classpath` được m2e tạo lại
với Maven container và đủ ba nguồn test. Không dùng nội dung marker cache cũ làm
bằng chứng rằng toàn bộ Problems hiện tại đã hết; kiểm tra compiler bên dưới
là kiểm tra độc lập có thể chạy lại.

## Kiểm chứng thực tế

| Kiểm tra | Kết quả |
|---|---|
| `mvn -Pide-tests test-compile` | SUCCESS, toàn bộ mã và nguồn test biên dịch được |
| `mvn -Papi-tests package` | SUCCESS, API 46 PASS / 0 FAIL |
| `mvn test` | 57 PASS / 34 FAIL, không ERROR/SKIP |
| `mvn -Pgui-tests test` | 93 PASS / 17 FAIL, không ERROR/SKIP |
| Eclipse batch compiler, Java 17, mã ứng dụng | 0 lỗi / 0 cảnh báo |
| Eclipse batch compiler, mã ứng dụng và toàn bộ test | 0 lỗi / 2 cảnh báo ở GUI test TX1 gốc |

Các lệnh thực tế dùng `-B -ntp` và báo cáo riêng trong
`target/tx2-problems-fix-20261120/`. Eclipse batch compiler được lấy từ extension
Java đang cài, dùng thư viện hệ thống JDK 17 và classpath do Maven giải quyết.
Maven package/API và biên dịch thành công không đồng nghĩa toàn bộ test PASS.
Hai lệnh unit/GUI vẫn trả exit code 1 do assertion thất bại.

So sánh từng tên/status với lần bổ sung API: **không có test nào đổi kết quả**.
GUI TX1 gốc vẫn 45 PASS/13 FAIL; 47 characterization GUI vẫn PASS. 34 FAIL unit
và 4 FAIL integration Swing là xung đột đặc tả/BigDecimal với hành vi TX1 đã được
phân tích trước, không phải lỗi compile/import mới. Không sửa nghiệp vụ để thay
baseline khi phạm vi sửa nghiệp vụ chưa được xác nhận lại.

Hai cảnh báo giữ nguyên trong `gui-test/GUIBlackBox58Test.java`:

1. Dòng 20: import `java.util.function.Consumer` không được dùng.
2. Dòng 169: `@SuppressWarnings("unchecked")` không cần thiết.

Đây là cảnh báo vệ sinh mã test, không gây lỗi compile hoặc lỗi chạy API. File
58 GUI test gốc được giữ nguyên theo yêu cầu trước đó. Không đặt cảnh báo thành
ignore để làm bảng Problems trống. Ngoài hai cảnh báo này, không phát hiện lỗi
hoặc cảnh báo compiler trong phạm vi kiểm tra toàn bộ source hiện có.

## File liên quan

File có sẵn thay đổi trong lượt sửa này:

```text
.classpath
.project
pom.xml
src/view/GUI.java
src/api/ApiException.java
gui-test/Tx1SwingBehaviorCompatibilityTest.java   chỉ bỏ hai import
README_API.md
```

Cấu hình và báo cáo mới:

```text
.settings/org.eclipse.core.resources.prefs
.settings/org.eclipse.jdt.core.prefs
.settings/org.eclipse.jdt.apt.core.prefs          do m2e tạo
.settings/org.eclipse.m2e.core.prefs
.vscode/settings.json                          cục bộ, Git ignore
reports/IDE_PROBLEMS_FIX.md
reports/tx2-problems-fix-20261120/test-summary.json
```

Trước sửa đã sao lưu 118 file hiện có, Git status và marker IDE tại
`target/tx2-problems-fix-20261120/before`. Class test từng được sinh chung trong
lượt kiểm tra IDE đầu tiên đã được chuyển nguyên thư mục sang bản sao lưu
`test-classes-before-isolation`; không xóa dữ liệu. Output IDE sau đó được tách
riêng và unit test được biên dịch/chạy lại từ output riêng.

Log API, unit, GUI, Eclipse compiler, dependency tree và tổng hợp nằm cùng thư
mục bằng chứng. SHA-256 xác nhận file 58 GUI test gốc, các unit/service test và
các model/Service/Repository nghiệp vụ không thay đổi trong lượt này. Không
commit/push/reset/clean. Ant/NetBeans build chưa được chuyển sang Maven; hướng
build được kiểm chứng ở đây là Maven và IDE dùng m2e.

Tham khảo cấu hình: [Maven JDK discovery](https://maven.apache.org/plugins/maven-toolchains-plugin/toolchains/jdk-discovery.html),
[quản lý dự án Java trong VS Code](https://code.visualstudio.com/docs/java/java-project).
