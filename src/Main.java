import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JFrame {
    private final JTextArea textArea, lineNumberArea, addressArea, hexDumpArea, outputArea;
    private final JScrollPane lineNumberScrollPane, addressScrollPane, hexDumpScrollPane, outputScrollPane;
    private File currentFile;

    public Main() {
        setTitle("6502 Assembler");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel(new BorderLayout());

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
        openButton.addActionListener(e -> openFile());
        toolBar.add(openButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveFile(false));
        toolBar.add(saveButton);

        JButton saveAsButton = new JButton("Save As");
        saveAsButton.addActionListener(e -> saveFile(true));
        toolBar.add(saveAsButton);

        JButton assembleButton = new JButton("Assemble");
        assembleButton.addActionListener(e -> assembleCode());
        toolBar.add(assembleButton);

        contentPane.add(toolBar, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {updateLineNumbers();}
            @Override public void removeUpdate(DocumentEvent e) {updateLineNumbers();}
            @Override public void changedUpdate(DocumentEvent e) {updateLineNumbers();}
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
                int width = getWidth();
                outputScrollPane.setPreferredSize(new Dimension((int) (width * 0.8), 40));
                revalidate();
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
        String code = textArea.getText();
        String[] lines = code.split("\\n");
        StringBuilder addressSb = new StringBuilder();
        StringBuilder hexDumpSb = new StringBuilder();
        StringBuilder outputSb = new StringBuilder();
        int address = 0;
        boolean startAddressFound = false;
        Map<String, Integer> foundLabels = new HashMap<>();//int represents the address of the label

        // First pass to get labels and directives and check for syntax errors
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            if (lines[lineNum].trim().isEmpty()) {
                addressSb.append("\n");
                continue;
            }
            String[] tokens = lines[lineNum].trim().split("\\s*[;].*|\\s+");

            // Check for comment
            if (tokens[0].isEmpty()) {
                addressSb.append("\n");
                continue;
            }

            // Get the start address
            if (!startAddressFound) {
                if (tokens[0].equalsIgnoreCase(".ORG")) {
                    try {
                        address = Integer.parseInt(tokens[1].substring(tokens[1].indexOf("$") + 1), 16);
                        startAddressFound = true;
                        if (tokens.length > 2) {
                            outputArea.setText("Error: Unrecognized token. Consider adding a ';' to make the following text a comment. Line " + lineNum);
                            return;
                        }
                        addressSb.append(String.format("0x%04X", address)).append("\n");
                        continue;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        outputArea.setText("Error: could not find start address. Line " + lineNum);
                        return;
                    }
                } else {
                    outputArea.setText("Error: could not find start address. Line " + lineNum);
                    return;
                }
            }

            // Check for end
            if (tokens[0].equalsIgnoreCase(".END")) {
                if (tokens.length > 1) {
                    outputArea.setText("Error: Unrecognized token. Consider adding a ';' to make the following text a comment. Line " + lineNum);
                    return;
                }
                addressSb.append("\n");
                break;
            }

            // Check for label
            if (tokens[0].endsWith(":")) {
                if (tokens.length > 1) {
                    outputArea.setText("Error: Unrecognized token. Consider adding a ';' to make the following text a comment. Line " + lineNum);
                    return;
                }
                String label = tokens[0].substring(0, tokens[0].length() - 1);
                foundLabels.put(label, address);
                addressSb.append("\n");
                continue;
            }

            // Parse instruction (first pass to verify opcodes and operand count)
            int instructionSize;
            switch (tokens[0].toUpperCase()) {
                case "LDA":
                case "LDX":
                case "LDY":
                    if (tokens.length == 2 && !tokens[1].matches("^\\d.*")) {
                        if (tokens[1].startsWith("#")) {
                            instructionSize = 2;
                        } else {
                            instructionSize = 3;
                        }
                    } else {
                        outputArea.setText("Error: invalid operand. Line " + lineNum);
                        return;
                    }
                    break;
                case "STA":
                case "ADC":
                case "CPX":
                case "INC":
                    if (tokens.length == 2 && !tokens[1].matches("^\\d.*")) {
                        instructionSize = 3;
                    } else {
                        outputArea.setText("Error: invalid operand. Line " + lineNum);
                        return;
                    }
                    break;
                case "BNE":
                    if (tokens.length == 2 && !tokens[1].matches("^\\d.*")) {
                        instructionSize = 2;
                    } else {
                        outputArea.setText("Error: invalid operand. Line " + lineNum);
                        return;
                    }
                    break;
                case "TXA":
                case "TYA":
                case "TAX":
                case "TAY":
                case "NOP":
                case "BRK":
                    if (tokens.length > 1) {
                        outputArea.setText("Error: instruction requires no operand. Line " + lineNum);
                        return;
                    }
                    instructionSize = 1;
                    break;
                case "SYS":
                    if (tokens.length == 1) {
                        instructionSize = 1;
                    } else if (tokens.length == 2 && !tokens[1].matches("^\\d.*")) {
                        instructionSize = 3;
                    } else {
                        outputArea.setText("Error: invalid operand. Line " + lineNum);
                        return;
                    }
                    break;
                case "DAT":
                    if (tokens.length == 2) {
                        instructionSize = (tokens[1].substring(1).length() + 1) / 2;
                    } else {
                        outputArea.setText("Error: invalid operand. Line " + lineNum);
                        return;
                    }
                    break;
                default:
                    outputArea.setText("Error: invalid instruction/token. Line " + lineNum);
                    return;
            }

            // Update the address
            addressSb.append(String.format("0x%04X", address)).append("\n");
            address += instructionSize;
        }

        addressSb.deleteCharAt(addressSb.length() - 1);

        // Second pass for assembling and mapping labels to addresses
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            if (lines[lineNum].trim().isEmpty()) {
                hexDumpSb.append("\n");
                continue;
            }
            String[] tokens = lines[lineNum].trim().split("\\s*[;].*|\\s+");

            // Ignore first pass checks for org, end, labels, and comments
            if (tokens[0].equalsIgnoreCase(".ORG") || tokens[0].equalsIgnoreCase(".END") || tokens[0].endsWith(":") || tokens[0].isEmpty()) {
                hexDumpSb.append("\n");
                continue;
            }

            List<Byte> lineHexDump = new ArrayList<>();

            // Parse instruction (second pass)
            try {
                switch (tokens[0].toUpperCase()) {
                    case "LDA":
                        if (tokens[1].startsWith("#")) {
                            lineHexDump.add((byte) 0xA9);
                            lineHexDump.add(parseConst(tokens));
                        } else if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xAD);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (foundLabels.containsKey(tokens[1])) {
                            lineHexDump.add((byte) 0xAD);
                            byte[] operands = parseAbsLabel(tokens, foundLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                            return;
                        }
                        break;
                    case "STA":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0x8D);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (foundLabels.containsKey(tokens[1])) {
                            lineHexDump.add((byte) 0x8D);
                            byte[] operands = parseAbsLabel(tokens, foundLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                            return;
                        }
                        break;
                    case "ADC":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0x6D);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (foundLabels.containsKey(tokens[1])) {
                            lineHexDump.add((byte) 0x6D);
                            byte[] operands = parseAbsLabel(tokens, foundLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                            return;
                        }
                        break;
                    case "LDX":
                        if (tokens[1].startsWith("#")) {
                            lineHexDump.add((byte) 0xA2);
                            lineHexDump.add(parseConst(tokens));
                        } else if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xAE);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (foundLabels.containsKey(tokens[1])) {
                            lineHexDump.add((byte) 0xAE);
                            byte[] operands = parseAbsLabel(tokens, foundLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                            return;
                        }
                        break;
                    case "LDY":
                        if (tokens[1].startsWith("#")) {
                            lineHexDump.add((byte) 0xA0);
                            lineHexDump.add(parseConst(tokens));
                        } else if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xAC);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (foundLabels.containsKey(tokens[1])) {
                            lineHexDump.add((byte) 0xAC);
                            byte[] operands = parseAbsLabel(tokens, foundLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                            return;
                        }
                        break;
                    case "CPX":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xEC);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (foundLabels.containsKey(tokens[1])) {
                            lineHexDump.add((byte) 0xEC);
                            byte[] operands = parseAbsLabel(tokens, foundLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                            return;
                        }
                        break;
                    case "BNE":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xD0);
                            lineHexDump.add(parseRelAddr(tokens));
                        } else if (foundLabels.containsKey(tokens[1])) {
                            lineHexDump.add((byte) 0xD0);
                            lineHexDump.add(parseRelLabel(tokens, foundLabels, address));
                        } else {
                            outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                            return;
                        }
                        break;
                    case "INC":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xEE);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (foundLabels.containsKey(tokens[1])) {
                            lineHexDump.add((byte) 0xEE);
                            byte[] operands = parseAbsLabel(tokens, foundLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                            return;
                        }
                        break;
                    case "SYS":
                        if (tokens.length > 1) {
                            if (tokens[1].startsWith("$")) {
                                lineHexDump.add((byte) 0xFF);
                                byte[] operands = parseAbsAddr(tokens);
                                lineHexDump.add(operands[0]);
                                lineHexDump.add(operands[1]);
                            } else if (foundLabels.containsKey(tokens[1])) {
                                lineHexDump.add((byte) 0xFF);
                                byte[] operands = parseAbsLabel(tokens, foundLabels);
                                lineHexDump.add(operands[0]);
                                lineHexDump.add(operands[1]);
                            } else {
                                outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                                return;
                            }
                        }
                        break;
                    case "DAT":
                        if (tokens[1].startsWith("$")) {
                            String hexString = tokens[1];
                            if (tokens[1].length() % 2 != 0) {
                                hexString = "0" + tokens[1];
                            }
                            String[] hexPairs = hexString.split("(?<=\\G..)");
                            byte[] byteArray = new byte[hexPairs.length];
                            for (int i = 0; i < hexPairs.length; i++) {
                                byteArray[i] = (byte) Integer.parseInt(hexPairs[i], 16);
                            }
                            for (byte b : byteArray) {
                                lineHexDump.add(b);
                            }
                        } else {
                            outputArea.setText("Error: invalid operand or label use. Line " + lineNum);
                            return;
                        }
                        break;
                    default:
                        outputArea.setText("Error: invalid instruction/token. Line " + lineNum);
                        return;
                }
            } catch (InvalidParameterException e) {
                outputArea.setText("Error: " + e.getMessage() + ". Line " + lineNum);
                return;
            }

            for (byte b : lineHexDump) {
                hexDumpSb.append(String.format("%02X", b)).append(" ");
                outputSb.append(String.format("0x%02X", b)).append(", ");
            }
            hexDumpSb.deleteCharAt(hexDumpSb.length() - 1);
            hexDumpSb.append("\n");
        }
        hexDumpSb.deleteCharAt(hexDumpSb.length() - 1);
        outputSb.deleteCharAt(hexDumpSb.length() - 1);
        outputSb.deleteCharAt(hexDumpSb.length() - 1);

        // Update the address area and hex dump area
        addressArea.setText(addressSb.toString());
        hexDumpArea.setText(hexDumpSb.toString());
        outputArea.setText(outputSb.toString());
    }

    private byte parseConst(String[] tokens) throws InvalidParameterException {
        String operand = tokens[1].substring(tokens[1].indexOf("$") + 1);
        if (operand.length() == 2) {
            return Byte.parseByte(operand, 16);
        }
        throw new InvalidParameterException("Invalid operand length.");
    }

    private byte parseRelAddr(String[] tokens) throws InvalidParameterException {
        String operand = tokens[1].substring(tokens[1].indexOf("$") + 1);
        if (operand.length() == 2) {
            return Byte.parseByte(operand, 16);
        }
        throw new InvalidParameterException("Invalid operand length.");
    }

    private byte[] parseAbsAddr(String[] tokens) throws InvalidParameterException {
        String operand = tokens[1].substring(tokens[1].indexOf("$") + 1);
        if (operand.length() == 4) {
            int intValue = Integer.parseInt(operand, 16);
            return new byte[] {(byte) (intValue & 0xFF), (byte) ((intValue >> 8) & 0xFF)};
        }
        throw new InvalidParameterException("Invalid operand length.");
    }

    private byte parseRelLabel(String[] tokens, Map<String, Integer> foundLabels, int address) throws InvalidParameterException {
        if (!foundLabels.containsKey(tokens[1])) {
            throw new InvalidParameterException("Label not found.");
        } else {
            int difference = foundLabels.get(tokens[1]) - (address + 2);
            if (difference < -128 || difference > 127) {
                throw new InvalidParameterException("Target address is too far for relative addressing.");
            }
            return (byte) difference;
        }
    }

    private byte[] parseAbsLabel(String[] tokens, Map<String, Integer> foundLabels) throws InvalidParameterException {
        if (!foundLabels.containsKey(tokens[1])) {
            throw new InvalidParameterException("Label not found.");
        } else {
            int intValue = foundLabels.get(tokens[1]);
            return new byte[] {(byte) (intValue & 0xFF), (byte) ((intValue >> 8) & 0xFF)};
        }
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