package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import model.NgayCong;
import model.NhanVien;
import model.PhongBan;

public class TimKiemNhanVienDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Color BACKGROUND = new Color(245, 247, 246);
    private static final Color BORDER = new Color(207, 216, 220);
    private static final Font APP_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final String[] CRITERIA = { "Họ tên", "Mã nhân viên", "Trạng thái", "Vị trí",
            "Tuổi", "Giới tính", "Thâm niên" };

    private final PhongBan phongBan;
    private final SimpleDateFormat dateFormat;
    private final Runnable onChanged;
    private final JComboBox<String> criterionBox = new JComboBox<>(CRITERIA);
    private final JTextField valueField = new JTextField();
    private final JComboBox<String> genderBox = new JComboBox<>(new String[] { "Nam", "Nữ" });
    private final JLabel valueLabel = new JLabel("Giá trị:");
    private final CardLayout filterValueLayout = new CardLayout();
    private final JPanel filterValuePanel = new JPanel(filterValueLayout);
    private final JLabel ketQuaLocLabel = new JLabel("Số lượng đã lọc: 0");
    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[] {
            "Mã NV", "Họ tên", "Ngày sinh", "Ngày vào làm", "Số điện thoại", "Giới tính", "Trạng thái", "Vị trí", "Ngày công"
    }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return column >= 0 && column <= 6;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (row < 0 || row >= displayedEmployees.size() || aValue == null) return;
            NhanVien nv = displayedEmployees.get(row);
            String val = aValue.toString().trim();
            boolean changed = false;
            switch (column) {
                case 0: nv.setID(val); changed = true; break;
                case 1: nv.setFullName(val); changed = true; break;
                case 2:
                    try {
                        nv.setBirthDate(dateFormat.parse(val));
                        changed = true;
                    } catch (java.text.ParseException e) {
                        showError("Ngày sinh không hợp lệ (dd/MM/yyyy).");
                        return;
                    }
                    break;
                case 3:
                    try {
                        nv.setNgayVaoLam(dateFormat.parse(val));
                        changed = true;
                    } catch (java.text.ParseException e) {
                        showError("Ngày vào làm không hợp lệ (dd/MM/yyyy).");
                        return;
                    }
                    break;
                case 4: nv.setSoDT(val); changed = true; break;
                case 5: nv.setGioiTinh("Nam".equals(val)); changed = true; break;
                case 6: nv.setTrangThai(val); changed = true; break;
            }
            if (changed) {
                super.setValueAt(aValue, row, column);
                if (onChanged != null) onChanged.run();
            }
        }
    };
    private final JTable table = new JTable(tableModel);
    private final List<NhanVien> displayedEmployees = new ArrayList<>();
    private int lastClickedCol = -1;
    private int lastClickedRow = -1;

    public TimKiemNhanVienDialog(Window owner, PhongBan phongBan, SimpleDateFormat dateFormat,
            String initialCriterion, Runnable onChanged) {
        super(owner, "Tìm kiếm nhân viên - " + phongBan.getTenPB(), ModalityType.MODELESS);
        this.phongBan = phongBan;
        this.dateFormat = dateFormat;
        this.onChanged = onChanged;
        criterionBox.setSelectedItem(initialCriterion);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1280, 650);
        setMinimumSize(new java.awt.Dimension(1050, 560));
        setLocationRelativeTo(owner);
        buildInterface();
        refreshTable(employee -> true);
    }

    private void buildInterface() {
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder("Bộ lọc nhân viên"));
        criterionBox.setPreferredSize(new java.awt.Dimension(185, 32));
        valueField.setPreferredSize(new java.awt.Dimension(280, 32));
        genderBox.setPreferredSize(new java.awt.Dimension(120, 32));
        filterValuePanel.add(valueField, "text");
        filterValuePanel.add(genderBox, "gender");
        filterValuePanel.setPreferredSize(new java.awt.Dimension(280, 32));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 6, 5, 6);
        constraints.anchor = GridBagConstraints.CENTER;
        filterPanel.add(new JLabel("Tìm theo:"), constraints);
        filterPanel.add(criterionBox, constraints);
        filterPanel.add(valueLabel, constraints);
        filterPanel.add(filterValuePanel, constraints);

        JButton applyButton = new JButton("Áp dụng");
        JButton allButton = new JButton("Hiển thị tất cả");
        applyButton.addActionListener(event -> applyFilter());
        valueField.addActionListener(event -> applyFilter());
        allButton.addActionListener(event -> {
            valueField.setText("");
            refreshTable(employee -> true);
        });
        filterPanel.add(applyButton, constraints);
        filterPanel.add(allButton, constraints);
        
        ketQuaLocLabel.setForeground(new java.awt.Color(0, 102, 204));
        ketQuaLocLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        filterPanel.add(ketQuaLocLabel, constraints);
        
        criterionBox.addActionListener(event -> updateFilterInput());
        updateFilterInput();

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(28);
        
        javax.swing.JComboBox<String> cbGioiTinh = new javax.swing.JComboBox<>(new String[]{"Nam", "Nữ"});
        table.getColumnModel().getColumn(5).setCellEditor(new javax.swing.DefaultCellEditor(cbGioiTinh));
        
        javax.swing.JComboBox<String> cbTrangThai = new javax.swing.JComboBox<>(new String[]{"Đang làm", "Nghỉ phép", "Nghỉ việc", "Thử việc"});
        table.getColumnModel().getColumn(6).setCellEditor(new javax.swing.DefaultCellEditor(cbTrangThai));
        
        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        java.util.Comparator<String> dateComparator = (s1, s2) -> {
            try {
                if (s1 == null || s1.isEmpty()) return s2 == null || s2.isEmpty() ? 0 : -1;
                if (s2 == null || s2.isEmpty()) return 1;
                java.util.Date d1 = dateFormat.parse(s1);
                java.util.Date d2 = dateFormat.parse(s2);
                return d1.compareTo(d2);
            } catch (Exception e) {
                return s1.compareTo(s2);
            }
        };
        sorter.setComparator(2, dateComparator);
        sorter.setComparator(3, dateComparator);

        javax.swing.JPopupMenu contextMenu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem quickEditItem = new javax.swing.JMenuItem("Chỉnh sửa nhanh ô này");
        javax.swing.JMenuItem bulkEditItem = new javax.swing.JMenuItem("Chỉnh sửa trạng thái hàng loạt");
        javax.swing.JMenuItem openProfileItem = new javax.swing.JMenuItem("Sửa hồ sơ");
        
        quickEditItem.addActionListener(event -> {
            if (lastClickedRow >= 0 && lastClickedCol >= 0) {
                quickEditCell(lastClickedRow, lastClickedCol);
            }
        });
        bulkEditItem.addActionListener(event -> bulkEditStatus());
        openProfileItem.addActionListener(event -> openProfile());
        
        contextMenu.add(quickEditItem);
        contextMenu.add(bulkEditItem);
        contextMenu.addSeparator();
        contextMenu.add(openProfileItem);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent event) {
                showContextMenu(event);
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent event) {
                showContextMenu(event);
            }
            private void showContextMenu(java.awt.event.MouseEvent event) {
                if (!event.isPopupTrigger()) {
                    return;
                }
                int row = table.rowAtPoint(event.getPoint());
                int col = table.columnAtPoint(event.getPoint());
                if (row >= 0) {
                    if (!table.isRowSelected(row)) {
                        table.setRowSelectionInterval(row, row);
                    }
                    lastClickedRow = row;
                    lastClickedCol = col;
                    contextMenu.show(table, event.getX(), event.getY());
                }
            }
        });

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setSelectionBackground(new Color(178, 223, 219));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 34));

        JButton profileButton = new JButton("Mở hồ sơ");
        JButton deleteButton = new JButton("Xóa nhân viên");
        JButton resetButton = new JButton("Reset ngày công phòng ban");
        JButton closeButton = new JButton("Đóng");
        profileButton.addActionListener(event -> openProfile());
        deleteButton.addActionListener(event -> deleteEmployee());
        resetButton.addActionListener(event -> resetWorkdays());
        closeButton.addActionListener(event -> dispose());
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 8));
        actions.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        actions.add(profileButton);
        actions.add(deleteButton);
        actions.add(resetButton);
        actions.add(closeButton);

        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        styleComponents(getContentPane());
    }

    private void updateFilterInput() {
        boolean gender = "Giới tính".equals(criterionBox.getSelectedItem());
        valueLabel.setText(gender ? "Giá trị:" : "Nhập giá trị:");
        filterValueLayout.show(filterValuePanel, gender ? "gender" : "text");
        valueField.setToolTipText("Số và Mã NV có thể nhập khoảng (VD: 10-20) hoặc số cụ thể");
    }

    private void applyFilter() {
        String criterion = (String) criterionBox.getSelectedItem();
        if ("Giới tính".equals(criterion)) {
            boolean male = "Nam".equals(genderBox.getSelectedItem());
            refreshTable(employee -> Boolean.TRUE.equals(employee.getGioiTinh()) == male);
            return;
        }
        String value = valueField.getText().trim();
        if (value.isEmpty()) {
            showError("Vui lòng nhập giá trị tìm kiếm.");
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
                                refreshTable(employee -> {
                                    int age = calculateAge(employee.getBirthDate());
                                    return age >= min && age <= max;
                                });
                            } else if ("Thâm niên".equals(criterion)) {
                                refreshTable(employee -> {
                                    int seniority = calculateSeniority(employee.getNgayVaoLam());
                                    return seniority >= min && seniority <= max;
                                });
                            } else if ("Mã nhân viên".equals(criterion)) {
                                refreshTable(employee -> {
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
                        refreshTable(employee -> calculateAge(employee.getBirthDate()) == number);
                        return;
                    } else if ("Thâm niên".equals(criterion)) {
                        refreshTable(employee -> calculateSeniority(employee.getNgayVaoLam()) == number);
                        return;
                    } else if ("Mã nhân viên".equals(criterion)) {
                        refreshTable(employee -> {
                            int id = extractNumber(employee.getId());
                            return id == number;
                        });
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                if (!"Mã nhân viên".equals(criterion)) {
                    showError("Giá trị tìm kiếm số phải là số nguyên không âm hoặc khoảng hợp lệ (VD: 10-20).");
                    return;
                }
            }
        }
        
        String keyword = value.toLowerCase();
        refreshTable(employee -> getTextValue(employee, criterion).toLowerCase().contains(keyword));
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

    // TÍNH NĂNG 5: TÌM KIẾM NÂNG CAO (Thống kê số lượng lọc được)
    // Cập nhật ketQuaLocLabel bằng số lượng phần tử của displayedEmployees
    private void refreshTable(Predicate<NhanVien> filter) {
        tableModel.setRowCount(0);
        displayedEmployees.clear();
        if (phongBan.getNhanVien() == null) {
            ketQuaLocLabel.setText("Số lượng đã lọc: 0");
            return;
        }
        for (NhanVien employee : phongBan.getNhanVien()) {
            if (!filter.test(employee)) {
                continue;
            }
            displayedEmployees.add(employee);
            NgayCong workdays = employee.getNgayCong();
            tableModel.addRow(new Object[] { employee.getId(), employee.getFullName(), formatDate(employee.getBirthDate()),
                    formatDate(employee.getNgayVaoLam()), employee.getSoDT(),
                    Boolean.TRUE.equals(employee.getGioiTinh()) ? "Nam" : "Nữ", employee.getTrangThai(), employee.getViTri(),
                    workdays == null ? "Chưa tính" : String.format("%.1f/%.1f (%.1f%%) - %s", workdays.getSoNgayDaLam(),
                            workdays.getSoNgayChuan(), workdays.tinhPhanTramHoanThanh(), workdays.getPhanLoai()) });
        }
        ketQuaLocLabel.setText("Số lượng đã lọc: " + displayedEmployees.size());
    }

    private void quickEditCell(int viewRow, int viewCol) {
        if (viewRow < 0 || viewCol < 0) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        int modelCol = table.convertColumnIndexToModel(viewCol);
        NhanVien nv = displayedEmployees.get(modelRow);
        
        switch (modelCol) {
            case 0:
                String newId = JOptionPane.showInputDialog(this, "Nhập Mã NV mới:", nv.getId());
                if (newId != null && !newId.trim().isEmpty()) {
                    nv.setID(newId.trim());
                    refreshTable(this::matchesCurrentFilter);
                    onChanged.run();
                }
                break;
            case 1:
                String newName = JOptionPane.showInputDialog(this, "Nhập Họ tên mới:", nv.getFullName());
                if (newName != null && !newName.trim().isEmpty()) {
                    nv.setFullName(newName.trim());
                    refreshTable(this::matchesCurrentFilter);
                    onChanged.run();
                }
                break;
            case 2:
            case 3:
                String title = modelCol == 2 ? "Ngày sinh (dd/MM/yyyy):" : "Ngày vào làm (dd/MM/yyyy):";
                String currentVal = modelCol == 2 ? (nv.getBirthDate() != null ? dateFormat.format(nv.getBirthDate()) : "") 
                                                : (nv.getNgayVaoLam() != null ? dateFormat.format(nv.getNgayVaoLam()) : "");
                String newDateStr = JOptionPane.showInputDialog(this, title, currentVal);
                if (newDateStr != null && !newDateStr.trim().isEmpty()) {
                    try {
                        java.util.Date newDate = dateFormat.parse(newDateStr.trim());
                        if (modelCol == 2) nv.setBirthDate(newDate);
                        else nv.setNgayVaoLam(newDate);
                        refreshTable(this::matchesCurrentFilter);
                        onChanged.run();
                    } catch (java.text.ParseException e) {
                        showError("Định dạng ngày không hợp lệ.");
                    }
                }
                break;
            case 4:
                String newPhone = JOptionPane.showInputDialog(this, "Nhập Số điện thoại mới:", nv.getSoDT());
                if (newPhone != null && !newPhone.trim().isEmpty()) {
                    nv.setSoDT(newPhone.trim());
                    refreshTable(this::matchesCurrentFilter);
                    onChanged.run();
                }
                break;
            case 5:
                Object[] options = {"Nam", "Nữ"};
                int choice = JOptionPane.showOptionDialog(this, "Chọn giới tính:", "Giới tính", 
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 
                    Boolean.TRUE.equals(nv.getGioiTinh()) ? options[0] : options[1]);
                if (choice == 0) { nv.setGioiTinh(true); refreshTable(this::matchesCurrentFilter); onChanged.run(); }
                else if (choice == 1) { nv.setGioiTinh(false); refreshTable(this::matchesCurrentFilter); onChanged.run(); }
                break;
            case 6:
                String[] statuses = {"Đang làm", "Nghỉ phép", "Nghỉ việc", "Thử việc"};
                String newStatus = (String) JOptionPane.showInputDialog(this, "Chọn trạng thái:", "Trạng thái",
                    JOptionPane.QUESTION_MESSAGE, null, statuses, nv.getTrangThai());
                if (newStatus != null) {
                    nv.setTrangThai(newStatus);
                    refreshTable(this::matchesCurrentFilter);
                    onChanged.run();
                }
                break;
            case 7:
                GUINhanVien dialog = new GUINhanVien(this, nv, dateFormat, () -> {
                    refreshTable(this::matchesCurrentFilter);
                    if (onChanged != null) onChanged.run();
                });
                dialog.setSelectedTab(2);
                dialog.setVisible(true);
                break;
            case 8:
                showError("Vui lòng mở màn hình chính hoặc hồ sơ để tính ngày công.");
                break;
        }
    }

    private void bulkEditStatus() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            showError("Vui lòng chọn ít nhất một nhân viên.");
            return;
        }
        
        String[] statuses = {"Đang làm", "Nghỉ phép", "Nghỉ việc", "Thử việc"};
        String newStatus = (String) JOptionPane.showInputDialog(this, 
            "Chọn trạng thái mới cho " + selectedRows.length + " nhân viên:", 
            "Sửa trạng thái hàng loạt",
            JOptionPane.QUESTION_MESSAGE, null, statuses, statuses[0]);
            
        if (newStatus != null) {
            for (int viewRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                NhanVien nv = displayedEmployees.get(modelRow);
                nv.setTrangThai(newStatus);
            }
            refreshTable(this::matchesCurrentFilter);
            onChanged.run();
            JOptionPane.showMessageDialog(this, "Đã cập nhật trạng thái cho " + selectedRows.length + " nhân viên.");
        }
    }

    private NhanVien selectedEmployee() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0 || viewRow >= table.getRowCount()) {
            showError("Vui lòng chọn nhân viên.");
            return null;
        }
        int row = table.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= displayedEmployees.size()) return null;
        return displayedEmployees.get(row);
    }

    private void openProfile() {
        NhanVien employee = selectedEmployee();
        if (employee == null) {
            return;
        }
        new GUINhanVien(this, employee, dateFormat, () -> {
            refreshTable(this::matchesCurrentFilter);
            onChanged.run();
        }).setVisible(true);
    }

    private void deleteEmployee() {
        NhanVien employee = selectedEmployee();
        if (employee == null) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa nhân viên " + employee.getFullName() + "?",
                "Xóa nhân viên", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            phongBan.getNhanVien().remove(employee);
            refreshTable(this::matchesCurrentFilter);
            onChanged.run();
        }
    }

    private void resetWorkdays() {
        int result = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn reset ngày công của toàn bộ phòng ban " + phongBan.getTenPB() + "?",
                "Reset ngày công phòng ban", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        for (NhanVien employee : phongBan.getNhanVien()) {
            employee.setNgayCong(new NgayCong(employee, 0, 26));
        }
        refreshTable(this::matchesCurrentFilter);
        onChanged.run();
    }

    // TÍNH NĂNG 5: TÌM KIẾM NÂNG CAO (Khoảng số & Thời gian thực)
    // Thuật toán băm chuỗi (split) để nhận diện nếu người dùng nhập khoảng số như '20-30'
    private boolean matchesCurrentFilter(NhanVien employee) {
        String criterion = (String) criterionBox.getSelectedItem();
        if ("Giới tính".equals(criterion)) {
            return Boolean.TRUE.equals(employee.getGioiTinh()) == "Nam".equals(genderBox.getSelectedItem());
        }
        String value = valueField.getText().trim().toLowerCase();
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
        Calendar today = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        start.setTime(date);
        int years = today.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < start.get(Calendar.DAY_OF_YEAR)) {
            years--;
        }
        return years;
    }

    private String formatDate(Date date) {
        return date == null ? "" : dateFormat.format(date);
    }

    private void styleComponents(Container container) {
        container.setBackground(BACKGROUND);
        for (java.awt.Component component : container.getComponents()) {
            component.setFont(APP_FONT);
            if (component instanceof JTextField field) {
                field.setBackground(Color.WHITE);
                field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(5, 7, 5, 7)));
            } else if (component instanceof JTable currentTable) {
                currentTable.getTableHeader().setFont(APP_FONT.deriveFont(Font.BOLD));
                currentTable.getTableHeader().setBackground(new Color(0, 77, 64));
                currentTable.getTableHeader().setForeground(Color.WHITE);
            }
            if (component instanceof Container child) {
                styleComponents(child);
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Dữ liệu không hợp lệ", JOptionPane.WARNING_MESSAGE);
    }
}
