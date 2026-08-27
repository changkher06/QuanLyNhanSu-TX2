package model;

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

    public double tinhPhanTramHoanThanh() {
        if (soNgayChuan <= 0) {
            return 0;
        }
        return soNgayDaLam / soNgayChuan * 100;
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
}
