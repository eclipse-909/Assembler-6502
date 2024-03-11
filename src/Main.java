import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;

public class Main extends JFrame {
    private JTextArea textArea, lineNumberArea, addressArea, hexDumpArea, outputArea;
    private JPanel contentPane;
    private JScrollPane lineNumberScrollPane, addressScrollPane, hexDumpScrollPane, outputScrollPane;
    private File currentFile;

    public Main() {
        setTitle("6502 Assembler");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        contentPane = new JPanel(new BorderLayout());

        // Navigation bar
        JToolBar toolBar = new JToolBar();
        JButton newButton = new JButton("New");
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.setText("");
                addressArea.setText("");
                hexDumpArea.setText("");
                currentFile = null;
            }
        });
        toolBar.add(newButton);

        JButton openButton = new JButton("Open");
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });
        toolBar.add(openButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile(false);
            }
        });
        toolBar.add(saveButton);

        JButton saveAsButton = new JButton("Save As");
        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile(true);
            }
        });
        toolBar.add(saveAsButton);

        JButton assembleButton = new JButton("Assemble");
        assembleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                assembleCode();
            }
        });
        toolBar.add(assembleButton);

        contentPane.add(toolBar, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLineNumbers();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLineNumbers();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLineNumbers();
            }
        });
        JScrollPane scrollPane = new JScrollPane(textArea);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        lineNumberArea = new JTextArea("1");
        lineNumberArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lineNumberArea.setEditable(false);
        lineNumberArea.setBackground(Color.LIGHT_GRAY);
        lineNumberScrollPane = new JScrollPane(lineNumberArea);
        lineNumberScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        lineNumberScrollPane.setPreferredSize(new Dimension(40, textArea.getHeight()));
        contentPane.add(lineNumberScrollPane, BorderLayout.WEST);

        JPanel addressHexContainer = new JPanel();
        addressHexContainer.setLayout(new BoxLayout(addressHexContainer, BoxLayout.X_AXIS));

        addressArea = new JTextArea("0x0000");
        addressArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        addressArea.setEditable(false);
        addressArea.setBackground(Color.LIGHT_GRAY);
        addressScrollPane = new JScrollPane(addressArea);
        addressScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        addressScrollPane.setPreferredSize(new Dimension(60, textArea.getHeight()));
        addressHexContainer.add(addressScrollPane, BorderLayout.WEST);

        hexDumpArea = new JTextArea("00");
        hexDumpArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        hexDumpArea.setEditable(false);
        hexDumpArea.setBackground(Color.LIGHT_GRAY);
        hexDumpScrollPane = new JScrollPane(hexDumpArea);
        hexDumpScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        hexDumpScrollPane.setPreferredSize(new Dimension(80, textArea.getHeight()));
        addressHexContainer.add(hexDumpScrollPane, BorderLayout.EAST);

        contentPane.add(addressHexContainer, BorderLayout.EAST);

        JPanel outputContainer = new JPanel();
        outputContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection(outputArea.getText());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });
        outputContainer.add(copyButton, BorderLayout.SOUTH);

        outputArea = new JTextArea("");
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        outputScrollPane.setPreferredSize(new Dimension((int) (getWidth() * 0.8), 40));
        outputContainer.add(outputScrollPane, BorderLayout.CENTER);

        contentPane.add(outputContainer, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = getWidth(); // Get the width of the frame
                outputScrollPane.setPreferredSize(new Dimension((int) (width * 0.8), 40));
                revalidate(); // Revalidate the container to update the layout
            }
        });

        setContentPane(contentPane);
        setVisible(true);

        updateLineNumbers();
        syncScrollBars(scrollPane);
    }

    private void updateLineNumbers() {
        int totalLines = textArea.getLineCount();
        int digits = Math.max(String.valueOf(totalLines).length(), 2);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= totalLines; i++) {
            sb.append(String.format("%1$" + digits + "s", i)).append("\n");
        }
        lineNumberArea.setText(sb.toString());
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Assembly files (*.asm6502)", "asm6502"));
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.getName().endsWith(".asm6502")) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    textArea.setText(stringBuilder.toString());
                    currentFile = file;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a file with the .asm6502 extension.", "Invalid File Type", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile(boolean saveAs) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Assembly files (*.asm6502)", "asm6502"));
        if (!saveAs && currentFile != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
                writer.write(textArea.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            int returnValue = fileChooser.showSaveDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".asm6502")) {
                    file = new File(file.getAbsolutePath() + ".asm6502");
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(textArea.getText());
                    currentFile = file;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void assembleCode() {
        // Placeholder values for addresses and hex dump
        int totalLines = textArea.getLineCount();
        int addressDigits = 4;
        StringBuilder addressSb = new StringBuilder();
        StringBuilder hexDumpSb = new StringBuilder();
        StringBuilder outputSb = new StringBuilder();
        for (int i = 0; i < totalLines; i++) {
            addressSb.append(String.format("0x%04X", i * 3)).append("\n");
            hexDumpSb.append("00 00 00\n");
            outputSb.append("0x00, 0x00, 0x00, ");
        }
        outputSb.deleteCharAt(outputSb.length() - 1);
        outputSb.deleteCharAt(outputSb.length() - 1);
        addressArea.setText(addressSb.toString());
        hexDumpArea.setText(hexDumpSb.toString());
        outputArea.setText(outputSb.toString());
    }

    private void syncScrollBars(JScrollPane scrollPane) {
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        JScrollBar lineNumberBar = lineNumberScrollPane.getVerticalScrollBar();
        JScrollBar addressBar = addressScrollPane.getVerticalScrollBar();
        JScrollBar hexDumpBar = hexDumpScrollPane.getVerticalScrollBar();

        verticalBar.addAdjustmentListener(e -> {
            lineNumberBar.setValue(verticalBar.getValue());
            addressBar.setValue(verticalBar.getValue());
            hexDumpBar.setValue(verticalBar.getValue());
        });

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                lineNumberBar.setValue(verticalBar.getValue());
                addressBar.setValue(verticalBar.getValue());
                hexDumpBar.setValue(verticalBar.getValue());
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}