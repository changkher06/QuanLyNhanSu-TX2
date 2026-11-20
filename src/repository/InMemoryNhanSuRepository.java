package repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import model.NhanVien;
import model.PhongBan;

/** Một cây PhongBan/NhanVien duy nhất; không duy trì bản sao nhân viên riêng. */
public final class InMemoryNhanSuRepository implements NhanSuRepository {
    private final List<PhongBan> phongBans = new ArrayList<>();
    private final Map<NhanVien, UUID> nhanVienIds = new IdentityHashMap<>();
    private final Map<PhongBan, UUID> phongBanIds = new IdentityHashMap<>();

    @Override
    public synchronized List<PhongBan> danhSachPhongBan() {
        return List.copyOf(phongBans);
    }

    @Override
    public synchronized List<NhanVien> danhSachNhanVien(PhongBan phongBan) {
        kiemTraPhongBan(phongBan);
        return List.copyOf(phongBan.getNhanVien());
    }

    @Override
    public synchronized List<NhanVien> tatCaNhanVien() {
        return phongBans.stream().flatMap(pb -> pb.getNhanVien().stream()).toList();
    }

    @Override
    public synchronized Optional<NhanVien> timTheoMa(String ma) {
        // Giữ tiện ích đọc đã có ở giai đoạn 1; không suy ra tính duy nhất của mã.
        String key = ma == null ? null : ma.trim().toUpperCase(Locale.ROOT);
        return tatCaNhanVien().stream().filter(nv -> Objects.equals(key,
                nv.getId() == null ? null : nv.getId().trim().toUpperCase(Locale.ROOT))).findFirst();
    }

    @Override
    public synchronized List<NhanVien> timTatCaTheoMa(String ma) {
        return tatCaNhanVien().stream().filter(nv -> Objects.equals(ma, nv.getId())).toList();
    }

    @Override
    public synchronized UUID idNhanVien(NhanVien nhanVien) {
        kiemTraNhanVien(nhanVien);
        dongBoDinhDanh();
        return nhanVienIds.get(nhanVien);
    }

    @Override
    public synchronized UUID idPhongBan(PhongBan phongBan) {
        kiemTraPhongBan(phongBan);
        dongBoDinhDanh();
        return phongBanIds.get(phongBan);
    }

    @Override
    public synchronized Optional<NhanVien> timNhanVienTheoId(UUID id) {
        dongBoDinhDanh();
        return nhanVienIds.entrySet().stream().filter(e -> e.getValue().equals(id)).map(Map.Entry::getKey).findFirst();
    }

    @Override
    public synchronized Optional<PhongBan> timPhongBanTheoId(UUID id) {
        dongBoDinhDanh();
        return phongBanIds.entrySet().stream().filter(e -> e.getValue().equals(id)).map(Map.Entry::getKey).findFirst();
    }

    @Override
    public synchronized void themPhongBan(PhongBan phongBan) {
        List<PhongBan> next = new ArrayList<>(phongBans);
        next.add(phongBan);
        thayThePhongBan(next);
    }

    @Override
    public synchronized void thayThePhongBan(Collection<PhongBan> next) {
        if (next == null) throw new IllegalArgumentException("Danh sách phòng ban không được null.");
        List<PhongBan> checked = new ArrayList<>(next);
        for (PhongBan pb : checked) {
            if (pb == null || pb.getNhanVien() == null) {
                throw new IllegalArgumentException("Phòng ban không hợp lệ.");
            }
            for (NhanVien nv : pb.getNhanVien()) {
                if (nv == null) throw new IllegalArgumentException("Nhân viên không được null.");
            }
        }
        // Chỉ thay dữ liệu sau khi toàn bộ danh sách mới hợp lệ.
        phongBans.clear();
        phongBans.addAll(checked);
        dongBoDinhDanh();
    }

    @Override
    public synchronized void xoaPhongBan(PhongBan phongBan) {
        kiemTraPhongBan(phongBan);
        phongBans.remove(phongBan);
        dongBoDinhDanh();
    }

    @Override
    public synchronized void themNhanVien(PhongBan phongBan, NhanVien nhanVien) {
        kiemTraPhongBan(phongBan);
        if (nhanVien == null) throw new IllegalArgumentException("Nhân viên không được null.");
        phongBan.getNhanVien().add(nhanVien);
        dongBoDinhDanh();
    }

    @Override
    public synchronized void doiMaNhanVien(NhanVien nhanVien, String maMoi) {
        kiemTraNhanVien(nhanVien);
        // TX1 gán trực tiếp; không kiểm tra mã trùng, không đổi mã trưởng phòng.
        nhanVien.setID(maMoi);
    }

    @Override
    public synchronized void xoaNhanVien(PhongBan phongBan, NhanVien nhanVien) {
        kiemTraPhongBan(phongBan);
        if (!phongBan.getNhanVien().remove(nhanVien)) {
            throw new IllegalArgumentException("Nhân viên không thuộc phòng ban.");
        }
        dongBoDinhDanh();
    }

    @Override
    public synchronized void kiemTraNhanVien(NhanVien nhanVien) {
        if (nhanVien == null || tatCaNhanVien().stream().noneMatch(nv -> nv == nhanVien)) {
            throw new IllegalArgumentException("Không tìm thấy nhân viên trong công ty.");
        }
    }

    private void kiemTraPhongBan(PhongBan phongBan) {
        if (phongBan == null || !phongBans.contains(phongBan)) {
            throw new IllegalArgumentException("Không tìm thấy phòng ban.");
        }
    }

    private void dongBoDinhDanh() {
        Set<PhongBan> activePhongBans = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<NhanVien> activeNhanViens = Collections.newSetFromMap(new IdentityHashMap<>());
        for (PhongBan pb : phongBans) {
            activePhongBans.add(pb);
            phongBanIds.computeIfAbsent(pb, key -> idMoi());
            for (NhanVien nv : pb.getNhanVien()) {
                activeNhanViens.add(nv);
                nhanVienIds.computeIfAbsent(nv, key -> idMoi());
            }
        }
        phongBanIds.keySet().retainAll(activePhongBans);
        nhanVienIds.keySet().retainAll(activeNhanViens);
    }

    private UUID idMoi() {
        UUID id;
        do { id = UUID.randomUUID(); }
        while (idDaDung(id));
        return id;
    }

    private boolean idDaDung(UUID id) {
        return phongBanIds.values().stream().anyMatch(id::equals)
                || nhanVienIds.values().stream().anyMatch(id::equals);
    }
}
