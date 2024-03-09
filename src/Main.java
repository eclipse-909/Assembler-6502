import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main {
    private JFrame frame;
    private JTable codeTable;
    private JTextArea machineCodeTextArea;
    private JButton createButton;
    private JButton openButton;
    private JButton saveButton;
    private JButton saveAsButton;
    private JButton assembleButton;

    private boolean editingMode = false;

    public Main() {
        frame = new JFrame("Assembly Text Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Create top navigation bar with buttons
        JPanel topPanel = new JPanel();
        createButton = new JButton("Create");
        openButton = new JButton("Open");
        saveButton = new JButton("Save");
        saveAsButton = new JButton("Save As");
        assembleButton = new JButton("Assemble");
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEditingMode(true);
            }
        });
        topPanel.add(createButton);
        topPanel.add(openButton);
        topPanel.add(saveButton);
        topPanel.add(saveAsButton);
        topPanel.add(assembleButton);
        frame.add(topPanel, BorderLayout.NORTH);

        // Create table for displaying assembly code with line numbers, addresses, labels, code, and machine code
        DefaultTableModel model = new DefaultTableModel(new String[]{"Line", "Address", "Label", "Code", "Machine Code"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing for Label and Code columns only when in editing mode
                return editingMode && (column == 2 || column == 3);
            }
        };
        codeTable = new JTable(model);
        codeTable.getColumnModel().getColumn(0).setPreferredWidth(50); // Line number
        codeTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Address
        codeTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Label
        codeTable.getColumnModel().getColumn(3).setPreferredWidth(200); // Code
        codeTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Machine code
        JScrollPane tableScrollPane = new JScrollPane(codeTable);
        frame.add(tableScrollPane, BorderLayout.CENTER);

        // Create bottom text area for displaying machine code after assembly
        machineCodeTextArea = new JTextArea();
        machineCodeTextArea.setEditable(false);
        JScrollPane machineCodeScrollPane = new JScrollPane(machineCodeTextArea);
        frame.add(machineCodeScrollPane, BorderLayout.SOUTH);

        setEditingMode(false); // Set initial editing mode

        frame.setVisible(true);
    }

    private void setEditingMode(boolean editing) {
        editingMode = editing;
        saveButton.setEnabled(editing);
        saveAsButton.setEnabled(editing);
        assembleButton.setEnabled(editing);
        codeTable.setEnabled(editing);
        codeTable.getTableHeader().setVisible(editing); // Show/hide table headers
        if (editing) {
            // Add one or two rows when entering editing mode
            DefaultTableModel model = (DefaultTableModel) codeTable.getModel();
            model.setRowCount(2); // Set initial row count
        } else {
            // Remove all rows when not editing
            ((DefaultTableModel) codeTable.getModel()).setRowCount(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}