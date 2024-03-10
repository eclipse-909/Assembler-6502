import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Main extends JFrame {
    private JTextArea textArea;
    private JTextArea lineNumberArea;
    private JTextArea addressArea;
    private JPanel contentPane;
    private JScrollPane lineNumberScrollPane;
    private JScrollPane addressScrollPane;

    public Main() {
        setTitle("Simple Text Editor");
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
                // Add action for assembling code
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
                updateAddresses();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLineNumbers();
                updateAddresses();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLineNumbers();
                updateAddresses();
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

        addressArea = new JTextArea("0x0000");
        addressArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        addressArea.setEditable(false);
        addressArea.setBackground(Color.LIGHT_GRAY);
        addressScrollPane = new JScrollPane(addressArea);
        addressScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        addressScrollPane.setPreferredSize(new Dimension(60, textArea.getHeight()));
        contentPane.add(addressScrollPane, BorderLayout.EAST);

        setContentPane(contentPane);
        setVisible(true);

        updateLineNumbers();
        updateAddresses();
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

    private void updateAddresses() {
        int totalLines = textArea.getLineCount();
        int addressDigits = 4;
        StringBuilder addressSb = new StringBuilder();
        for (int i = 0; i < totalLines; i++) {
            addressSb.append(String.format("0x%04X", i * 3)).append("\n");
        }
        addressArea.setText(addressSb.toString());
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                textArea.setText(stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile(boolean saveAs) {
        JFileChooser fileChooser = new JFileChooser();
        if (!saveAs) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(textArea.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            int returnValue = fileChooser.showSaveDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(textArea.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void syncScrollBars(JScrollPane scrollPane) {
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        JScrollBar lineNumberBar = lineNumberScrollPane.getVerticalScrollBar();
        JScrollBar addressBar = addressScrollPane.getVerticalScrollBar();

        verticalBar.addAdjustmentListener(e -> {
            lineNumberBar.setValue(verticalBar.getValue());
            addressBar.setValue(verticalBar.getValue());
        });

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                lineNumberBar.setValue(verticalBar.getValue());
                addressBar.setValue(verticalBar.getValue());
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}