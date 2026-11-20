package api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import model.ChucVu;
import model.NgayCong;
import model.NhanVien;
import model.PhongBan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import repository.NhanSuRepository;

/** Fixture loader, outside controllers. Exactly one repository stores live API data. */
@Component
public class SampleData {
    public record SeedEmployee(ApiDtos.CreateEmployee nhanVien, boolean daTinhNgayCong, boolean coLichSuChucVu) {}
    public record SeedDepartment(String tenPhongBan, List<SeedEmployee> nhanVien) {}
    private final NhanSuRepository repo;
    private final ObjectMapper json;
    public SampleData(NhanSuRepository repo, ObjectMapper json) { this.repo = repo; this.json = json; }

    @PostConstruct
    public void initialize() throws IOException { reset(); }

    public ApiDtos.ResetResult reset() throws IOException {
        // Parse and construct before replacing the live tree; malformed fixtures leave live data intact.
        SeedDepartment[] fixture;
        try (InputStream input = new ClassPathResource("sample-data.json").getInputStream()) {
            fixture = json.readValue(input, SeedDepartment[].class);
        }
        List<PhongBan> departments = new ArrayList<>();
        int count = 0;
        for (SeedDepartment seed : fixture) {
            ArrayList<NhanVien> employees = new ArrayList<>();
            for (SeedEmployee row : seed.nhanVien()) {
                var in = row.nhanVien();
                NhanVien nv = new NhanVien(in.hoTen(), ApiMapping.date(in.ngaySinh()), in.soDT(),
                        in.gioiTinh(), in.maNv(), in.trangThai(), in.viTri(), in.queQuan(),
                        ApiMapping.date(in.ngayVaoLam()), null);
                if (row.daTinhNgayCong()) nv.setNgayCong(new NgayCong(nv, 0, 26));
                if (row.coLichSuChucVu()) {
                    nv.getLichSuChucVu().add(new ChucVu(in.viTri(), nv.getNgayVaoLam(), null));
                }
                employees.add(nv);
                count++;
            }
            departments.add(new PhongBan(seed.tenPhongBan(), "", employees));
        }
        synchronized (repo) {
            repo.thayThePhongBan(departments);
            return new ApiDtos.ResetResult(departments.size(), count);
        }
    }
}
