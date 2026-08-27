package model;

public class TrinhDo {
    private String trinhDo;
    private String ngayDo;

    public TrinhDo() {
        this(null, null);
    }

    public TrinhDo(String trinhDo, String ngayDo) {
        this.trinhDo = trinhDo;
        this.ngayDo = ngayDo;
    }

    public String getTrinhDo() {
        return trinhDo;
    }

    public void setTrinhDo(String trinhDo) {
        this.trinhDo = trinhDo;
    }

    public String getNgayDo() {
        return ngayDo;
    }

    public void setNgayDo(String ngayDo) {
        this.ngayDo = ngayDo;
    }
}
