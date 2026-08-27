package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CalendarDateField extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    static {
        DATE_FORMAT.setLenient(false);
    }
    private final JTextField textField = new JTextField();
    private final JButton clearButton;
    private Date date;
    private boolean updatingText;

    public CalendarDateField(Date initialDate, boolean optional) {
        setLayout(new BorderLayout(4, 0));
        setDate(initialDate);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateDateFromText();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateDateFromText();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateDateFromText();
            }
        });

        JButton calendarButton = new JButton("Lịch");
        calendarButton.addActionListener(event -> showCalendar());
        add(textField, BorderLayout.CENTER);
        add(calendarButton, BorderLayout.EAST);

        clearButton = new JButton("Xóa");
        clearButton.setVisible(optional);
        clearButton.addActionListener(event -> setDate(null));
        add(clearButton, BorderLayout.WEST);
    }

    public Date getDate() {
        Date typedDate = parseText(textField.getText());
        return typedDate == null ? null : new Date(typedDate.getTime());
    }

    public void setDate(Date value) {
        date = value == null ? null : new Date(value.getTime());
        updatingText = true;
        textField.setText(date == null ? "" : DATE_FORMAT.format(date));
        updatingText = false;
    }

    private void showCalendar() {
        Calendar displayedMonth = Calendar.getInstance();
        Date typedDate = parseText(textField.getText());
        displayedMonth.setTime(typedDate == null ? new Date() : typedDate);
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);

        JDialog dialog = new JDialog((Dialog) null, "Chọn ngày", true);
        JPanel calendarPanel = new JPanel(new BorderLayout(6, 6));
        calendarPanel.setBackground(new Color(245, 247, 246));
        calendarPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel header = new JPanel(new BorderLayout());
        JButton previousButton = new JButton("Tháng trước");
        JButton nextButton = new JButton("Tháng sau");
        JLabel monthLabel = new JLabel("", JLabel.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        styleButton(previousButton);
        styleButton(nextButton);
        header.add(previousButton, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextButton, BorderLayout.EAST);
        calendarPanel.add(header, BorderLayout.NORTH);

        JPanel daysPanel = new JPanel(new GridLayout(0, 7, 3, 3));
        calendarPanel.add(daysPanel, BorderLayout.CENTER);
        Runnable refresh = () -> refreshCalendar(dialog, displayedMonth, monthLabel, daysPanel);
        previousButton.addActionListener(event -> {
            displayedMonth.add(Calendar.MONTH, -1);
            refresh.run();
        });
        nextButton.addActionListener(event -> {
            displayedMonth.add(Calendar.MONTH, 1);
            refresh.run();
        });
        refresh.run();

        dialog.add(calendarPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateDateFromText() {
        if (!updatingText) {
            Date typedDate = parseText(textField.getText());
            if (typedDate != null || textField.getText().trim().isEmpty()) {
                date = typedDate;
            }
        }
    }

    private Date parseText(String text) {
        text = text.trim();
        if (!text.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return null;
        }
        ParsePosition position = new ParsePosition(0);
        Date parsedDate = DATE_FORMAT.parse(text, position);
        return parsedDate != null && position.getIndex() == text.length()
                ? parsedDate : null;
    }

    private void refreshCalendar(JDialog dialog, Calendar month, JLabel monthLabel, JPanel daysPanel) {
        monthLabel.setText(String.format("Tháng %d/%d", month.get(Calendar.MONTH) + 1,
                month.get(Calendar.YEAR)));
        daysPanel.removeAll();
        String[] weekdays = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
        for (String weekday : weekdays) {
            daysPanel.add(new JLabel(weekday, JLabel.CENTER));
        }

        Calendar firstDay = (Calendar) month.clone();
        int firstWeekday = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
        if (firstWeekday < 0) {
            firstWeekday += 7;
        }
        for (int i = 0; i < firstWeekday; i++) {
            daysPanel.add(new JLabel());
        }
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= daysInMonth; day++) {
            final int selectedDay = day;
            JButton dayButton = new JButton(String.valueOf(day));
            styleDayButton(dayButton);
            dayButton.addActionListener(event -> {
                Calendar selectedDate = (Calendar) month.clone();
                selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay);
                setDate(selectedDate.getTime());
                dialog.dispose();
            });
            daysPanel.add(dayButton);
        }
        daysPanel.revalidate();
        daysPanel.repaint();
        dialog.pack();
    }

    private void styleButton(JButton button) {
        button.setUI(new ModernButtonUI());
        button.setBackground(new Color(178, 223, 219));
        button.setForeground(Color.BLACK);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setRolloverEnabled(false);
    }

    private void styleDayButton(JButton button) {
        button.setUI(new ModernButtonUI());
        button.setBackground(new Color(224, 242, 241));
        button.setForeground(Color.BLACK);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setRolloverEnabled(false);
    }
}
