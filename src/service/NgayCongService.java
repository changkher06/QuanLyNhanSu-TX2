package service;

import java.math.BigDecimal;
import java.util.Objects;
import model.NgayCong;
import model.NhanVien;
import model.PhongBan;
import repository.NhanSuRepository;

public final class NgayCongService {
    private final NhanSuRepository repository;

    public NgayCongService(NhanSuRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public NgayCong capNhatNgayCong(NhanVien nhanVien, String daLam, String chuan) {
        if (daLam == null || chuan == null) {
            throw new IllegalArgumentException("Ngày công và ngày chuẩn phải là số hợp lệ.");
        }
        try {
            return capNhatNgayCong(nhanVien, Double.parseDouble(daLam.trim()), Double.parseDouble(chuan.trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Ngày công và ngày chuẩn phải là số hợp lệ.", exception);
        }
    }

    public NgayCong capNhatNgayCong(NhanVien nhanVien, double daLam, double chuan) {
        // Ba điều kiện nguyên bản trong GUI.calculateNgayCong ở 3312331.
        // Không thêm isFinite hoặc giới hạn số chữ số thập phân.
        if (daLam < 0 || chuan <= 0 || daLam > chuan) {
            throw new IllegalArgumentException("Số ngày công không hợp lệ.");
        }
        synchronized (repository) {
            repository.kiemTraNhanVien(nhanVien);
            NgayCong ngayCong = new NgayCong(nhanVien, daLam, chuan);
            nhanVien.setNgayCong(ngayCong);
            return ngayCong;
        }
    }

    /** Adapter tương thích giai đoạn 1; nghiệp vụ vẫn dùng double như TX1. */
    public NgayCong capNhatNgayCong(NhanVien nhanVien, BigDecimal daLam, BigDecimal chuan) {
        if (daLam == null || chuan == null) throw new IllegalArgumentException("Ngày công và ngày chuẩn phải là số hợp lệ.");
        return capNhatNgayCong(nhanVien, daLam.doubleValue(), chuan.doubleValue());
    }

    public void resetPhongBan(PhongBan phongBan) {
        synchronized (repository) {
            for (NhanVien nv : repository.danhSachNhanVien(phongBan)) {
                nv.setNgayCong(new NgayCong(nv, 0, 26));
            }
        }
    }

}
