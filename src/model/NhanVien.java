package model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class NhanVien extends Person {
    private String trangThai;
    private String viTri;
    private Date ngayVaoLam;
    private NgayCong ngayCong;
    private final List<TrinhDo> trinhDos = new ArrayList<>();
    private final List<ChucVu> lichSuChucVu = new ArrayList<>();

    public NhanVien() {
        super();
    }

    public NhanVien(String fullName, java.util.Date birthDate, String soDT,
            Boolean gioiTinh, String id, String trangThai, String viTri, String queQuan) {
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.soDT = soDT;
        this.gioiTinh = gioiTinh;
        this.id = id;
        this.trangThai = trangThai;
        this.viTri = viTri;
        this.queQuan = queQuan;
    }

    public NhanVien(String fullName, java.util.Date birthDate, String soDT,
            Boolean gioiTinh, String id, String trangThai, String viTri, String queQuan,
            NgayCong ngayCong) {
        this(fullName, birthDate, soDT, gioiTinh, id, trangThai, viTri, queQuan);
        setNgayCong(ngayCong);
    }

    public NhanVien(String fullName, java.util.Date birthDate, String soDT,
            Boolean gioiTinh, String id, String trangThai, String viTri, String queQuan,
            Date ngayVaoLam, NgayCong ngayCong) {
        this(fullName, birthDate, soDT, gioiTinh, id, trangThai, viTri, queQuan, ngayCong);
        this.ngayVaoLam = ngayVaoLam;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public String getViTri() {
        return viTri;
    }

    public void setViTri(String viTri) {
        this.viTri = viTri;
    }

    public Date getNgayVaoLam() {
        return ngayVaoLam;
    }

    public void setNgayVaoLam(Date ngayVaoLam) {
        this.ngayVaoLam = ngayVaoLam;
    }

    public NgayCong getNgayCong() {
        return ngayCong;
    }

    public void setNgayCong(NgayCong ngayCong) {
        this.ngayCong = ngayCong;
        if (ngayCong != null && ngayCong.getNhanVien() != this) {
            ngayCong.setNhanVien(this);
        }
    }

    public List<TrinhDo> getTrinhDos() {
        return trinhDos;
    }

    public List<ChucVu> getLichSuChucVu() {
        return lichSuChucVu;
    }

    public void capNhatViTriTheoChucVuHienTai() {
        Date homNay = boGio(new Date());
        ChucVu chucVuGanNhat = null;
        for (ChucVu chucVu : lichSuChucVu) {
            Date ngayNhan = chucVu.getNgayNhan();
            Date ngayKetThuc = chucVu.getNgayKetthuc();
            if (ngayNhan == null || boGio(ngayNhan).after(homNay)
                    || (ngayKetThuc != null && boGio(ngayKetThuc).before(homNay))) {
                continue;
            }
            if (chucVuGanNhat == null || ngayNhan.after(chucVuGanNhat.getNgayNhan())) {
                chucVuGanNhat = chucVu;
            }
        }
        if (chucVuGanNhat != null) {
            setViTri(chucVuGanNhat.getTenChucVu());
        }
    }

    private Date boGio(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
