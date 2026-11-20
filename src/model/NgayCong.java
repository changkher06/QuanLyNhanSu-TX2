package model;

import java.math.BigDecimal;

public class NgayCong {
    private NhanVien nhanVien;
    private double soNgayDaLam;
    private double soNgayChuan;

    public NgayCong() {
    }

    public NgayCong(NhanVien nhanVien, double soNgayDaLam, double soNgayChuan) {
        this.nhanVien = nhanVien;
        this.soNgayDaLam = soNgayDaLam;
        this.soNgayChuan = soNgayChuan;
    }

    /** Adapter giai đoạn 1, vẫn chuyển sang double trước khi tính như TX1. */
    public NgayCong(NhanVien nhanVien, BigDecimal soNgayDaLam, BigDecimal soNgayChuan) {
        this(nhanVien, soNgayDaLam.doubleValue(), soNgayChuan.doubleValue());
    }

    public double tinhPhanTramHoanThanh() {
        if (soNgayChuan <= 0) {
            return 0;
        }
        return soNgayDaLam / soNgayChuan * 100;
    }

    public String getPhanLoai() {
        double tyLe = tinhPhanTramHoanThanh();
        if (tyLe >= 100) return "Xuất sắc";
        if (tyLe >= 90) return "Giỏi";
        if (tyLe >= 75) return "Khá";
        if (tyLe >= 50) return "Trung bình";
        return "Kém";
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public double getSoNgayDaLam() {
        return soNgayDaLam;
    }

    public void setSoNgayDaLam(double soNgayDaLam) {
        this.soNgayDaLam = soNgayDaLam;
    }

    public double getSoNgayChuan() {
        return soNgayChuan;
    }

    public void setSoNgayChuan(double soNgayChuan) {
        this.soNgayChuan = soNgayChuan;
    }

    /** Adapter hữu hạn, không phải dữ liệu BigDecimal gốc và không dùng để phân loại. */
    public BigDecimal getSoNgayDaLamChinhXac() {
        return BigDecimal.valueOf(soNgayDaLam);
    }

    public BigDecimal getSoNgayChuanChinhXac() {
        return BigDecimal.valueOf(soNgayChuan);
    }
}
