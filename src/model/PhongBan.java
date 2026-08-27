package model;

import java.util.ArrayList;

public class PhongBan {
    private String tenPB;
    private String idTruongPhong;
    private ArrayList<NhanVien> nhanVien;

    public PhongBan() {
        this.nhanVien = new ArrayList<>();
    }

    public PhongBan(String tenPB, String idTruongPhong,
            ArrayList<NhanVien> nhanVien) {
        this.tenPB = tenPB;
        this.idTruongPhong = idTruongPhong;
        this.nhanVien = nhanVien;
    }

    public String getTenPB(){
        return tenPB;
    }
    public void setTenPB(String tenPB){
        this.tenPB = tenPB;
    }

    public String getIdTruongPhong(){
        return idTruongPhong;
    }

    public void setIdTruongPhong(String idTruongPhong){
        this.idTruongPhong = idTruongPhong;
    }

    public ArrayList<NhanVien> getNhanVien(){
        return nhanVien;
    }

    public void setNhanVien(ArrayList<NhanVien> nhanVien){
        this.nhanVien = nhanVien;
    }
    
}
