package service;

import java.time.Clock;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import model.NhanVien;
import model.PhongBan;
import repository.NhanSuRepository;

/** Logic áp dụng và lọc lại chuyển từ TimKiemNhanVienDialog ở commit 3312331. */
public final class TimKiemNhanVienService {
    private final NhanSuRepository repository;
    private final Clock clock;

    public TimKiemNhanVienService(NhanSuRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    public TimKiemNhanVienService(NhanSuRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<NhanVien> tatCa(PhongBan phongBan) {
        return repository.danhSachNhanVien(phongBan);
    }

    public List<NhanVien> timKiem(PhongBan phongBan, String tieuChi, String giaTri) {
        synchronized (repository) {
            List<NhanVien> employees = repository.danhSachNhanVien(phongBan);
            AtomicReference<List<NhanVien>> result = new AtomicReference<>(List.of());
            apDungBoLoc(tieuChi, giaTri,
                    filter -> result.set(employees.stream().filter(filter).toList()),
                    message -> { throw new IllegalArgumentException(message); });
            return result.get();
        }
    }

    /** Adapter tạo predicate đã có từ giai đoạn 1; timKiem áp dụng đầy đủ fallback TX1. */
    public Predicate<NhanVien> taoBoLoc(String tieuChi, String giaTri) {
        AtomicReference<Predicate<NhanVien>> result = new AtomicReference<>();
        apDungBoLoc(tieuChi, giaTri, result::set,
                message -> { throw new IllegalArgumentException(message); });
        return result.get();
    }

    public void apDungBoLoc(String criterion, String input,
            Consumer<Predicate<NhanVien>> refreshTable, Consumer<String> showError) {
        if ("Giới tính".equals(criterion)) {
            boolean male = "Nam".equals(input);
            refreshTable.accept(employee -> Boolean.TRUE.equals(employee.getGioiTinh()) == male);
            return;
        }
        String value = input.trim();
        if (value.isEmpty()) {
            showError.accept("Vui lòng nhập giá trị tìm kiếm.");
            return;
        }
        
        if ("Tuổi".equals(criterion) || "Thâm niên".equals(criterion) || "Mã nhân viên".equals(criterion)) {
            if (value.contains("-")) {
                try {
                    String[] parts = value.split("-");
                    if (parts.length == 2) {
                        int min = Integer.parseInt(parts[0].trim());
                        int max = Integer.parseInt(parts[1].trim());
                        if (min >= 0 && max >= 0 && min <= max) {
                            if ("Tuổi".equals(criterion)) {
                                refreshTable.accept(employee -> {
                                    int age = calculateAge(employee.getBirthDate());
                                    return age >= min && age <= max;
                                });
                            } else if ("Thâm niên".equals(criterion)) {
                                refreshTable.accept(employee -> {
                                    int seniority = calculateSeniority(employee.getNgayVaoLam());
                                    return seniority >= min && seniority <= max;
                                });
                            } else if ("Mã nhân viên".equals(criterion)) {
                                refreshTable.accept(employee -> {
                                    int id = extractNumber(employee.getId());
                                    return id != -1 && id >= min && id <= max;
                                });
                            }
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Fall through to exact match or string match
                }
            }
            
            try {
                int number = Integer.parseInt(value);
                if (number >= 0) {
                    if ("Tuổi".equals(criterion)) {
                        refreshTable.accept(employee -> calculateAge(employee.getBirthDate()) == number);
                        return;
                    } else if ("Thâm niên".equals(criterion)) {
                        refreshTable.accept(employee -> calculateSeniority(employee.getNgayVaoLam()) == number);
                        return;
                    } else if ("Mã nhân viên".equals(criterion)) {
                        refreshTable.accept(employee -> {
                            int id = extractNumber(employee.getId());
                            return id == number;
                        });
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                if (!"Mã nhân viên".equals(criterion)) {
                    showError.accept("Giá trị tìm kiếm số phải là số nguyên không âm hoặc khoảng hợp lệ (VD: 10-20).");
                    return;
                }
            }
        }
        
        String keyword = value.toLowerCase();
        refreshTable.accept(employee -> getTextValue(employee, criterion).toLowerCase().contains(keyword));
    }

    public boolean khopBoLocHienTai(NhanVien employee, String criterion, String input) {
        if ("Giới tính".equals(criterion)) {
            return Boolean.TRUE.equals(employee.getGioiTinh()) == "Nam".equals(input);
        }
        String value = input.trim().toLowerCase();
        if (value.isEmpty()) {
            return true;
        }
        
        if ("Tuổi".equals(criterion) || "Thâm niên".equals(criterion) || "Mã nhân viên".equals(criterion)) {
            if (value.contains("-")) {
                try {
                    String[] parts = value.split("-");
                    if (parts.length == 2) {
                        int min = Integer.parseInt(parts[0].trim());
                        int max = Integer.parseInt(parts[1].trim());
                        if (min >= 0 && max >= 0 && min <= max) {
                            if ("Tuổi".equals(criterion)) {
                                int age = calculateAge(employee.getBirthDate());
                                return age >= min && age <= max;
                            } else if ("Thâm niên".equals(criterion)) {
                                int seniority = calculateSeniority(employee.getNgayVaoLam());
                                return seniority >= min && seniority <= max;
                            } else if ("Mã nhân viên".equals(criterion)) {
                                int idNum = extractNumber(employee.getId());
                                return idNum != -1 && idNum >= min && idNum <= max;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Fall through
                }
            }
            try {
                int number = Integer.parseInt(value);
                if (number >= 0) {
                    if ("Tuổi".equals(criterion)) {
                        return calculateAge(employee.getBirthDate()) == number;
                    } else if ("Thâm niên".equals(criterion)) {
                        return calculateSeniority(employee.getNgayVaoLam()) == number;
                    } else if ("Mã nhân viên".equals(criterion)) {
                        int idNum = extractNumber(employee.getId());
                        return idNum == number;
                    }
                }
            } catch (NumberFormatException e) {
                if (!"Mã nhân viên".equals(criterion)) {
                    return false;
                }
            }
        }
        
        return getTextValue(employee, criterion).toLowerCase().contains(value);
    }

    private int extractNumber(String text) {
        if (text == null) return -1;
        String num = text.replaceAll("[^0-9]", "");
        return num.isEmpty() ? -1 : Integer.parseInt(num);
    }

    private String getTextValue(NhanVien employee, String criterion) {
        if ("Họ tên".equals(criterion)) {
            return value(employee.getFullName());
        }
        if ("Mã nhân viên".equals(criterion)) {
            return value(employee.getId());
        }
        if ("Trạng thái".equals(criterion)) {
            return value(employee.getTrangThai());
        }
        return value(employee.getViTri());
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private int calculateAge(Date birthDate) {
        return calculateYearsSince(birthDate);
    }

    private int calculateSeniority(Date hireDate) {
        return calculateYearsSince(hireDate);
    }

    private int calculateYearsSince(Date date) {
        if (date == null) {
            return -1;
        }
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone(clock.getZone()));
        today.setTimeInMillis(clock.millis());
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone(clock.getZone()));
        start.setTime(date);
        int years = today.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < start.get(Calendar.DAY_OF_YEAR)) {
            years--;
        }
        return years;
    }
}
