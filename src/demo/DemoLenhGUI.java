package demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import model.NgayCong;
import model.NhanVien;
import model.PhongBan;

public class DemoLenhGUI {
    private final ArrayList<PhongBan> danhSachPhongBan = new ArrayList<>();
    private final SimpleDateFormat dinhDangNgay = new SimpleDateFormat("dd/MM/yyyy");

    public DemoLenhGUI() {
        dinhDangNgay.setLenient(false);
    }

    public static void main(String[] args) throws ParseException {
        DemoLenhGUI demo = new DemoLenhGUI();
        demo.napDuLieuMau();
        demo.hienThiDuLieu();

        PhongBan phongBanMoi = demo.themPhongBan("Ke toan", "NV004");
        NhanVien nhanVienMoi = demo.themNhanVien(phongBanMoi, "NV004", "Pham Thi Dung",
                "10/10/1998", "0901000004", false, "Dang lam", "Nhan vien");
        demo.tinhNgayCong(nhanVienMoi, 18, 26);
        demo.suaNhanVien(nhanVienMoi, "Pham Thi Dung", "Ke toan vien");
        demo.suaPhongBan(phongBanMoi, "Tai chinh - Ke toan", "NV004");
        demo.hienThiDuLieu();

        demo.resetNgayCongPhongBan(phongBanMoi);
        demo.xoaNhanVien(phongBanMoi, nhanVienMoi);
        demo.xoaPhongBan(phongBanMoi);
        demo.hienThiDuLieu();
    }

    public void napDuLieuMau() throws ParseException {
        danhSachPhongBan.clear();
        PhongBan kinhDoanh = themPhongBan("Kinh doanh", "NV001");
        NhanVien truongPhong = themNhanVien(kinhDoanh, "NV001", "Nguyen Van An",
                "12/03/1985", "0901000001", true, "Dang lam", "Truong phong");
        tinhNgayCong(truongPhong, 22, 26);

        NhanVien nhanVien = themNhanVien(kinhDoanh, "NV002", "Tran Thi Binh",
                "25/07/1995", "0901000002", false, "Dang lam", "Nhan vien");
        tinhNgayCong(nhanVien, 20, 26);
    }

    public PhongBan themPhongBan(String tenPhongBan, String maTruongPhong) {
        PhongBan phongBan = new PhongBan(tenPhongBan, maTruongPhong, new ArrayList<>());
        danhSachPhongBan.add(phongBan);
        System.out.println("Them phong ban: " + tenPhongBan);
        return phongBan;
    }

    public NhanVien themNhanVien(PhongBan phongBan, String maNhanVien, String hoTen,
            String ngaySinh, String soDienThoai, Boolean gioiTinh, String trangThai,
            String viTri) throws ParseException {
        NhanVien nhanVien = new NhanVien(hoTen, dinhDangNgay.parse(ngaySinh), soDienThoai,
                gioiTinh, maNhanVien, trangThai, viTri, "Hà Nội");
        nhanVien.setNgayCong(new NgayCong(nhanVien, 0, 26));
        phongBan.getNhanVien().add(nhanVien);
        System.out.println("  Them nhan vien: " + hoTen);
        return nhanVien;
    }

    public void suaNhanVien(NhanVien nhanVien, String hoTenMoi, String viTriMoi) {
        nhanVien.setFullName(hoTenMoi);
        nhanVien.setViTri(viTriMoi);
        System.out.println("  Sua nhan vien: " + hoTenMoi);
    }

    public void xoaNhanVien(PhongBan phongBan, NhanVien nhanVien) {
        phongBan.getNhanVien().remove(nhanVien);
        System.out.println("  Xoa nhan vien: " + nhanVien.getFullName());
    }

    public void suaPhongBan(PhongBan phongBan, String tenMoi, String maTruongPhongMoi) {
        phongBan.setTenPB(tenMoi);
        phongBan.setIdTruongPhong(maTruongPhongMoi);
        System.out.println("Sua phong ban: " + tenMoi);
    }

    public void xoaPhongBan(PhongBan phongBan) {
        danhSachPhongBan.remove(phongBan);
        System.out.println("Xoa phong ban: " + phongBan.getTenPB());
    }

    public void tinhNgayCong(NhanVien nhanVien, double soNgayDaLam, double soNgayChuan) {
        NgayCong ngayCong = new NgayCong(nhanVien, soNgayDaLam, soNgayChuan);
        nhanVien.setNgayCong(ngayCong);
        System.out.printf("  Tinh ngay cong %s: %.1f/%.1f = %.1f%%%n",
                nhanVien.getFullName(), soNgayDaLam, soNgayChuan,
                ngayCong.tinhPhanTramHoanThanh());
    }

    public void resetNgayCongPhongBan(PhongBan phongBan) {
        for (NhanVien nhanVien : phongBan.getNhanVien()) {
            nhanVien.setNgayCong(new NgayCong(nhanVien, 0, 26));
        }
        System.out.println("Reset ngay cong phong ban: " + phongBan.getTenPB());
    }

    public void hienThiDuLieu() {
        System.out.println("\nDANH SACH DU LIEU");
        for (PhongBan phongBan : danhSachPhongBan) {
            System.out.println("Phong ban: " + phongBan.getTenPB()
                    + " | Truong phong: " + phongBan.getIdTruongPhong());
            for (NhanVien nhanVien : phongBan.getNhanVien()) {
                NgayCong ngayCong = nhanVien.getNgayCong();
                System.out.printf("  - %s | %s | Ngay cong: %.1f/%.1f (%.1f%%)%n",
                        nhanVien.getId(), nhanVien.getFullName(),
                        ngayCong.getSoNgayDaLam(), ngayCong.getSoNgayChuan(),
                        ngayCong.tinhPhanTramHoanThanh());
            }
        }
    }
}
