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
import repository.InMemoryNhanSuRepository;
import repository.NhanSuRepository;
import service.NgayCongService;
import service.NhanVienService;

public class GUI extends JFrame {
	private final NhanSuRepository repository;
	private final NhanVienService nhanVienService;
	private final NgayCongService ngayCongService;
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
	private final DefaultTableModel nhanVienModel = createFeature7InlineEditingModel();
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
		this(new InMemoryNhanSuRepository());
	}

	/** Factory để truyền cùng repository cho GUI và API trong giai đoạn sau. */
	public static GUI create(NhanSuRepository repository) {
		return new GUI(repository);
	}

	private GUI(NhanSuRepository repository) {
		super("Quản lý nhân sự công ty Thành Hưng");
		this.repository = java.util.Objects.requireNonNull(repository);
		this.nhanVienService = new NhanVienService(repository);
		this.ngayCongService = new NgayCongService(repository);
		UIManager.put("Panel.background", BACKGROUND);
		UIManager.put("OptionPane.background", BACKGROUND);
		UIManager.put("OptionPane.messageFont", APP_FONT);
		DATE_FORMAT.setLenient(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new java.awt.Dimension(1000, 620));
		buildInterface();
		reloadPhongBanModel();
		setExtendedState(JFrame.MAXIMIZED_BOTH);
	}

	public NhanVienService getNhanVienService() {
		return nhanVienService;
	}

	public NgayCongService getNgayCongService() {
		return ngayCongService;
	}

	private void reloadPhongBanModel() {
		phongBanModel.clear();
		for (PhongBan pb : repository.danhSachPhongBan()) phongBanModel.addElement(pb);
		if (!phongBanModel.isEmpty()) phongBanList.setSelectedIndex(0);
		updateTongSoCongTy();
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
		fields.add(new JLabel("Mã NV (theo mẫu NVxxx)"));
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
		
		setupFeature6SmartSorting();

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

	// =========================================================================
	// ==================== DANH SÁCH 10 TÍNH NĂNG CHÍNH =====================
	// =========================================================================

	// TÍNH NĂNG 1 & 2: THỐNG KÊ TỔNG SỐ NHÂN VIÊN TOÀN CÔNG TY
	// Thuật toán vòng lặp quét qua tất cả phòng ban và cộng dồn số lượng nhân viên
	private void updateTongSoCongTy() {
		int total = repository.tatCaNhanVien().size();
		tongSoCongTyLabel.setText("Tổng số nhân viên toàn công ty: " + total);
	}

	private void refreshNhanVienTable() {
		nhanVienModel.setRowCount(0);
		displayedNhanViens.clear();
		PhongBan phongBan = phongBanList.getSelectedValue();
		if (phongBan == null || phongBan.getNhanVien() == null) {
			return;
		}
		for (NhanVien nhanVien : repository.danhSachNhanVien(phongBan)) {
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
		repository.themPhongBan(phongBan);
		phongBanModel.addElement(phongBan);
		phongBanList.setSelectedValue(phongBan, true);
		clearPhongBanForm();
	}

	// Du lieu nen co dinh cho TX1 muc 2.2 - sheet DU_LIEU_NEN (58 test case).
	private void loadSampleData() {
		try {
			List<PhongBan> sampleDepartments = new ArrayList<>();

			// PB-KT: Phong Ky thuat - ba nhan vien co dinh.
			ArrayList<NhanVien> kyThuat = new ArrayList<>();

			NhanVien nv001 = new NhanVien("Nguyễn An", DATE_FORMAT.parse("01/01/2000"),
					"0900000001", true, "NV001", "Đang làm", "Nhân viên", "Quảng Ninh", null);
			nv001.setNgayVaoLam(DATE_FORMAT.parse("01/01/2022"));
			nv001.setNgayCong(new NgayCong(nv001, 0, 26));
			nv001.getLichSuChucVu().add(new ChucVu("Nhân viên", DATE_FORMAT.parse("01/01/2022"), null));
			nv001.capNhatViTriTheoChucVuHienTai();
			kyThuat.add(nv001);

			NhanVien nv002 = new NhanVien("Trần Bình", DATE_FORMAT.parse("01/01/1998"),
					"0900000002", true, "NV002", "Đang làm", "", "Hà Nội", null);
			nv002.setNgayVaoLam(DATE_FORMAT.parse("01/01/2024"));
			// No NgayCong assigned: GUI displays "Chưa tính".
			kyThuat.add(nv002);

			NhanVien nv003 = new NhanVien("Lê Lan", DATE_FORMAT.parse("01/01/2004"),
					"0900000003", false, "NV003", "Thử việc", "Thực tập sinh", "Hải Phòng", null);
			nv003.setNgayVaoLam(DATE_FORMAT.parse("01/01/2026"));
			nv003.setNgayCong(new NgayCong(nv003, 0, 26));
			nv003.getLichSuChucVu().add(new ChucVu("Thực tập sinh", DATE_FORMAT.parse("01/01/2026"), null));
			nv003.capNhatViTriTheoChucVuHienTai();
			kyThuat.add(nv003);

			sampleDepartments.add(new PhongBan("Kỹ thuật", "", kyThuat));

			// PB-KD: Phong Kinh doanh - kiem tra pham vi tim kiem va ma trung lien phong.
			ArrayList<NhanVien> kinhDoanh = new ArrayList<>();
			NhanVien nv100 = new NhanVien("Nguyễn Mai", DATE_FORMAT.parse("01/01/1995"),
					"0900000100", false, "NV100", "Đang làm", "Nhân viên kinh doanh", "Hà Nội", null);
			nv100.setNgayVaoLam(DATE_FORMAT.parse("01/01/2020"));
			nv100.setNgayCong(new NgayCong(nv100, 0, 26));
			nv100.getLichSuChucVu().add(new ChucVu("Nhân viên kinh doanh", DATE_FORMAT.parse("01/01/2020"), null));
			nv100.capNhatViTriTheoChucVuHienTai();
			kinhDoanh.add(nv100);
			sampleDepartments.add(new PhongBan("Kinh doanh", "", kinhDoanh));

			repository.thayThePhongBan(sampleDepartments);
			reloadPhongBanModel();
			phongBanList.repaint();
			updateTongSoCongTy();
		} catch (ParseException exception) {
			showError("Không thể khởi tạo dữ liệu mẫu.");
		}
	}


	private void deletePhongBan() {
		int index = phongBanList.getSelectedIndex();
		if (index < 0) {
			showError("Vui lòng chọn phòng ban cần xóa.");
			return;
		}
		repository.xoaPhongBan(phongBanModel.get(index));
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
		try {
			nhanVienService.themNhanVien(phongBan, nhanVien);
		} catch (IllegalArgumentException exception) {
			showError(exception.getMessage());
			return;
		}
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
		nhanVienService.xoaNhanVien(phongBan, nhanVien);
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
			NhanVien nhanVien = displayedNhanViens.get(index);
			NgayCong ngayCong = ngayCongService.capNhatNgayCong(nhanVien,
					ngayCongField.getText(), ngayChuanField.getText());
			refreshNhanVienTable();
			nhanVienTable.setRowSelectionInterval(viewIndex, viewIndex);
			JOptionPane.showMessageDialog(this,
					String.format("Ngày công: %.1f/%.1f (%.1f%%)\nPhân loại: %s", ngayCong.getSoNgayDaLam(),
							ngayCong.getSoNgayChuan(), ngayCong.tinhPhanTramHoanThanh(), ngayCong.getPhanLoai()),
						"Kết quả ngày công", JOptionPane.INFORMATION_MESSAGE);
		} catch (IllegalArgumentException exception) {
			showError(exception.getMessage());
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
			ngayCongService.resetPhongBan(phongBan);
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


	// TÍNH NĂNG 6: SẮP XẾP THÔNG MINH BẢNG DỮ LIỆU
	// Khởi tạo TableRowSorter để cho phép người dùng click tiêu đề cột để sắp xếp
	private void setupFeature6SmartSorting() {
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
	}

	// TÍNH NĂNG 7: CHỈNH SỬA TRỰC TIẾP TRÊN BẢNG (Inline-Editing)
	// Cho phép click đúp vào các ô của bảng để sửa dữ liệu nhanh chóng.
	private DefaultTableModel createFeature7InlineEditingModel() {
		return new DefaultTableModel(
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
			try {
			switch (column) {
				case 0: nhanVienService.doiMaNhanVien(nv, val); changed = true; break;
				case 1: nv.setFullName(val); changed = true; break;
				case 2:
					try {
						nhanVienService.capNhatNgaySinh(nv, DATE_FORMAT.parse(val));
						changed = true;
					} catch (java.text.ParseException e) {
						showError("Ngày sinh không hợp lệ (dd/MM/yyyy).");
						return;
					}
					break;
				case 3:
					try {
						nhanVienService.capNhatNgayVaoLam(nv, DATE_FORMAT.parse(val));
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
			} catch (IllegalArgumentException exception) {
				showError(exception.getMessage());
				return;
			}
			if (changed) {
				super.setValueAt(aValue, row, column);
			}
		}
	};
	}

	// TÍNH NĂNG 8: CHỈNH SỬA NHANH VỊ TRÍ (Shortcut nhảy Tab)
	// Cho phép click đúp vào cột Vị trí để mở thẳng sang Tab Lịch sử chức vụ của Hồ sơ
	private void quickEditCell(int viewRow, int viewCol) {
		if (viewRow < 0 || viewCol < 0) return;
		int modelRow = nhanVienTable.convertRowIndexToModel(viewRow);
		int modelCol = nhanVienTable.convertColumnIndexToModel(viewCol);
		NhanVien nv = displayedNhanViens.get(modelRow);
		
		try {
		switch (modelCol) {
			case 0:
				String newId = JOptionPane.showInputDialog(this, "Nhập Mã NV mới:", nv.getId());
				if (newId != null && !newId.trim().isEmpty()) {
					nhanVienService.doiMaNhanVien(nv, newId.trim());
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
						if (modelCol == 2) nhanVienService.capNhatNgaySinh(nv, newDate);
						else nhanVienService.capNhatNgayVaoLam(nv, newDate);
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
		} catch (IllegalArgumentException exception) {
			showError(exception.getMessage());
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


}
