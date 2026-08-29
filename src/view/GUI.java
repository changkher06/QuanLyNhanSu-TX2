package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import model.NgayCong;
import model.NhanVien;
import model.PhongBan;
import model.ChucVu;
import model.TrinhDo;

public class GUI extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final Color BACKGROUND = new Color(245, 247, 246);
	private static final Color TEAL_DARK = new Color(0, 77, 64);
	private static final Color BORDER = new Color(207, 216, 220);
	private static final Font APP_FONT = new Font("Segoe UI", Font.PLAIN, 13);
	private final JLabel tongSoCongTyLabel = new JLabel("Tổng số nhân viên toàn công ty: 0");
	private final DefaultListModel<PhongBan> phongBanModel = new DefaultListModel<>();
	private final JList<PhongBan> phongBanList = new JList<>(phongBanModel);
	private int lastClickedCol = -1;
	private int lastClickedRow = -1;
	// TÍNH NĂNG 7: CHỈNH SỬA TRỰC TIẾP TRÊN BẢNG (Inline-Editing)
	// Cho phép click đúp vào các ô của bảng để sửa dữ liệu nhanh chóng.
	private final DefaultTableModel nhanVienModel = new DefaultTableModel(
			new Object[] { "Mã NV", "Họ tên", "Ngày sinh", "Ngày vào làm", "Số điện thoại", "Giới tính", "Trạng thái", "Vị trí", "Ngày công" }, 0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return column >= 0 && column <= 6;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			if (row < 0 || row >= displayedNhanViens.size() || aValue == null) return;
			NhanVien nv = displayedNhanViens.get(row);
			String val = aValue.toString().trim();
			boolean changed = false;
			switch (column) {
				case 0: nv.setID(val); changed = true; break;
				case 1: nv.setFullName(val); changed = true; break;
				case 2:
					try {
						nv.setBirthDate(DATE_FORMAT.parse(val));
						changed = true;
					} catch (java.text.ParseException e) {
						showError("Ngày sinh không hợp lệ (dd/MM/yyyy).");
						return;
					}
					break;
				case 3:
					try {
						nv.setNgayVaoLam(DATE_FORMAT.parse(val));
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
			}
		}
	};
	private final JTable nhanVienTable = new JTable(nhanVienModel);
	private final List<NhanVien> displayedNhanViens = new ArrayList<>();

	private final JTextField tenPhongBanField = new JTextField();
	private final JTextField truongPhongField = new JTextField();
	private final JTextField tongSoNhanVienField = new JTextField();
	private final JTextField maNhanVienField = new JTextField();
	private final JTextField hoTenField = new JTextField();
	private final CalendarDateField ngaySinhField = new CalendarDateField(null, false);
	private final CalendarDateField ngayVaoLamField = new CalendarDateField(null, false);
	private final JTextField soDienThoaiField = new JTextField();
	private final JComboBox<String> gioiTinhBox = new JComboBox<>(new String[] { "Nam", "Nữ" });
	private final JComboBox<String> trangThaiBox = new JComboBox<>(new String[] { "Đang làm", "Nghỉ phép", "Nghỉ việc", "Thử việc" });
	private final JTextField viTriField = new JTextField();
	private final JTextField queQuanField = new JTextField();
	private final JTextField ngayCongField = new JTextField();
	private final JTextField ngayChuanField = new JTextField("26");
	private final JLabel ketQuaNgayCongLabel = new JLabel("Chưa có dữ liệu ngày công");

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

	public GUI() {
		super("Quản lý nhân sự công ty Thành Hưng");
		UIManager.put("Panel.background", BACKGROUND);
		UIManager.put("OptionPane.background", BACKGROUND);
		UIManager.put("OptionPane.messageFont", APP_FONT);
		DATE_FORMAT.setLenient(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new java.awt.Dimension(1000, 620));
		buildInterface();
		setExtendedState(JFrame.MAXIMIZED_BOTH);
	}

	private void buildInterface() {
		setJMenuBar(createMenuBar());
		phongBanList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		phongBanList.setBorder(BorderFactory.createTitledBorder("Phòng ban"));
		phongBanList.setCellRenderer((list, value, index, selected, focus) -> {
			JLabel label = new JLabel(value == null ? "" : createPhongBanDisplayText(value));
			label.setOpaque(true);
			label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
			if (selected) {
				label.setBackground(list.getSelectionBackground());
				label.setForeground(list.getSelectionForeground());
			} else {
				label.setBackground(list.getBackground());
				label.setForeground(list.getForeground());
			}
			return label;
		});
		phongBanList.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				loadSelectedPhongBan();
			}
		});
		addPhongBanContextMenu();

		JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
		leftPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
		leftPanel.add(new JScrollPane(phongBanList), BorderLayout.CENTER);
		leftPanel.add(createPhongBanButtons(), BorderLayout.SOUTH);

		JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
		rightPanel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));
		rightPanel.add(createPhongBanForm(), BorderLayout.NORTH);
		rightPanel.add(new JScrollPane(nhanVienTable), BorderLayout.CENTER);
		rightPanel.add(createNhanVienPanel(), BorderLayout.SOUTH);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
		splitPane.setDividerLocation(220);
		splitPane.setBorder(null);
		splitPane.setBackground(BACKGROUND);
		add(splitPane, BorderLayout.CENTER);
		
		JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		tongSoCongTyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		tongSoCongTyLabel.setForeground(TEAL_DARK);
		statusBar.add(tongSoCongTyLabel);
		add(statusBar, BorderLayout.SOUTH);
		
		styleComponents(getContentPane());
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu searchMenu = new JMenu("Tìm kiếm nhân viên");
		JMenuItem nameItem = new JMenuItem("Theo họ tên");
		JMenuItem idItem = new JMenuItem("Theo mã nhân viên");
		JMenuItem statusItem = new JMenuItem("Theo trạng thái");
		JMenuItem positionItem = new JMenuItem("Theo vị trí");
		JMenuItem ageItem = new JMenuItem("Theo tuổi");
		JMenuItem genderItem = new JMenuItem("Theo giới tính");
		JMenuItem seniorityItem = new JMenuItem("Theo thâm niên");
		JMenuItem allItem = new JMenuItem("Hiển thị tất cả");
		nameItem.addActionListener(event -> showSearchDialog("Họ tên"));
		idItem.addActionListener(event -> showSearchDialog("Mã nhân viên"));
		statusItem.addActionListener(event -> showSearchDialog("Trạng thái"));
		positionItem.addActionListener(event -> showSearchDialog("Vị trí"));
		ageItem.addActionListener(event -> showSearchDialog("Tuổi"));
		genderItem.addActionListener(event -> showSearchDialog("Giới tính"));
		seniorityItem.addActionListener(event -> showSearchDialog("Thâm niên"));
		allItem.addActionListener(event -> showSearchDialog("Họ tên"));
		searchMenu.add(nameItem);
		searchMenu.add(idItem);
		searchMenu.add(statusItem);
		searchMenu.add(positionItem);
		searchMenu.add(ageItem);
		searchMenu.add(genderItem);
		searchMenu.add(seniorityItem);
		searchMenu.addSeparator();
		searchMenu.add(allItem);
		JMenu helpMenu = new JMenu("Trợ giúp");
		JMenuItem featureListItem = new JMenuItem("Danh sách tính năng");
		featureListItem.addActionListener(event -> showFeatureList());
		helpMenu.add(featureListItem);

		menuBar.add(searchMenu);
		menuBar.add(helpMenu);
		return menuBar;
	}

	private void showFeatureList() {
		String features = "<html><body style='width: 500px; font-family: Segoe UI, sans-serif; font-size: 13px;'>"
			+ "<h2 style='color: #004d40; margin-top: 0;'>Danh sách tính năng ứng dụng</h2>"
			+ "<ul style='margin-left: 15px;'>"
			+ "<li><b>Quản lý phòng ban:</b> Xem, thêm, sửa, xóa phòng ban.</li>"
			+ "<li><b>Quản lý nhân viên:</b> Hiển thị danh sách nhân viên theo phòng ban với đầy đủ thông tin chi tiết.</li>"
			+ "<li><b>Hồ sơ chi tiết:</b> Quản lý lịch sử chức vụ và bằng cấp. <b>Đặc biệt:</b> Vị trí hiện tại tự động cập nhật, và khi thêm chức vụ mới, hệ thống tự động điền 'Ngày kết thúc' cho chức vụ cũ.</li>"
			+ "<li><b>Tính ngày công:</b> Tính toán số ngày đi làm, hiển thị tỷ lệ % hoàn thành và phân loại tự động thành các mức: Kém, Trung bình, Khá, Giỏi, Xuất sắc.</li>"
			+ "<li><b>Tìm kiếm nâng cao:</b> Tìm kiếm linh hoạt theo nhiều tiêu chí (họ tên, mã NV, trạng thái, vị trí, giới tính, tuổi, thâm niên). <b>Đặc biệt:</b> Hỗ trợ tìm theo khoảng (ví dụ '20-30').</li>"
			+ "<li><b>Sắp xếp thông minh & Cố định bảng:</b> Bấm vào tiêu đề cột Ngày sinh / Ngày vào làm (và các cột khác) để tự động sắp xếp. Cấu trúc bảng được khóa cố định chống kéo thả sai lệch.</li>"
			+ "<li><b>Chỉnh sửa ngay trên bảng (Inline-Editing):</b> Nhấn đúp chuột vào bất kỳ ô nào trên bảng để sửa văn bản, sửa ngày, hoặc chọn Trạng thái/Giới tính bằng menu thả xuống tiện lợi.</li>"
			+ "<li><b>Chỉnh sửa nhanh Vị trí:</b> Khi click đúp vào cột Vị trí, hệ thống sẽ mở thẳng sang tab 'Lịch sử chức vụ' cực kỳ nhanh chóng.</li>"
			+ "<li><b>Thao tác chuột phải đa năng:</b> Click chuột phải vào nhân viên để sửa hồ sơ, xóa nhanh, chỉnh sửa nhanh ô đang trỏ chuột.</li>"
			+ "<li><b>Sửa trạng thái hàng loạt:</b> Bôi đen nhiều nhân viên cùng lúc, click chuột phải chọn 'Chỉnh sửa trạng thái hàng loạt' để thay đổi trạng thái đồng thời.</li>"
			+ "</ul>"
			+ "</body></html>";
			
		JOptionPane.showMessageDialog(this, features, "Hướng dẫn tính năng", JOptionPane.INFORMATION_MESSAGE);
	}

	// TÍNH NĂNG 5: TÌM KIẾM NÂNG CAO
	// Mở hộp thoại tìm kiếm hỗ trợ lọc theo khoảng (VD: '20-30') và thống kê thời gian thực
	private void showSearchDialog(String criterion) {
		PhongBan phongBan = phongBanList.getSelectedValue();
		if (phongBan == null) {
			showError("Vui lòng chọn phòng ban trước khi tìm kiếm.");
			return;
		}
		new TimKiemNhanVienDialog(this, phongBan, DATE_FORMAT, criterion, () -> {
			refreshNhanVienTable();
			phongBanList.repaint();
		updateTongSoCongTy();
		}).setVisible(true);
	}

	private JPanel createPhongBanButtons() {
		JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
		JButton addButton = new JButton("Thêm");
		JButton sampleButton = new JButton("Nạp dữ liệu mẫu");

		addButton.addActionListener(event -> addPhongBan());
		sampleButton.addActionListener(event -> loadSampleData());
		panel.add(addButton);
		panel.add(sampleButton);
		return panel;
	}

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
						BorderFactory.createLineBorder(BORDER),
						BorderFactory.createEmptyBorder(5, 7, 5, 7)));
			} else if (component instanceof JTable table) {
				table.setRowHeight(28);
				table.setShowGrid(false);
				table.setIntercellSpacing(new java.awt.Dimension(0, 0));
				table.setSelectionBackground(new Color(178, 223, 219));
				table.setSelectionForeground(Color.BLACK);
				table.getTableHeader().setFont(APP_FONT.deriveFont(Font.BOLD));
				table.getTableHeader().setBackground(TEAL_DARK);
				table.getTableHeader().setForeground(Color.WHITE);
			} else if (component instanceof JList<?> list) {
				list.setFont(APP_FONT);
				list.setBackground(Color.WHITE);
				list.setSelectionBackground(new Color(178, 223, 219));
				list.setSelectionForeground(Color.BLACK);
			}
			if (component instanceof Container child) {
				styleComponents(child);
			}
		}
	}

	private Color buttonColor(String text) {
		if (text.contains("Xóa") || text.contains("Reset")) {
			return new Color(255, 205, 210);
		}
		if (text.contains("Nạp") || text.contains("Tính")) {
			return new Color(255, 224, 178);
		}
		if (text.equals("Sửa") || text.equals("Lịch")) {
			return new Color(187, 222, 251);
		}
		if (text.equals("Hủy") || text.equals("Làm mới")) {
			return new Color(224, 228, 230);
		}
		return new Color(178, 223, 219);
	}

	private String createPhongBanDisplayText(PhongBan phongBan) {
		String tenTruongPhong = timTenTruongPhong(phongBan);
		int tongNhanVien = (phongBan.getNhanVien() != null) ? phongBan.getNhanVien().size() : 0;
		return "<html><b>" + phongBan.getTenPB() + "</b><br>Trưởng phòng: "
				+ tenTruongPhong + "<br>Tổng nhân viên: " + tongNhanVien + "</html>";
	}

	private String timTenTruongPhong(PhongBan phongBan) {
		if (phongBan.getNhanVien() != null) {
			for (NhanVien nhanVien : phongBan.getNhanVien()) {
				if (phongBan.getIdTruongPhong() != null
						&& phongBan.getIdTruongPhong().equals(nhanVien.getId())) {
					return nhanVien.getFullName();
				}
			}
		}
		return phongBan.getIdTruongPhong() == null || phongBan.getIdTruongPhong().isEmpty()
				? "Chưa có" : phongBan.getIdTruongPhong();
	}

	private void addPhongBanContextMenu() {
		JPopupMenu contextMenu = new JPopupMenu();
		JMenuItem editItem = new JMenuItem("Sửa phòng ban");
		JMenuItem deleteItem = new JMenuItem("Xóa phòng ban");
		editItem.addActionListener(event -> showEditPhongBanDialog());
		deleteItem.addActionListener(event -> showDeletePhongBanDialog());
		contextMenu.add(editItem);
		contextMenu.add(deleteItem);

		phongBanList.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				showPhongBanContextMenu(event, contextMenu);
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				showPhongBanContextMenu(event, contextMenu);
			}
		});
	}

	private void showPhongBanContextMenu(MouseEvent event, JPopupMenu contextMenu) {
		if (!event.isPopupTrigger()) {
			return;
		}
		int index = phongBanList.locationToIndex(event.getPoint());
		if (index < 0 || !phongBanList.getCellBounds(index, index).contains(event.getPoint())) {
			return;
		}
		phongBanList.setSelectedIndex(index);
		contextMenu.show(phongBanList, event.getX(), event.getY());
	}

	private void showEditPhongBanDialog() {
		PhongBan phongBan = phongBanList.getSelectedValue();
		if (phongBan == null) {
			return;
		}
		JTextField nameField = new JTextField(phongBan.getTenPB());
		JTextField managerField = new JTextField(phongBan.getIdTruongPhong());
		JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
		form.add(new JLabel("Tên phòng ban:"));
		form.add(nameField);
		form.add(new JLabel("Mã trưởng phòng:"));
		form.add(managerField);

		int result = JOptionPane.showConfirmDialog(this, form, "Sửa thông tin phòng ban",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION) {
			return;
		}
		if (nameField.getText().trim().isEmpty()) {
			showError("Tên phòng ban không được để trống.");
			return;
		}
		phongBan.setTenPB(nameField.getText().trim());
		phongBan.setIdTruongPhong(managerField.getText().trim());
		tenPhongBanField.setText(phongBan.getTenPB());
		truongPhongField.setText(phongBan.getIdTruongPhong());
		phongBanList.repaint();
		updateTongSoCongTy();
		loadSelectedPhongBan();
	}

	private void showDeletePhongBanDialog() {
		PhongBan phongBan = phongBanList.getSelectedValue();
		if (phongBan == null) {
			return;
		}
		int result = JOptionPane.showConfirmDialog(this,
				"Bạn có chắc muốn xóa phòng ban " + phongBan.getTenPB() + "?",
				"Xóa phòng ban", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (result == JOptionPane.YES_OPTION) {
			deletePhongBan();
		}
	}

	private JPanel createPhongBanForm() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Thông tin phòng ban"));
		addField(panel, "Tên phòng ban:", tenPhongBanField, 0, 0);
		addField(panel, "Mã trưởng phòng:", truongPhongField, 0, 1);
		addField(panel, "Tổng số nhân viên:", tongSoNhanVienField, 0, 2);
		tongSoNhanVienField.setEditable(false); // Không cho sửa
		return panel;
	}

	private JPanel createNhanVienPanel() {
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createTitledBorder("Thông tin nhân viên"));

		JPanel fields = new JPanel(new GridLayout(4, 6, 6, 5));
		fields.add(new JLabel("Mã NV"));
		fields.add(new JLabel("Họ tên"));
		fields.add(new JLabel("Ngày sinh"));
		fields.add(new JLabel("Ngày vào làm"));
		fields.add(new JLabel("Số điện thoại"));
		fields.add(new JLabel("Quê quán"));
		fields.add(maNhanVienField);
		fields.add(hoTenField);
		fields.add(ngaySinhField);
		fields.add(ngayVaoLamField);
		fields.add(soDienThoaiField);
		fields.add(queQuanField);
		fields.add(new JLabel("Giới tính"));
		fields.add(new JLabel("Trạng thái"));
		fields.add(new JLabel("Vị trí"));
		fields.add(new JLabel("Ngày đã làm"));
		fields.add(new JLabel("Ngày chuẩn"));
		fields.add(new JLabel("")); // Trống cho khớp 6 cột
		fields.add(gioiTinhBox);
		fields.add(trangThaiBox);
		fields.add(viTriField);
		fields.add(ngayCongField);
		fields.add(ngayChuanField);
		fields.add(new JLabel("")); // Trống cho khớp 6 cột
		panel.add(fields, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton addButton = new JButton("Thêm nhân viên");
		JButton clearButton = new JButton("Làm mới");
		JButton calculateButton = new JButton("Tính ngày công");
		JButton resetButton = new JButton("Reset ngày công phòng ban");
		addButton.addActionListener(event -> addNhanVien());
		clearButton.addActionListener(event -> clearNhanVienForm());
		calculateButton.addActionListener(event -> calculateNgayCong());
		resetButton.addActionListener(event -> resetNgayCong());
		buttons.add(addButton);
		buttons.add(clearButton);
		buttons.add(calculateButton);
		buttons.add(resetButton);
		JPanel footer = new JPanel(new BorderLayout());
		footer.add(ketQuaNgayCongLabel, BorderLayout.WEST);
		footer.add(buttons, BorderLayout.EAST);
		panel.add(footer, BorderLayout.SOUTH);

		nhanVienTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		nhanVienTable.getTableHeader().setReorderingAllowed(false);
		
		javax.swing.JComboBox<String> cbGioiTinh = new javax.swing.JComboBox<>(new String[]{"Nam", "Nữ"});
		nhanVienTable.getColumnModel().getColumn(5).setCellEditor(new javax.swing.DefaultCellEditor(cbGioiTinh));
		
		javax.swing.JComboBox<String> cbTrangThai = new javax.swing.JComboBox<>(new String[]{"Đang làm", "Nghỉ phép", "Nghỉ việc", "Thử việc"});
		nhanVienTable.getColumnModel().getColumn(6).setCellEditor(new javax.swing.DefaultCellEditor(cbTrangThai));
		
		// TÍNH NĂNG 6: SẮP XẾP THÔNG MINH BẢNG DỮ LIỆU
		// Khởi tạo TableRowSorter để cho phép người dùng click tiêu đề cột để sắp xếp
		javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(nhanVienModel);
		nhanVienTable.setRowSorter(sorter);
		java.util.Comparator<String> dateComparator = (s1, s2) -> {
			try {
				if (s1 == null || s1.isEmpty()) return s2 == null || s2.isEmpty() ? 0 : -1;
				if (s2 == null || s2.isEmpty()) return 1;
				Date d1 = DATE_FORMAT.parse(s1);
				Date d2 = DATE_FORMAT.parse(s2);
				return d1.compareTo(d2);
			} catch (ParseException e) {
				return s1.compareTo(s2);
			}
		};
		sorter.setComparator(2, dateComparator);
		sorter.setComparator(3, dateComparator);

		nhanVienTable.getSelectionModel().addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				loadSelectedNhanVien();
			}
		});
		addNhanVienContextMenu();
		return panel;
	}

	private void addNhanVienContextMenu() {
		JPopupMenu contextMenu = new JPopupMenu();
		JMenuItem quickEditItem = new JMenuItem("Chỉnh sửa nhanh ô này");
		JMenuItem bulkEditItem = new JMenuItem("Chỉnh sửa trạng thái hàng loạt");
		JMenuItem editItem = new JMenuItem("Mở hồ sơ nhân viên");
		JMenuItem deleteItem = new JMenuItem("Xóa nhân viên");
		
		quickEditItem.addActionListener(event -> {
			if (lastClickedRow >= 0 && lastClickedCol >= 0) {
				quickEditCell(lastClickedRow, lastClickedCol);
			}
		});
		bulkEditItem.addActionListener(event -> bulkEditStatus());
		editItem.addActionListener(event -> showEditNhanVienDialog());
		deleteItem.addActionListener(event -> showDeleteNhanVienDialog());
		
		contextMenu.add(quickEditItem);
		contextMenu.add(bulkEditItem);
		contextMenu.addSeparator();
		contextMenu.add(editItem);
		contextMenu.add(deleteItem);

		nhanVienTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				showNhanVienContextMenu(event, contextMenu);
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				showNhanVienContextMenu(event, contextMenu);
			}
		});
	}

	// TÍNH NĂNG 8: CHỈNH SỬA NHANH VỊ TRÍ (Shortcut nhảy Tab)
	// Cho phép click đúp vào cột Vị trí để mở thẳng sang Tab Lịch sử chức vụ của Hồ sơ
	private void quickEditCell(int viewRow, int viewCol) {
		if (viewRow < 0 || viewCol < 0) return;
		int modelRow = nhanVienTable.convertRowIndexToModel(viewRow);
		int modelCol = nhanVienTable.convertColumnIndexToModel(viewCol);
		NhanVien nv = displayedNhanViens.get(modelRow);
		
		switch (modelCol) {
			case 0:
				String newId = JOptionPane.showInputDialog(this, "Nhập Mã NV mới:", nv.getId());
				if (newId != null && !newId.trim().isEmpty()) {
					nv.setID(newId.trim());
					refreshNhanVienTable();
				}
				break;
			case 1:
				String newName = JOptionPane.showInputDialog(this, "Nhập Họ tên mới:", nv.getFullName());
				if (newName != null && !newName.trim().isEmpty()) {
					nv.setFullName(newName.trim());
					refreshNhanVienTable();
				}
				break;
			case 2:
			case 3:
				String title = modelCol == 2 ? "Ngày sinh (dd/MM/yyyy):" : "Ngày vào làm (dd/MM/yyyy):";
				String currentVal = modelCol == 2 ? (nv.getBirthDate() != null ? DATE_FORMAT.format(nv.getBirthDate()) : "") 
												: (nv.getNgayVaoLam() != null ? DATE_FORMAT.format(nv.getNgayVaoLam()) : "");
				String newDateStr = JOptionPane.showInputDialog(this, title, currentVal);
				if (newDateStr != null && !newDateStr.trim().isEmpty()) {
					try {
						java.util.Date newDate = DATE_FORMAT.parse(newDateStr.trim());
						if (modelCol == 2) nv.setBirthDate(newDate);
						else nv.setNgayVaoLam(newDate);
						refreshNhanVienTable();
					} catch (java.text.ParseException e) {
						showError("Định dạng ngày không hợp lệ.");
					}
				}
				break;
			case 4:
				String newPhone = JOptionPane.showInputDialog(this, "Nhập Số điện thoại mới:", nv.getSoDT());
				if (newPhone != null && !newPhone.trim().isEmpty()) {
					nv.setSoDT(newPhone.trim());
					refreshNhanVienTable();
				}
				break;
			case 5:
				Object[] options = {"Nam", "Nữ"};
				int choice = JOptionPane.showOptionDialog(this, "Chọn giới tính:", "Giới tính", 
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 
					Boolean.TRUE.equals(nv.getGioiTinh()) ? options[0] : options[1]);
				if (choice == 0) { nv.setGioiTinh(true); refreshNhanVienTable(); }
				else if (choice == 1) { nv.setGioiTinh(false); refreshNhanVienTable(); }
				break;
			case 6:
				String[] statuses = {"Đang làm", "Nghỉ phép", "Nghỉ việc", "Thử việc"};
				String newStatus = (String) JOptionPane.showInputDialog(this, "Chọn trạng thái:", "Trạng thái",
					JOptionPane.QUESTION_MESSAGE, null, statuses, nv.getTrangThai());
				if (newStatus != null) {
					nv.setTrangThai(newStatus);
					refreshNhanVienTable();
				}
				break;
			case 7:
				GUINhanVien dialog = new GUINhanVien(this, nv, DATE_FORMAT, this::refreshNhanVienTable);
				dialog.setSelectedTab(2);
				dialog.setVisible(true);
				break;
			case 8:
				showError("Vui lòng sử dụng tính năng 'Tính ngày công' ở bên dưới.");
				break;
		}
	}

	// TÍNH NĂNG 10: SỬA TRẠNG THÁI HÀNG LOẠT (Bulk-Editing)
	// Quét các dòng đang bôi đen và dùng convertRowIndexToModel để sửa dữ liệu an toàn.
	private void bulkEditStatus() {
		int[] selectedRows = nhanVienTable.getSelectedRows();
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
				int modelRow = nhanVienTable.convertRowIndexToModel(viewRow);
				NhanVien nv = displayedNhanViens.get(modelRow);
				nv.setTrangThai(newStatus);
			}
			refreshNhanVienTable();
			JOptionPane.showMessageDialog(this, "Đã cập nhật trạng thái cho " + selectedRows.length + " nhân viên.");
		}
	}

	// TÍNH NĂNG 9: THAO TÁC CHUỘT PHẢI ĐA NĂNG
	// Bắt sự kiện chuột phải thông minh, không làm mất bôi đen hiện tại nếu click trúng vùng xanh.
	private void showNhanVienContextMenu(java.awt.event.MouseEvent event, javax.swing.JPopupMenu contextMenu) {
		if (!event.isPopupTrigger()) {
			return;
		}
		int row = nhanVienTable.rowAtPoint(event.getPoint());
		int col = nhanVienTable.columnAtPoint(event.getPoint());
		if (row < 0) {
			return;
		}
		if (!nhanVienTable.isRowSelected(row)) {
			nhanVienTable.setRowSelectionInterval(row, row);
		}
		lastClickedRow = row;
		lastClickedCol = col;
		contextMenu.show(nhanVienTable, event.getX(), event.getY());
	}

	// TÍNH NĂNG 3: HỒ SƠ CHI TIẾT & TỰ ĐỘNG HÓA LỊCH SỬ CHỨC VỤ
	// Mở cửa sổ hồ sơ, nơi phần mềm sẽ tự động chốt sổ ngày kết thúc khi thêm chức vụ mới
	private void showEditNhanVienDialog() {
		PhongBan phongBan = getSelectedPhongBan();
		int viewIndex = nhanVienTable.getSelectedRow();
		if (phongBan == null || viewIndex < 0) {
			return;
		}

		int index = nhanVienTable.convertRowIndexToModel(viewIndex);
		NhanVien nhanVien = displayedNhanViens.get(index);
		GUINhanVien dialog = new GUINhanVien(this, nhanVien, DATE_FORMAT, () -> {
			refreshNhanVienTable();
			phongBanList.repaint();
		updateTongSoCongTy();
			loadSelectedNhanVien();
		});
		dialog.setVisible(true);
	}

	private void showDeleteNhanVienDialog() {
		PhongBan phongBan = getSelectedPhongBan();
		int viewIndex = nhanVienTable.getSelectedRow();
		if (phongBan == null || viewIndex < 0) {
			return;
		}
		int index = nhanVienTable.convertRowIndexToModel(viewIndex);
		NhanVien nhanVien = displayedNhanViens.get(index);
		int result = JOptionPane.showConfirmDialog(this,
				"Bạn có chắc muốn xóa nhân viên " + nhanVien.getFullName() + "?",
				"Xóa nhân viên", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (result == JOptionPane.YES_OPTION) {
			deleteNhanVien();
		}
	}

	private void addField(JPanel panel, String labelText, java.awt.Component field, int column, int row) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column * 2;
		constraints.gridy = row;
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.anchor = GridBagConstraints.WEST;
		panel.add(new JLabel(labelText), constraints);

		constraints.gridx++;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add(field, constraints);
	}

	private void addPhongBan() {
		if (tenPhongBanField.getText().trim().isEmpty()) {
			showError("Vui lòng nhập tên phòng ban.");
			return;
		}
		PhongBan phongBan = new PhongBan(tenPhongBanField.getText().trim(),
				truongPhongField.getText().trim(), new ArrayList<>());
		phongBanModel.addElement(phongBan);
		phongBanList.setSelectedValue(phongBan, true);
		clearPhongBanForm();
	}

	private void loadSampleData() {
		try {
			phongBanModel.clear();
			java.util.Random rand = new java.util.Random(12345);
			int idCounter = 100;

			// 1. Phòng Kinh Doanh
			NhanVien tpKinhDoanh = new NhanVien("Nguyen Van An", DATE_FORMAT.parse("12/03/1985"), "0901000001", true, "NV001", "Đang làm", "Trưởng phòng", "Hà Nội", null);
			tpKinhDoanh.setNgayVaoLam(DATE_FORMAT.parse("01/08/2007"));
			tpKinhDoanh.setNgayCong(new NgayCong(tpKinhDoanh, 22, 26));
			tpKinhDoanh.getTrinhDos().add(new TrinhDo("Đại học Quản trị kinh doanh", "15/06/2007"));
			tpKinhDoanh.getLichSuChucVu().add(new ChucVu("Nhân viên kinh doanh", DATE_FORMAT.parse("01/08/2007"), DATE_FORMAT.parse("31/12/2015")));
			tpKinhDoanh.getLichSuChucVu().add(new ChucVu("Trưởng phòng kinh doanh", DATE_FORMAT.parse("01/01/2016"), null));
			tpKinhDoanh.capNhatViTriTheoChucVuHienTai();
			
			ArrayList<NhanVien> kinhDoanh = new ArrayList<>();
			kinhDoanh.add(tpKinhDoanh);
			for (int i = 0; i < 15; i++) {
				kinhDoanh.add(generateRandomNhanVien(idCounter++, "Nhân viên kinh doanh", rand));
			}
			phongBanModel.addElement(new PhongBan("Kinh doanh", "NV001", kinhDoanh));

			// 2. Phòng Kỹ Thuật
			NhanVien tpKyThuat = new NhanVien("Le Van Cuong", DATE_FORMAT.parse("08/11/1982"), "0901000002", true, "NV002", "Đang làm", "Trưởng phòng", "Hồ Chí Minh", null);
			tpKyThuat.setNgayVaoLam(DATE_FORMAT.parse("15/05/2010"));
			tpKyThuat.setNgayCong(new NgayCong(tpKyThuat, 26, 26));
			tpKyThuat.getTrinhDos().add(new TrinhDo("Thạc sĩ CNTT", "20/09/2006"));
			tpKyThuat.getLichSuChucVu().add(new ChucVu("Trưởng phòng kỹ thuật", DATE_FORMAT.parse("15/05/2010"), null));
			tpKyThuat.capNhatViTriTheoChucVuHienTai();

			ArrayList<NhanVien> kyThuat = new ArrayList<>();
			kyThuat.add(tpKyThuat);
			for (int i = 0; i < 20; i++) {
				kyThuat.add(generateRandomNhanVien(idCounter++, "Kỹ sư phần mềm", rand));
			}
			phongBanModel.addElement(new PhongBan("Kỹ thuật", "NV002", kyThuat));

			// 3. Phòng Nhân Sự
			NhanVien tpNhanSu = new NhanVien("Ngo Thanh Truc", DATE_FORMAT.parse("22/11/1988"), "0901000003", false, "NV003", "Đang làm", "Trưởng phòng", "Đà Nẵng", null);
			tpNhanSu.setNgayVaoLam(DATE_FORMAT.parse("01/03/2012"));
			tpNhanSu.setNgayCong(new NgayCong(tpNhanSu, 24, 26));
			
			ArrayList<NhanVien> nhanSu = new ArrayList<>();
			nhanSu.add(tpNhanSu);
			for (int i = 0; i < 10; i++) {
				nhanSu.add(generateRandomNhanVien(idCounter++, "Chuyên viên nhân sự", rand));
			}
			phongBanModel.addElement(new PhongBan("Nhân sự", "NV003", nhanSu));

			// 4. Phòng Kế Toán
			NhanVien tpKeToan = new NhanVien("Doan Van Toan", DATE_FORMAT.parse("30/01/1980"), "0901000004", true, "NV004", "Đang làm", "Trưởng phòng", "Cần Thơ", null);
			tpKeToan.setNgayVaoLam(DATE_FORMAT.parse("10/10/2005"));
			tpKeToan.setNgayCong(new NgayCong(tpKeToan, 25, 26));
			tpKeToan.getTrinhDos().add(new TrinhDo("Thac si Ke toan", "15/06/2004"));
			tpKeToan.getLichSuChucVu().add(new ChucVu("Ke toan vien", DATE_FORMAT.parse("15/06/2005"), DATE_FORMAT.parse("15/06/2010")));
			tpKeToan.getLichSuChucVu().add(new ChucVu("Ke toan truong", DATE_FORMAT.parse("16/06/2010"), null));
			tpKeToan.capNhatViTriTheoChucVuHienTai();
			
			ArrayList<NhanVien> keToan = new ArrayList<>();
			keToan.add(tpKeToan);
			for (int i = 0; i < 10; i++) {
				keToan.add(generateRandomNhanVien(idCounter++, "Ke toan vien", rand));
			}
			phongBanModel.addElement(new PhongBan("Ke toan", "NV004", keToan));

			phongBanList.setSelectedIndex(0);
			phongBanList.repaint();
		updateTongSoCongTy();
		} catch (ParseException exception) {
			showError("Không thể khởi tạo dữ liệu mẫu.");
		}
	}

	private NhanVien generateRandomNhanVien(int id, String chucVu, java.util.Random rand) throws ParseException {
		String[] ho = {"Nguyen", "Tran", "Le", "Pham", "Hoang", "Huynh", "Phan", "Vu", "Vo", "Dang", "Bui", "Do", "Ngo", "Ly", "Duong"};
		String[] demNam = {"Van", "Huu", "Quang", "Dinh", "Xuan", "Cong", "Thanh", "Minh", "Tien", "Quoc"};
		String[] demNu = {"Thi", "Thu", "Ngoc", "Bich", "Kim", "Thanh", "Hong", "Lan", "Mai", "Tuyet"};
		String[] ten = {"An", "Binh", "Cuong", "Dung", "Hoa", "Hung", "Linh", "Minh", "Nghia", "Phuc", "Quan", "Sang", "Thao", "Trang", "Tuan", "Vinh", "Son", "Tien", "Phat", "Tai", "Giang", "Phong", "Kien", "Lam", "My", "Nhung", "Oanh", "Quyen", "Sinh", "Tam", "Uyen", "Vy", "Yen"};
		String[] queQuanArr = {"Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Cần Thơ", "Hải Phòng", "Quảng Ninh", "Nghệ An", "Thanh Hóa", "Đồng Nai", "Bình Dương", "Bắc Ninh", "Vĩnh Phúc"};

		boolean isNam = rand.nextBoolean();
		String fullName = ho[rand.nextInt(ho.length)] + " " + (isNam ? demNam[rand.nextInt(demNam.length)] : demNu[rand.nextInt(demNu.length)]) + " " + ten[rand.nextInt(ten.length)];
		String idStr = String.format("NV%03d", id);
		String soDT = String.format("09%08d", rand.nextInt(100000000));
		String queQuan = queQuanArr[rand.nextInt(queQuanArr.length)];
		
		long startBirth = DATE_FORMAT.parse("01/01/1980").getTime();
		long endBirth = DATE_FORMAT.parse("31/12/2000").getTime();
		Date birthDate = new Date(startBirth + (long)(rand.nextDouble() * (endBirth - startBirth)));
		
		long startHire = DATE_FORMAT.parse("01/01/2015").getTime();
		long endHire = DATE_FORMAT.parse("31/12/2023").getTime();
		Date hireDate = new Date(startHire + (long)(rand.nextDouble() * (endHire - startHire)));
		
		NhanVien nv = new NhanVien(fullName, birthDate, soDT, isNam, idStr, "Đang làm", chucVu, queQuan, null);
		nv.setNgayVaoLam(hireDate);
		nv.setNgayCong(new NgayCong(nv, 20 + rand.nextInt(7), 26)); // Random 20-26
		
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.setTime(birthDate);
		int namTotNghiep = cal.get(java.util.Calendar.YEAR) + 22;
		nv.getTrinhDos().add(new TrinhDo("Cu nhan", "15/07/" + namTotNghiep));
		nv.getLichSuChucVu().add(new ChucVu(chucVu, hireDate, null));
		nv.capNhatViTriTheoChucVuHienTai();
		
		return nv;
	}

	private void deletePhongBan() {
		int index = phongBanList.getSelectedIndex();
		if (index < 0) {
			showError("Vui lòng chọn phòng ban cần xóa.");
			return;
		}
		phongBanModel.remove(index);
		clearPhongBanForm();
		nhanVienModel.setRowCount(0);
		clearNhanVienForm();
		updateTongSoCongTy();
	}

	private void addNhanVien() {
		PhongBan phongBan = getSelectedPhongBan();
		if (phongBan == null) {
			return;
		}
		NhanVien nhanVien = readNhanVienForm();
		if (nhanVien == null) {
			return;
		}
		phongBan.getNhanVien().add(nhanVien);
		nhanVien.setNgayCong(new NgayCong(nhanVien, 0, 26));
		refreshNhanVienTable();
		phongBanList.repaint();
		updateTongSoCongTy();
		clearNhanVienForm();
	}

	private void deleteNhanVien() {
		PhongBan phongBan = getSelectedPhongBan();
		int viewIndex = nhanVienTable.getSelectedRow();
		if (phongBan == null || viewIndex < 0) {
			showError("Vui lòng chọn nhân viên cần xóa.");
			return;
		}
		int index = nhanVienTable.convertRowIndexToModel(viewIndex);
		NhanVien nhanVien = displayedNhanViens.get(index);
		phongBan.getNhanVien().remove(nhanVien);
		refreshNhanVienTable();
		phongBanList.repaint();
		updateTongSoCongTy();
		clearNhanVienForm();
	}

	private NhanVien readNhanVienForm() {
		if (maNhanVienField.getText().trim().isEmpty() || hoTenField.getText().trim().isEmpty()) {
			showError("Mã nhân viên và họ tên không được để trống.");
			return null;
		}
		Date birthDate;
		birthDate = ngaySinhField.getDate();
		if (birthDate == null) {
			showError("Ngày sinh phải có định dạng dd/MM/yyyy.");
			return null;
		}
		Date hireDate = ngayVaoLamField.getDate();
		if (hireDate == null) {
			showError("Ngày vào làm phải có định dạng dd/MM/yyyy.");
			return null;
		}
		if (hireDate.before(birthDate)) {
			showError("Ngày vào làm không được trước ngày sinh.");
			return null;
		}
		return new NhanVien(hoTenField.getText().trim(), birthDate,
				soDienThoaiField.getText().trim(), gioiTinhBox.getSelectedIndex() == 0,
				maNhanVienField.getText().trim(), (String) trangThaiBox.getSelectedItem(),
				viTriField.getText().trim(), queQuanField.getText().trim(), hireDate, null);
	}

	// TÍNH NĂNG 4: TÍNH NGÀY CÔNG & PHÂN LOẠI
	// Cập nhật số ngày đã làm và tính toán tỷ lệ % hoàn thành tự động
	private void calculateNgayCong() {
		PhongBan phongBan = getSelectedPhongBan();
		int viewIndex = nhanVienTable.getSelectedRow();
		if (phongBan == null || viewIndex < 0) {
			showError("Vui lòng chọn nhân viên cần tính ngày công.");
			return;
		}
		int index = nhanVienTable.convertRowIndexToModel(viewIndex);
		try {
			double soNgayDaLam = Double.parseDouble(ngayCongField.getText().trim());
			double soNgayChuan = Double.parseDouble(ngayChuanField.getText().trim());
			if (soNgayDaLam < 0 || soNgayChuan <= 0
					|| soNgayDaLam > soNgayChuan) {
				showError("Số ngày công không hợp lệ.");
				return;
			}
			NhanVien nhanVien = displayedNhanViens.get(index);
			NgayCong ngayCong = new NgayCong(nhanVien, soNgayDaLam, soNgayChuan);
			nhanVien.setNgayCong(ngayCong);
			refreshNhanVienTable();
			nhanVienTable.setRowSelectionInterval(viewIndex, viewIndex);
			JOptionPane.showMessageDialog(this,
					String.format("Ngày công: %.1f/%.1f (%.1f%%)\nPhân loại: %s", soNgayDaLam,
							soNgayChuan, ngayCong.tinhPhanTramHoanThanh(), ngayCong.getPhanLoai()),
						"Kết quả ngày công", JOptionPane.INFORMATION_MESSAGE);
		} catch (NumberFormatException exception) {
			showError("Ngày công và ngày chuẩn phải là số hợp lệ.");
		}
	}

	private void resetNgayCong() {
		PhongBan phongBan = getSelectedPhongBan();
		if (phongBan == null) {
			return;
		}
		int result = JOptionPane.showConfirmDialog(this,
				"Bạn có chắc muốn reset ngày công của toàn bộ phòng ban "
						+ phongBan.getTenPB() + "?",
				"Reset ngày công phòng ban", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (result == JOptionPane.YES_OPTION) {
			if (phongBan.getNhanVien() != null) {
				for (NhanVien nhanVien : phongBan.getNhanVien()) {
					nhanVien.setNgayCong(new NgayCong(nhanVien, 0, 26));
				}
			}
			refreshNhanVienTable();
			clearNhanCongFields();
		}
	}

	private void loadSelectedPhongBan() {
		PhongBan phongBan = phongBanList.getSelectedValue();
		if (phongBan == null) {
			clearPhongBanForm();
			nhanVienModel.setRowCount(0);
			return;
		}
		tenPhongBanField.setText(phongBan.getTenPB());
		truongPhongField.setText(phongBan.getIdTruongPhong());
		int tongNhanVien = (phongBan.getNhanVien() != null) ? phongBan.getNhanVien().size() : 0;
		tongSoNhanVienField.setText(String.valueOf(tongNhanVien));
		refreshNhanVienTable();
		clearNhanVienForm();
	}

	private void loadSelectedNhanVien() {
		int viewIndex = nhanVienTable.getSelectedRow();
		PhongBan phongBan = phongBanList.getSelectedValue();
		if (phongBan == null || viewIndex < 0) {
			return;
		}
		int index = nhanVienTable.convertRowIndexToModel(viewIndex);
		if (index >= phongBan.getNhanVien().size()) {
			return;
		}
		NhanVien nhanVien = displayedNhanViens.get(index);
		nhanVien.capNhatViTriTheoChucVuHienTai();
		maNhanVienField.setText(nhanVien.getId());
		hoTenField.setText(nhanVien.getFullName());
		ngaySinhField.setDate(nhanVien.getBirthDate());
		ngayVaoLamField.setDate(nhanVien.getNgayVaoLam());
		soDienThoaiField.setText(nhanVien.getSoDT());
		queQuanField.setText(nhanVien.getQueQuan());
		gioiTinhBox.setSelectedIndex(Boolean.TRUE.equals(nhanVien.getGioiTinh()) ? 0 : 1);
		trangThaiBox.setSelectedItem(nhanVien.getTrangThai() != null ? nhanVien.getTrangThai() : "Đang làm");
		viTriField.setText(nhanVien.getViTri());
		NgayCong ngayCong = nhanVien.getNgayCong();
		if (ngayCong == null) {
			ngayCongField.setText("");
			ngayChuanField.setText("26");
			ketQuaNgayCongLabel.setText("Chưa có dữ liệu ngày công");
		} else {
			ngayCongField.setText(String.valueOf(ngayCong.getSoNgayDaLam()));
			ngayChuanField.setText(String.valueOf(ngayCong.getSoNgayChuan()));
			ketQuaNgayCongLabel.setText(String.format("Kết quả: %.1f%% - Xếp loại: %s",
					ngayCong.tinhPhanTramHoanThanh(), ngayCong.getPhanLoai()));
		}
	}

	// TÍNH NĂNG 1 & 2: THỐNG KÊ TỔNG SỐ NHÂN VIÊN TOÀN CÔNG TY
	// Thuật toán vòng lặp quét qua tất cả phòng ban và cộng dồn số lượng nhân viên
	private void updateTongSoCongTy() {
		int total = 0;
		for (int i = 0; i < phongBanModel.getSize(); i++) {
			PhongBan pb = phongBanModel.getElementAt(i);
			if (pb.getNhanVien() != null) {
				total += pb.getNhanVien().size();
			}
		}
		tongSoCongTyLabel.setText("Tổng số nhân viên toàn công ty: " + total);
	}

	private void refreshNhanVienTable() {
		nhanVienModel.setRowCount(0);
		displayedNhanViens.clear();
		PhongBan phongBan = phongBanList.getSelectedValue();
		if (phongBan == null || phongBan.getNhanVien() == null) {
			return;
		}
		for (NhanVien nhanVien : phongBan.getNhanVien()) {
			displayedNhanViens.add(nhanVien);
			nhanVien.capNhatViTriTheoChucVuHienTai();
			NgayCong ngayCong = nhanVien.getNgayCong();
			nhanVienModel.addRow(new Object[] {
					nhanVien.getId(), nhanVien.getFullName(), formatDate(nhanVien.getBirthDate()),
					formatDate(nhanVien.getNgayVaoLam()), nhanVien.getSoDT(),
					Boolean.TRUE.equals(nhanVien.getGioiTinh()) ? "Nam" : "Nữ",
					nhanVien.getTrangThai(), nhanVien.getViTri(),
					ngayCong == null ? "Chưa tính" : String.format("%.1f/%.1f (%.1f%%) - %s",
							ngayCong.getSoNgayDaLam(), ngayCong.getSoNgayChuan(),
							ngayCong.tinhPhanTramHoanThanh(), ngayCong.getPhanLoai())
			});
		}
	}

	private PhongBan getSelectedPhongBan() {
		PhongBan phongBan = phongBanList.getSelectedValue();
		if (phongBan == null) {
			showError("Vui lòng chọn phòng ban trước.");
		}
		return phongBan;
	}

	private String formatDate(Date date) {
		return date == null ? "" : DATE_FORMAT.format(date);
	}

	private void clearPhongBanForm() {
		tenPhongBanField.setText("");
		truongPhongField.setText("");
		tongSoNhanVienField.setText("");
	}

	private void clearNhanVienForm() {
		maNhanVienField.setText("");
		hoTenField.setText("");
		ngaySinhField.setDate(null);
		ngayVaoLamField.setDate(null);
		soDienThoaiField.setText("");
		queQuanField.setText("");
		gioiTinhBox.setSelectedIndex(0);
		trangThaiBox.setSelectedIndex(0);
		viTriField.setText("");
		clearNhanCongFields();
		nhanVienTable.clearSelection();
	}

	private void clearNhanCongFields() {
		ngayCongField.setText("");
		ngayChuanField.setText("26");
		ketQuaNgayCongLabel.setText("Chưa có dữ liệu ngày công");
	}

	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message, "Dữ liệu không hợp lệ", JOptionPane.WARNING_MESSAGE);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new GUI().setVisible(true));
	}
}
