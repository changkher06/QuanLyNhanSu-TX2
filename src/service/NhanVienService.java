package service;

import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import model.NgayCong;
import model.NhanVien;
import model.PhongBan;
import repository.NhanSuRepository;

public final class NhanVienService {
    private final NhanSuRepository repository;

    public NhanVienService(NhanSuRepository repository) {
        this(repository, ZoneId.systemDefault());
    }

    public NhanVienService(NhanSuRepository repository, ZoneId zone) {
        this.repository = Objects.requireNonNull(repository);
        // Giữ chữ ký constructor đã có; TX1 so sánh Date.before, không đổi múi giờ.
        Objects.requireNonNull(zone);
    }

    public NhanSuRepository getRepository() {
        return repository;
    }

    public NhanVien themNhanVien(PhongBan phongBan, NhanVien nhanVien) {
        synchronized (repository) {
            kiemTraThongTin(nhanVien);
            repository.themNhanVien(phongBan, nhanVien);
            nhanVien.setID(nhanVien.getId().trim());
            nhanVien.setFullName(nhanVien.getFullName().trim());
            nhanVien.setNgayCong(new NgayCong(nhanVien, 0, 26));
            return nhanVien;
        }
    }

    public void doiMaNhanVien(NhanVien nhanVien, String maMoi) {
        repository.doiMaNhanVien(nhanVien, maMoi);
    }

    /** Giữ cả thứ tự ghi TX1: đổi mã trước khi kiểm tra ngày; không sửa lịch sử. */
    public void capNhatThongTin(NhanVien nhanVien, NhanVien input) {
        synchronized (repository) {
            repository.kiemTraNhanVien(nhanVien);
            kiemTraMaTen(input);
            repository.doiMaNhanVien(nhanVien, input.getId().trim());
            Date birth = input.getBirthDate();
            Date hire = input.getNgayVaoLam();
            if (birth == null || hire == null || hire.before(birth)) {
                throw new IllegalArgumentException("Ngày sinh và ngày vào làm không hợp lệ.");
            }
            nhanVien.setFullName(input.getFullName().trim());
            nhanVien.setNgayVaoLam(hire);
            nhanVien.setBirthDate(birth);
            nhanVien.setSoDT(input.getSoDT());
            nhanVien.setQueQuan(input.getQueQuan());
            nhanVien.setGioiTinh(input.getGioiTinh());
            nhanVien.setTrangThai(input.getTrangThai());
            nhanVien.setViTri(input.getViTri());
        }
    }

    public void capNhatNgaySinh(NhanVien nhanVien, Date ngaySinh) {
        synchronized (repository) {
            repository.kiemTraNhanVien(nhanVien);
            // Inline/quick edit TX1 chỉ parse ngày ở view rồi gán setter.
            nhanVien.setBirthDate(ngaySinh);
        }
    }

    public void capNhatNgayVaoLam(NhanVien nhanVien, Date ngayVaoLam) {
        synchronized (repository) {
            repository.kiemTraNhanVien(nhanVien);
            nhanVien.setNgayVaoLam(ngayVaoLam);
        }
    }

    public void xoaNhanVien(PhongBan phongBan, NhanVien nhanVien) {
        repository.xoaNhanVien(phongBan, nhanVien);
    }

    private void kiemTraThongTin(NhanVien nhanVien) {
        kiemTraMaTen(nhanVien);
        kiemTraNgay(nhanVien.getBirthDate(), nhanVien.getNgayVaoLam());
    }

    private void kiemTraMaTen(NhanVien nhanVien) {
        if (nhanVien == null || nhanVien.getId() == null || nhanVien.getId().trim().isEmpty()
                || nhanVien.getFullName() == null || nhanVien.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã nhân viên và họ tên không được để trống.");
        }
    }

    private void kiemTraNgay(Date birth, Date hire) {
        if (birth == null) throw new IllegalArgumentException("Ngày sinh phải có định dạng dd/MM/yyyy.");
        if (hire == null) throw new IllegalArgumentException("Ngày vào làm phải có định dạng dd/MM/yyyy.");
        if (hire.before(birth)) {
            throw new IllegalArgumentException("Ngày vào làm không được trước ngày sinh.");
        }
    }
}
