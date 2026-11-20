package repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import model.NhanVien;
import model.PhongBan;

/**
 * Kho dữ liệu dùng chung, không phụ thuộc Swing. Danh sách trả về là snapshot
 * không sửa được; các phần tử vẫn là model cũ để giữ tương thích GUI TX1.
 * Service đồng bộ trên instance repository khi cần nhiều thao tác nguyên tử.
 */
public interface NhanSuRepository {
    List<PhongBan> danhSachPhongBan();
    List<NhanVien> danhSachNhanVien(PhongBan phongBan);
    List<NhanVien> tatCaNhanVien();
    /** Tra cứu tương thích: chỉ trả kết quả đầu tiên, không dùng mã để định danh bản ghi. */
    Optional<NhanVien> timTheoMa(String ma);
    List<NhanVien> timTatCaTheoMa(String ma);
    UUID idNhanVien(NhanVien nhanVien);
    UUID idPhongBan(PhongBan phongBan);
    Optional<NhanVien> timNhanVienTheoId(UUID id);
    Optional<PhongBan> timPhongBanTheoId(UUID id);
    void themPhongBan(PhongBan phongBan);
    void thayThePhongBan(Collection<PhongBan> phongBans);
    void xoaPhongBan(PhongBan phongBan);
    void themNhanVien(PhongBan phongBan, NhanVien nhanVien);
    void doiMaNhanVien(NhanVien nhanVien, String maMoi);
    void xoaNhanVien(PhongBan phongBan, NhanVien nhanVien);
    void kiemTraNhanVien(NhanVien nhanVien);
}
