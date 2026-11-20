package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import model.ChucVu;
import model.NhanVien;
import model.TrinhDo;
import model.PhongBan;
import repository.InMemoryNhanSuRepository;
import service.NhanVienService;

public class GUINhanVien extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final Color BACKGROUND = new Color(245, 247, 246);
	private static final Font APP_FONT = new Font("Segoe UI", Font.PLAIN, 13);
	private final NhanVien nhanVien;
	private final SimpleDateFormat dateFormat;
	private final Runnable onSaved;
	private final NhanVienService nhanVienService;

	private final JTextField idField;
	private final JTextField nameField;
	private final CalendarDateField birthDateField;
	private final CalendarDateField hireDateField;
	private final JTextField phoneField;
	private final JComboBox<String> genderBox;
	private final JComboBox<String> statusBox;
	private final JTextField positionField;
	private final JTextField queQuanField;
	private final JTabbedPane tabs = new JTabbedPane();

	private final DefaultTableModel trinhDoModel = new DefaultTableModel(
			new Object[] { "Trình độ / bằng cấp", "Ngày đạt" }, 0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	private final DefaultTableModel chucVuModel = new DefaultTableModel(
			new Object[] { "Chức vụ", "Ngày nhận", "Ngày kết thúc" }, 0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	private final JTable trinhDoTable = new JTable(trinhDoModel);
	private final JTable chucVuTable = new JTable(chucVuModel);

	public GUINhanVien(Window owner, NhanVien nhanVien, SimpleDateFormat dateFormat,
			Runnable onSaved) {
		this(owner, nhanVien, dateFormat, onSaved, resolveService(owner, nhanVien));
	}

	public GUINhanVien(Window owner, NhanVien nhanVien, SimpleDateFormat dateFormat,
			Runnable onSaved, NhanVienService nhanVienService) {
		super(owner, "Hồ sơ nhân viên - " + nhanVien.getFullName(),
			java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		this.nhanVien = nhanVien;
		this.dateFormat = dateFormat;
		this.onSaved = onSaved;
		this.nhanVienService = java.util.Objects.requireNonNull(nhanVienService);
		nhanVien.capNhatViTriTheoChucVuHienTai();

		idField = new JTextField(value(nhanVien.getId()));
		nameField = new JTextField(value(nhanVien.getFullName()));
		birthDateField = new CalendarDateField(nhanVien.getBirthDate(), false);
				hireDateField = new CalendarDateField(nhanVien.getNgayVaoLam(), false);
		phoneField = new JTextField(value(nhanVien.getSoDT()));
		genderBox = new JComboBox<>(new String[] { "Nam", "Nữ" });
		genderBox.setSelectedIndex(Boolean.TRUE.equals(nhanVien.getGioiTinh()) ? 0 : 1);
		statusBox = new JComboBox<>(new String[] { "Đang làm", "Nghỉ phép", "Nghỉ việc", "Thử việc" });
		statusBox.setSelectedItem(value(nhanVien.getTrangThai()));
		positionField = new JTextField(value(nhanVien.getViTri()));
		queQuanField = new JTextField(value(nhanVien.getQueQuan()));

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(760, 520);
		setLocationRelativeTo(owner);
		buildInterface();
	}

	private static NhanVienService resolveService(Window owner, NhanVien nhanVien) {
		for (Window window = owner; window != null; window = window.getOwner()) {
			if (window instanceof GUI gui) return gui.getNhanVienService();
			if (window instanceof TimKiemNhanVienDialog search) return search.getNhanVienService();
		}
		// Giữ constructor TX1 cho dialog độc lập; vẫn dùng chính model được truyền vào.
		var repository = new InMemoryNhanSuRepository();
		repository.themPhongBan(new PhongBan("Hồ sơ", "", new java.util.ArrayList<>(java.util.List.of(nhanVien))));
		return new NhanVienService(repository);
	}

	public void setSelectedTab(int index) {
		if (index >= 0 && index < tabs.getTabCount()) {
			tabs.setSelectedIndex(index);
		}
	}

	private void buildInterface() {
		tabs.addTab("Thông tin cá nhân", createBasicInfoPanel());
		tabs.addTab("Trình độ học vấn", createTrinhDoPanel());
		tabs.addTab("Lịch sử chức vụ", createChucVuPanel());

		JButton saveButton = new JButton("Lưu hồ sơ");
		JButton cancelButton = new JButton("Hủy");
		saveButton.addActionListener(event -> saveBasicInfo());
		cancelButton.addActionListener(event -> dispose());
		JPanel actions = new JPanel();
		actions.add(saveButton);
		actions.add(cancelButton);

		add(tabs, BorderLayout.CENTER);
		add(actions, BorderLayout.SOUTH);
		styleComponents(getContentPane());
	}

	// TÍNH NĂNG KỸ THUẬT: ĐỆ QUY NHUỘM MÀU GIAO DIỆN
	// Chui vào mọi ngóc ngách của các JPanel để tự động quét và nhuộm màu nút bấm
	private void styleComponents(Container container) {
		container.setBackground(BACKGROUND);
		for (Component component : container.getComponents()) {
			component.setFont(APP_FONT);
			if (component instanceof JButton button) {
				button.setUI(new ModernButtonUI());
				button.setBackground(buttonColor(button.getText()));
				button.setForeground(Color.BLACK);
				button.setOpaque(true);
				button.setContentAreaFilled(true);
				button.setBorderPainted(true);
				button.setFocusPainted(false);
				button.setFont(APP_FONT.deriveFont(Font.BOLD));
				button.setRolloverEnabled(false);
				button.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(button.getBackground().darker()),
						BorderFactory.createEmptyBorder(6, 11, 6, 11)));
			} else if (component instanceof JTextField field) {
				field.setBackground(Color.WHITE);
				field.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(new Color(207, 216, 220)),
						BorderFactory.createEmptyBorder(5, 7, 5, 7)));
			} else if (component instanceof JTable table) {
				table.setRowHeight(28);
				table.setShowGrid(false);
				table.setSelectionBackground(new Color(178, 223, 219));
				table.setSelectionForeground(Color.BLACK);
				table.getTableHeader().setBackground(new Color(0, 77, 64));
				table.getTableHeader().setForeground(Color.WHITE);
				table.getTableHeader().setReorderingAllowed(false);
			}
			if (component instanceof Container child) {
				styleComponents(child);
			}
		}
	}

	private Color buttonColor(String text) {
		if (text.equals("Xóa")) {
			return new Color(255, 205, 210);
		}
		if (text.equals("Sửa") || text.equals("Lịch")) {
			return new Color(187, 222, 251);
		}
		if (text.equals("Hủy")) {
			return new Color(224, 228, 230);
		}
		return new Color(178, 223, 219);
	}

	private JPanel createBasicInfoPanel() {
		JPanel form = new JPanel(new GridLayout(9, 2, 8, 8));
		form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		addField(form, "Mã nhân viên:", idField);
		addField(form, "Họ tên:", nameField);
		addField(form, "Ngày sinh (dd/MM/yyyy):", birthDateField);
		addField(form, "Ngày vào làm (dd/MM/yyyy):", hireDateField);
		addField(form, "Số điện thoại:", phoneField);
		addField(form, "Quê quán:", queQuanField);
		addField(form, "Giới tính:", genderBox);
		addField(form, "Trạng thái:", statusBox);
		addField(form, "Vị trí hiện tại:", positionField);
		positionField.setEditable(false);
		return form;
	}

	private JPanel createTrinhDoPanel() {
		trinhDoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		for (TrinhDo trinhDo : nhanVien.getTrinhDos()) {
			trinhDoModel.addRow(new Object[] { trinhDo.getTrinhDo(), trinhDo.getNgayDo() });
		}
		JButton addButton = new JButton("Thêm trình độ");
		JButton editButton = new JButton("Sửa");
		JButton deleteButton = new JButton("Xóa");
		addButton.addActionListener(event -> editTrinhDo(-1));
		editButton.addActionListener(event -> editTrinhDo(trinhDoTable.getSelectedRow()));
		deleteButton.addActionListener(event -> deleteTrinhDo());
		return createHistoryPanel(trinhDoTable, addButton, editButton, deleteButton);
	}

	private JPanel createChucVuPanel() {
		chucVuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		for (ChucVu chucVu : nhanVien.getLichSuChucVu()) {
			chucVuModel.addRow(new Object[] { chucVu.getTenChucVu(),
					formatDate(chucVu.getNgayNhan()), formatDate(chucVu.getNgayKetthuc()) });
		}
		JButton addButton = new JButton("Thêm chức vụ");
		JButton editButton = new JButton("Sửa");
		JButton deleteButton = new JButton("Xóa");
		addButton.addActionListener(event -> editChucVu(-1));
		editButton.addActionListener(event -> editChucVu(chucVuTable.getSelectedRow()));
		deleteButton.addActionListener(event -> deleteChucVu());
		return createHistoryPanel(chucVuTable, addButton, editButton, deleteButton);
	}

	private JPanel createHistoryPanel(JTable table, JButton addButton, JButton editButton,
			JButton deleteButton) {
		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JPanel buttons = new JPanel();
		buttons.add(addButton);
		buttons.add(editButton);
		buttons.add(deleteButton);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	private void editTrinhDo(int row) {
		TrinhDo current = row >= 0 ? nhanVien.getTrinhDos().get(row) : new TrinhDo();
		JTextField levelField = new JTextField(value(current.getTrinhDo()));
		CalendarDateField dateField = new CalendarDateField(parseDateOrNull(current.getNgayDo()), true);
		JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
		addField(form, "Trình độ / bằng cấp:", levelField);
		addField(form, "Ngày đạt:", dateField);
		if (showForm(form, row < 0 ? "Thêm trình độ" : "Sửa trình độ")
				&& !levelField.getText().trim().isEmpty()) {
			current.setTrinhDo(levelField.getText().trim());
			current.setNgayDo(formatDate(dateField.getDate()));
			if (row < 0) {
				nhanVien.getTrinhDos().add(current);
				trinhDoModel.addRow(new Object[] { current.getTrinhDo(), current.getNgayDo() });
			} else {
				trinhDoModel.setValueAt(current.getTrinhDo(), row, 0);
				trinhDoModel.setValueAt(current.getNgayDo(), row, 1);
			}
		}
	}

	private void editChucVu(int row) {
		ChucVu current = row >= 0 ? nhanVien.getLichSuChucVu().get(row) : new ChucVu();
		JTextField nameField = new JTextField(value(current.getTenChucVu()));
		CalendarDateField startField = new CalendarDateField(current.getNgayNhan(), false);
		CalendarDateField endField = new CalendarDateField(current.getNgayKetthuc(), true);
		JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
		addField(form, "Tên chức vụ:", nameField);
		addField(form, "Ngày nhận (dd/MM/yyyy):", startField);
		addField(form, "Ngày kết thúc (dd/MM/yyyy):", endField);
		if (!showForm(form, row < 0 ? "Thêm chức vụ" : "Sửa chức vụ")
				|| nameField.getText().trim().isEmpty()) {
			return;
		}
		try {
			Date start = parseRequiredDate(startField.getDate(), "Ngày nhận");
			Date end = endField.getDate();
			if (end != null && end.before(start)) {
				throw new ParseException("Ngày kết thúc trước ngày nhận", 0);
			}
			current.setTenChucVu(nameField.getText().trim());
			current.setNgayNhan(start);
			current.setNgayKetthuc(end);
			if (row < 0) {
				// TÍNH NĂNG 3: TỰ ĐỘNG CHỐT SỔ LỊCH SỬ CHỨC VỤ
				// Khi thêm chức vụ mới, hệ thống tự động tìm lại chức vụ cũ (gần nhất) 
				// và lấy ngày nhận chức mới để đắp vào 'Ngày kết thúc' của chức cũ.
				java.util.List<ChucVu> history = nhanVien.getLichSuChucVu();
				if (!history.isEmpty()) {
					ChucVu last = history.get(history.size() - 1);
					if (last.getNgayKetthuc() == null) {
						last.setNgayKetthuc(start);
						chucVuModel.setValueAt(formatDate(start), history.size() - 1, 2);
					}
				}
				history.add(current);
				chucVuModel.addRow(new Object[] { current.getTenChucVu(), formatDate(start), formatDate(end) });
			} else {
				chucVuModel.setValueAt(current.getTenChucVu(), row, 0);
				chucVuModel.setValueAt(formatDate(start), row, 1);
				chucVuModel.setValueAt(formatDate(end), row, 2);
			}
			nhanVien.capNhatViTriTheoChucVuHienTai();
			positionField.setText(value(nhanVien.getViTri()));
		} catch (ParseException exception) {
			showError("Ngày chức vụ phải có định dạng dd/MM/yyyy và hợp lệ.");
		}
	}

	private void deleteTrinhDo() {
		int row = trinhDoTable.getSelectedRow();
		if (row >= 0) {
			nhanVien.getTrinhDos().remove(row);
			trinhDoModel.removeRow(row);
		}
	}

	private void deleteChucVu() {
		int row = chucVuTable.getSelectedRow();
		if (row >= 0) {
			nhanVien.getLichSuChucVu().remove(row);
			chucVuModel.removeRow(row);
		}
	}

	private void saveBasicInfo() {
		if (idField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()) {
			showError("Mã nhân viên và họ tên không được để trống.");
			return;
		}
		try {
			NhanVien input = new NhanVien(nameField.getText(), birthDateField.getDate(),
					phoneField.getText().trim(), genderBox.getSelectedIndex() == 0,
					idField.getText(), (String) statusBox.getSelectedItem(), positionField.getText().trim(),
					queQuanField.getText().trim(), hireDateField.getDate(), null);
			nhanVienService.capNhatThongTin(nhanVien, input);
			onSaved.run();
			dispose();
		} catch (IllegalArgumentException exception) {
			showError(exception.getMessage());
		}
	}

	private Date parseRequiredDate(Date date, String fieldName) throws ParseException {
		if (date == null) {
			throw new ParseException(fieldName + " không được để trống", 0);
		}
		return date;
	}

	private Date parseDateOrNull(String text) {
		if (text == null || text.trim().isEmpty()) {
			return null;
		}
		try {
			return dateFormat.parse(text.trim());
		} catch (ParseException exception) {
			return null;
		}
	}

	private boolean showForm(JPanel form, String title) {
		return JOptionPane.showConfirmDialog(this, form, title,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
	}

	private void addField(JPanel panel, String label, java.awt.Component component) {
		panel.add(new JLabel(label));
		panel.add(component);
	}

	private String formatDate(Date date) {
		return date == null ? "" : dateFormat.format(date);
	}

	private String value(String text) {
		return text == null ? "" : text;
	}

	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message, "Dữ liệu không hợp lệ",
				JOptionPane.WARNING_MESSAGE);
	}

}
