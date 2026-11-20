package api;

import java.time.LocalDate;
import java.util.Date;
import model.NgayCong;
import model.NhanVien;
import model.PhongBan;
import org.springframework.stereotype.Component;
import repository.NhanSuRepository;

/** Call while holding the repository monitor to return a consistent DTO snapshot. */
@Component
public class ApiMapping {
    private final NhanSuRepository repo;
    public ApiMapping(NhanSuRepository repo) { this.repo = repo; }

    public ApiDtos.Department department(PhongBan pb) {
        return new ApiDtos.Department(repo.idPhongBan(pb), pb.getTenPB(), pb.getIdTruongPhong(),
                repo.danhSachNhanVien(pb).size());
    }

    public ApiDtos.Employee employee(NhanVien nv) {
        PhongBan pb = repo.danhSachPhongBan().stream()
                .filter(p -> repo.danhSachNhanVien(p).contains(nv)).findFirst().orElseThrow();
        return new ApiDtos.Employee(repo.idNhanVien(nv), repo.idPhongBan(pb), nv.getId(),
                nv.getFullName(), date(nv.getBirthDate()), date(nv.getNgayVaoLam()), nv.getSoDT(),
                nv.getGioiTinh(), nv.getQueQuan(), nv.getTrangThai(), nv.getViTri(), work(nv.getNgayCong()));
    }

    public static ApiDtos.WorkDays work(NgayCong nc) {
        return nc == null ? null : new ApiDtos.WorkDays(number(nc.getSoNgayDaLam()),
                number(nc.getSoNgayChuan()), number(nc.tinhPhanTramHoanThanh()), nc.getPhanLoai());
    }
    private static Object number(double value) {
        return Double.isFinite(value) ? (Object) value : Double.toString(value);
    }
    static Date date(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ApiConfiguration.DATE_ZONE).toInstant());
    }
    private static LocalDate date(Date value) {
        return value == null ? null : value.toInstant().atZone(ApiConfiguration.DATE_ZONE).toLocalDate();
    }
}
