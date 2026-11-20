package api;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import model.NhanVien;
import model.PhongBan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.NhanSuRepository;
import service.NgayCongService;
import service.NhanVienService;
import service.TimKiemNhanVienService;

@RestController
@RequestMapping("/api")
public class NhanSuController {
    private final NhanSuRepository repo;
    private final NhanVienService employees;
    private final NgayCongService work;
    private final TimKiemNhanVienService search;
    private final ApiMapping mapping;
    public NhanSuController(NhanSuRepository repo, NhanVienService employees,
            NgayCongService work, TimKiemNhanVienService search, ApiMapping mapping) {
        this.repo = repo; this.employees = employees; this.work = work;
        this.search = search; this.mapping = mapping;
    }

    @GetMapping("/phong-ban")
    public List<ApiDtos.Department> departments() {
        synchronized (repo) { return repo.danhSachPhongBan().stream().map(mapping::department).toList(); }
    }

    @PostMapping("/nhan-vien")
    public ResponseEntity<ApiDtos.Employee> create(@RequestBody ApiDtos.CreateEmployee input) {
        if (input.phongBanId() == null) throw ApiException.badRequest("Thiếu phongBanId (UUID).");
        synchronized (repo) {
            PhongBan pb = repo.timPhongBanTheoId(input.phongBanId())
                    .orElseThrow(() -> ApiException.missing("phòng ban"));
            NhanVien nv = new NhanVien(input.hoTen(), ApiMapping.date(input.ngaySinh()), input.soDT(),
                    input.gioiTinh(), input.maNv(), input.trangThai() == null ? "Đang làm" : input.trangThai(),
                    input.viTri(), input.queQuan(), ApiMapping.date(input.ngayVaoLam()), null);
            employees.themNhanVien(pb, nv);
            ApiDtos.Employee result = mapping.employee(nv);
            return ResponseEntity.created(URI.create("/api/nhan-vien/" + result.id())).body(result);
        }
    }

    @GetMapping("/nhan-vien")
    public List<ApiDtos.Employee> search(@RequestParam("maNv") String maNv) {
        synchronized (repo) {
            List<ApiDtos.Employee> result = new ArrayList<>();
            for (PhongBan pb : repo.danhSachPhongBan()) {
                // Preserve TX1 numeric/range/text fallback, rather than imposing exact/unique code lookup.
                for (NhanVien nv : search.timKiem(pb, "Mã nhân viên", maNv)) result.add(mapping.employee(nv));
            }
            return List.copyOf(result);
        }
    }

    @GetMapping("/nhan-vien/{id}")
    public ApiDtos.Employee get(@PathVariable("id") UUID id) {
        synchronized (repo) { return mapping.employee(find(id)); }
    }

    @PutMapping("/nhan-vien/{id}/ngay-cong")
    public ApiDtos.Employee update(@PathVariable("id") UUID id, @RequestBody ApiDtos.UpdateWorkDays input) {
        String done = numericText(input.soNgayDaLam(), "soNgayDaLam");
        String standard = numericText(input.soNgayChuan(), "soNgayChuan");
        synchronized (repo) {
            NhanVien nv = find(id);
            work.capNhatNgayCong(nv, done, standard);
            return mapping.employee(nv);
        }
    }
    private NhanVien find(UUID id) {
        return repo.timNhanVienTheoId(id).orElseThrow(() -> ApiException.missing("nhân viên"));
    }
    private String numericText(JsonNode node, String field) {
        if (node == null || node.isNull() || !(node.isNumber() || node.isTextual())) {
            throw ApiException.badRequest(field + " phải là JSON number hoặc string, không được null.");
        }
        return node.asText(); // Existing string service parses Double, including TX1 special values.
    }
}
