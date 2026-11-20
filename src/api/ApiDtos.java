package api;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.UUID;

/** Transport DTOs only. Business validation stays in the existing services. */
public final class ApiDtos {
    private ApiDtos() {}
    public record CreateEmployee(UUID phongBanId, String maNv, String hoTen,
            LocalDate ngaySinh, LocalDate ngayVaoLam, String soDT, Boolean gioiTinh,
            String queQuan, String trangThai, String viTri) {}
    public record UpdateWorkDays(JsonNode soNgayDaLam, JsonNode soNgayChuan) {}
    public record Department(UUID id, String tenPhongBan, String maTruongPhong, int soNhanVien) {}
    public record Employee(UUID id, UUID phongBanId, String maNv, String hoTen,
            LocalDate ngaySinh, LocalDate ngayVaoLam, String soDT, Boolean gioiTinh,
            String queQuan, String trangThai, String viTri, WorkDays ngayCong) {}
    /** Each numeric value is a finite JSON number or "NaN"/"Infinity"/"-Infinity". */
    public record WorkDays(Object soNgayDaLam, Object soNgayChuan, Object phanTramHoanThanh,
            String phanLoai) {}
    public record ResetResult(int soPhongBan, int soNhanVien) {}
    public record ErrorResponse(int status, String code, String message, String path) {}
}
