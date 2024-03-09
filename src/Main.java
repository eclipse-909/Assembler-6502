import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame {
    private JTextArea editorTextArea;
    private JTextArea hexDumpTextArea;

    public Main() {
        setTitle("6502 Assembly Editor and Assembler");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        createMenuBar();
        createEditorPanel();
        createHexDumpPanel();

        setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newMenuItem = new JMenuItem("New");
        JMenuItem openMenuItem = new JMenuItem("Open");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        JMenuItem saveAsMenuItem = new JMenuItem("Save As");
        JMenuItem assembleMenuItem = new JMenuItem("Assemble");

        newMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editorTextArea.setText("");
                hexDumpTextArea.setText("");
            }
        });

        openMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Assembly Files", "asm");
                fileChooser.setFileFilter(filter);
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }
                        reader.close();
                        editorTextArea.setText(stringBuilder.toString());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showSaveDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        FileWriter writer = new FileWriter(selectedFile);
                        writer.write(editorTextArea.getText());
                        writer.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        saveAsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showSaveDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        FileWriter writer = new FileWriter(selectedFile);
                        writer.write(editorTextArea.getText());
                        writer.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        assembleMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Implement assembling logic here
            }
        });

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(assembleMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void createEditorPanel() {
        JPanel editorPanel = new JPanel(new BorderLayout());

        // Custom table model for the editor table
        EditorTableModel editorTableModel = new EditorTableModel();
        JTable editorTable = new JTable(editorTableModel);

        editorPanel.add(new JScrollPane(editorTable), BorderLayout.CENTER);

        add(editorPanel, BorderLayout.CENTER);
    }

    private void createHexDumpPanel() {
        JPanel hexDumpPanel = new JPanel(new BorderLayout());
        hexDumpTextArea = new JTextArea();
        hexDumpTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        hexDumpTextArea.setEditable(false);
        hexDumpPanel.add(new JScrollPane(hexDumpTextArea), BorderLayout.CENTER);

        add(hexDumpPanel, BorderLayout.EAST);
    }

    // Custom table model for the editor table
    private class EditorTableModel extends AbstractTableModel {
        private List<EditorRow> rows = new ArrayList<>();
        private String orgAddress = "0000"; // Default org address

        public EditorTableModel() {
            // Add a default row
            addRow(new EditorRow("", ""));
        }

        public void addRow(EditorRow row) {
            rows.add(row);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Line";
                case 1:
                    return "Address";
                case 2:
                    return "Label";
                case 3:
                    return "Assembly Code";
                case 4:
                    return "Hex Dump";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            EditorRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return rowIndex + 1; // Line number
                case 1:
                    return calculateAddress(rowIndex); // Memory address
                case 2:
                    return row.getLabel(); // Label
                case 3:
                    return row.getAssemblyCode(); // Assembly code
                case 4:
                    return ""; // Hex dump (to be calculated later)
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2 || columnIndex == 3; // Only allow editing labels and assembly code
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            EditorRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 2:
                    row.setLabel((String) value);
                    break;
                case 3:
                    row.setAssemblyCode((String) value);
                    break;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        private String calculateAddress(int rowIndex) {
            // Calculate the address based on orgAddress and the size of previous instructions
            // This is just a placeholder, you'll need to implement the actual logic
            return String.format("%04X", Integer.parseInt(orgAddress, 16) + rowIndex * 2);
        }
    }

    // Represents a row in the editor table
    private static class EditorRow {
        private String label;
        private String assemblyCode;

        public EditorRow(String label, String assemblyCode) {
            this.label = label;
            this.assemblyCode = assemblyCode;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getAssemblyCode() {
            return assemblyCode;
        }

        public void setAssemblyCode(String assemblyCode) {
            this.assemblyCode = assemblyCode;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Main();
            }
        });
    }
}