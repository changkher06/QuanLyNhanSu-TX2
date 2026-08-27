package model;

import java.util.Date;

public class ChucVu {
    private String tenChucVu;
    private Date ngayNhan;
    private Date ngayKetthuc;

    public ChucVu() {
        this(null, null, null);
    }

    public ChucVu(String tenChucVu, Date ngayNhan, Date ngayKetThuc){
        this.tenChucVu = tenChucVu;
        this.ngayNhan = ngayNhan;
        this.ngayKetthuc = ngayKetThuc; 
    }

    public String getTenChucVu() {
        return tenChucVu;
    }

    public void setTenChucVu(String tenChucVu) {
        this.tenChucVu = tenChucVu;
    }

    public Date getNgayNhan() {
        return ngayNhan;
    }

    public void setNgayNhan(Date ngayNhan) {
        this.ngayNhan = ngayNhan;
    }

    public Date getNgayKetthuc() {
        return ngayKetthuc;
    }

    public void setNgayKetthuc(Date ngayKetthuc) {
        this.ngayKetthuc = ngayKetthuc;
    }

}
