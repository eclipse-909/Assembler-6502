import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**The entire application; GUI plus backend logic.*/
public class Main extends JFrame {
    /**Text area.*/
    private final JTextArea textArea, lineNumberArea, addressArea, hexDumpArea, outputArea;
    /**Scroll pane for text area.*/
    private final JScrollPane textScrollPane, lineNumberScrollPane, addressScrollPane, hexDumpScrollPane;
    /**Tracks the open file to allow Save when Save As isn't necessary.*/
    private File currentFile;
    /**Tracks the line number the caret is on.*/
    private static int startOffset = -1, lineHeight = -1;

    /**Constructor for the window.*/
    public Main() {
        setTitle("6502 Assembler");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel(new BorderLayout());

        // Navigation bar
        JToolBar toolBar = new JToolBar();
        toolBar.setBackground(Color.LIGHT_GRAY);
        JButton newButton = new JButton("New");
        newButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
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

        // Create the main text area
        textArea = new JTextArea() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (startOffset >= 0 && lineHeight > 0) {
                    g.setColor(new Color(150, 150, 150, 50));
                    g.fillRect(0, startOffset, getWidth(), lineHeight);
                }
            }
        };
        textArea.setTabSize(4);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        textArea.setBackground(Color.DARK_GRAY);
        textArea.setForeground(Color.LIGHT_GRAY);
        textArea.setCaretColor(Color.LIGHT_GRAY);
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {updateLineNumbers();}
            @Override public void removeUpdate(DocumentEvent e) {updateLineNumbers();}
            @Override public void changedUpdate(DocumentEvent e) {updateLineNumbers();}
        });
        textArea.addCaretListener(e -> highlightLine());

        // Create the line number area
        lineNumberArea = new JTextArea("1") {@Override public void scrollRectToVisible(Rectangle aRect) {}};
        lineNumberArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        lineNumberArea.setEditable(false);
        lineNumberArea.setBackground(Color.DARK_GRAY);
        lineNumberArea.setForeground(Color.LIGHT_GRAY);
        // Create the addresses area
        addressArea = new JTextArea("0x0000") {@Override public void scrollRectToVisible(Rectangle aRect) {}};
        addressArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        addressArea.setEditable(false);
        addressArea.setBackground(Color.DARK_GRAY);
        addressArea.setForeground(Color.LIGHT_GRAY);
        // Create the hex dump area
        hexDumpArea = new JTextArea("00") {@Override public void scrollRectToVisible(Rectangle aRect) {}};
        hexDumpArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        hexDumpArea.setEditable(false);
        hexDumpArea.setBackground(Color.DARK_GRAY);
        hexDumpArea.setForeground(Color.LIGHT_GRAY);
        // Organize the components
        textScrollPane = new JScrollPane(textArea);
        lineNumberScrollPane = new JScrollPane(lineNumberArea);
        addressScrollPane = new JScrollPane(addressArea);
        addressScrollPane.setPreferredSize(new Dimension(80, textScrollPane.getHeight()));
        hexDumpScrollPane = new JScrollPane(hexDumpArea);

        textScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        lineNumberScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        lineNumberScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        addressScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        addressScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        hexDumpScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        AdjustmentListener adjustmentListener = e -> {
            int value = e.getValue();
            textScrollPane.getVerticalScrollBar().setValue(value);
            lineNumberScrollPane.getVerticalScrollBar().setValue(value);
            addressScrollPane.getVerticalScrollBar().setValue(value);
            hexDumpScrollPane.getVerticalScrollBar().setValue(value);
        };

        textScrollPane.getVerticalScrollBar().addAdjustmentListener(adjustmentListener);
        lineNumberScrollPane.getVerticalScrollBar().addAdjustmentListener(adjustmentListener);
        addressScrollPane.getVerticalScrollBar().addAdjustmentListener(adjustmentListener);
        hexDumpScrollPane.getVerticalScrollBar().addAdjustmentListener(adjustmentListener);

        JPanel left = new JPanel(new BorderLayout()), right = new JPanel(new BorderLayout());
        left.add(lineNumberScrollPane, BorderLayout.WEST);
        left.add(textScrollPane, BorderLayout.CENTER);
        right.add(addressScrollPane, BorderLayout.WEST);
        right.add(hexDumpScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        splitPane.setUI(new BasicSplitPaneUI() {
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override public void setBorder(Border b) {}
                    @Override public void paint(Graphics g) {
                        g.setColor(Color.LIGHT_GRAY);
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        });
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setResizeWeight(0.7);

        contentPane.add(splitPane, BorderLayout.CENTER);

        // Output area
        JPanel outputContainer = new JPanel();
        outputContainer.setBackground(Color.LIGHT_GRAY);
        outputContainer.setLayout(new BoxLayout(outputContainer, BoxLayout.X_AXIS));
        outputContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection(outputArea.getText());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });
        outputContainer.add(copyButton, BorderLayout.SOUTH);

        outputArea = new JTextArea("") {@Override public void scrollRectToVisible(Rectangle aRect) {}};
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        outputArea.setEditable(false);
        outputArea.setBackground(Color.DARK_GRAY);
        outputArea.setForeground(Color.lightGray);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        outputScrollPane.setPreferredSize(new Dimension(getWidth() - 20, 50));
        outputContainer.add(outputScrollPane, BorderLayout.CENTER);

        contentPane.add(outputContainer, BorderLayout.SOUTH);

        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK);
        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(saveKeyStroke, "save");
        Action saveAction = new AbstractAction() {@Override public void actionPerformed(ActionEvent e) {saveFile(false);}};

        textArea.getActionMap().put("save", saveAction);

        setContentPane(contentPane);
        setVisible(true);

        updateLineNumbers();
    }

    /**Update the line numbers and clear the addresses and hex when the code is changed.*/
    private void updateLineNumbers() {
        int totalLines = textArea.getLineCount();
        int digits = Math.max(String.valueOf(totalLines).length(), 2);
        StringBuilder lineNumberSb = new StringBuilder(), addressSb = new StringBuilder(), hexSb = new StringBuilder();
        for (int i = 1; i <= totalLines; i++) {
            lineNumberSb.append(String.format("%1$" + digits + "s", i));
            if (i < totalLines) {
                lineNumberSb.append("\n");
                addressSb.append("\n");
                hexSb.append("\n");
            }
        }
        lineNumberArea.setText(lineNumberSb.toString());
        addressArea.setText(addressSb.toString());
        hexDumpArea.setText(hexSb.toString());
        revalidate();
    }

    /**Darkens the selected line to make it more clear.*/
    private void highlightLine() {
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int lineHeight = fm.getHeight();
        int caretPosition = textArea.getCaretPosition();
        int startOffset = textArea.getDocument().getDefaultRootElement().getElementIndex(caretPosition) * lineHeight;
        textArea.repaint();
        setLineHighlight(textArea, startOffset, lineHeight);
    }

    /**Sets tne location for the line highlighting.*/
    private static void setLineHighlight(JTextArea area, int start, int height) {
        startOffset = start;
        lineHeight = height;
        area.repaint();
    }

    /**Opens a file from disk and displays the assembly content.*/
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
                        stringBuilder.append(line);
                        if (reader.ready()) {
                            stringBuilder.append("\n");
                        }
                    }
                    textArea.setText(stringBuilder.toString());
                    currentFile = file;
                    SwingUtilities.invokeLater(() -> textArea.setCaretPosition(0));
                } catch (IOException e) {
                    outputArea.setText("I/O Error: could not open file.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a file with the .asm6502 extension.", "Invalid File Type", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**Saves the current assembly code to a file on disk.*/
    private void saveFile(boolean saveAs) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Assembly files (*.asm6502)", "asm6502"));
        if (!saveAs && currentFile != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
                writer.write(textArea.getText());
            } catch (IOException e) {
                outputArea.setText("I/O Error: could save as.");
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
                    outputArea.setText("I/O Error: could save file.");
                }
            }
        }
    }

    /**When you click the Assemble button, this function is called.
     * This function will either give an error in the output if something went wrong
     * or it will display corresponding addresses and hex for each line, and the resulting hex dump in the output.*/
    private void assembleCode() {
        String code = textArea.getText();
        if (code.isEmpty()) {
            outputArea.setText("Assembling Error: nothing to assemble.");
            return;
        }
        String[] lines = code.split("\\n");
        StringBuilder addressSb = new StringBuilder();
        StringBuilder hexDumpSb = new StringBuilder();
        StringBuilder outputSb = new StringBuilder();
        int address = 0;
        boolean startAddressFound = false;
        int endLine = textArea.getLineCount();//the first line that has nothing to assemble
        LabelList declaredLabels = new LabelList();
        LabelList calledLabels = new LabelList();

        // First pass to get labels and directives and check for syntax errors
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            if (lines[lineNum].trim().isEmpty()) {
                addressSb.append("\n");
                continue;
            }
            String[] tokens = lines[lineNum].trim().split("\\s*[;].*|\\s+");

            // Check for comment or empty line
            if (tokens.length == 0 || tokens[0].isEmpty()) {
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
                            outputArea.setText("Assembling Error: Unrecognized token. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
                            return;
                        }
                        addressSb.append("\n");
                        continue;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        outputArea.setText("Assembling Error: could not find start address. Line " + (lineNum + 1));
                        return;
                    }
                } else {
                    outputArea.setText("Assembling Error: could not find start address. Line " + (lineNum + 1));
                    return;
                }
            }

            // Check for end
            if (tokens[0].equalsIgnoreCase(".END")) {
                if (tokens.length > 1) {
                    outputArea.setText("Assembling Error: Unrecognized token. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
                    return;
                }
                if (lineNum < textArea.getLineCount() - 2) {
                    addressSb.append("\n");
                }
                continue;
            }

            // Check for label
            if (tokens[0].endsWith(":")) {
                if (tokens.length == 2 && tokens[1].startsWith("+$")) {
                    try {
                        byte offset = Byte.parseByte(tokens[1].substring(2), 16);
                        String label = tokens[0].substring(0, tokens[0].length() - 1);
                        declaredLabels.add(label, lineNum, address + offset);
                        addressSb.append("\n");
                        continue;
                    } catch (NumberFormatException e) {
                        outputArea.setText("Assembling Error: label offset must be formatted as '+$##' in hex. Line " + (lineNum + 1));
                        return;
                    }
                } else if (tokens.length > 2) {
                    outputArea.setText("Assembling Error: Unrecognized token. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
                    return;
                }
                String label = tokens[0].substring(0, tokens[0].length() - 1);
                int line = declaredLabels.lineNumberOf(label);
                if (line != -1) {
                    outputArea.setText("Assembling Error: Duplicate label declared. Lines " + (lineNum + 1) + " & " + (line + 1));
                    return;
                }
                declaredLabels.add(label, lineNum, address);
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
                        if (tokens[1].startsWith("#$")) {
                            instructionSize = 2;
                        } else {
                            instructionSize = 3;
                        }
                    } else {
                        outputArea.setText("Assembling Error: invalid operand. Line " + (lineNum + 1));
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
                        outputArea.setText("Assembling Error: invalid operand. Line " + (lineNum + 1));
                        return;
                    }
                    break;
                case "BNE":
                    if (tokens.length == 2 && !tokens[1].matches("^\\d.*")) {
                        instructionSize = 2;
                        if (!tokens[1].startsWith("$")) {
                            calledLabels.add(tokens[1], lineNum, address + 1);
                        }
                    } else {
                        outputArea.setText("Assembling Error: invalid operand. Line " + (lineNum + 1));
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
                        outputArea.setText("Assembling Error: instruction requires no operand. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
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
                        outputArea.setText("Assembling Error: invalid operand. Line " + (lineNum + 1));
                        return;
                    }
                    break;
                case "DAT":
                    if (tokens.length == 2) {
                        instructionSize = (tokens[1].substring(1).length() + 1) / 2;
                    } else {
                        outputArea.setText("Assembling Error: invalid operand. Line " + (lineNum + 1));
                        return;
                    }
                    break;
                default:
                    outputArea.setText("Assembling Error: invalid instruction/token. Line " + (lineNum + 1));
                    return;
            }

            // Update the address
            endLine = lineNum + 1;
            addressSb.append(String.format("0x%04X", address));
            if (lineNum < textArea.getLineCount() - 2) {
                addressSb.append("\n");
            }
            address += instructionSize;
        }

        // Second pass for assembling and mapping labels to addresses
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            if (lines[lineNum].trim().isEmpty()) {
                hexDumpSb.append("\n");
                continue;
            }
            String[] tokens = lines[lineNum].trim().split("\\s*[;].*|\\s+");

            // Ignore first pass checks for empty lines, org, end, labels, and comments
            if (tokens.length == 0 || tokens[0].equalsIgnoreCase(".ORG") || tokens[0].equalsIgnoreCase(".END") || tokens[0].endsWith(":") || tokens[0].isEmpty()) {
                if (lineNum < textArea.getLineCount() - 2) {
                    hexDumpSb.append("\n");
                }
                continue;
            }

            List<Byte> lineHexDump = new ArrayList<>();

            // Parse instruction (second pass)
            try {
                switch (tokens[0].toUpperCase()) {
                    case "LDA":
                        if (tokens[1].startsWith("#$")) {
                            lineHexDump.add((byte) 0xA9);
                            lineHexDump.add(parseConst(tokens));
                        } else if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xAD);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (declaredLabels.addressOfFirst(tokens[1]) != -1) {
                            lineHexDump.add((byte) 0xAD);
                            byte[] operands = parseAbsLabel(tokens, declaredLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Assembling Error: invalid operand or label use. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "STA":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0x8D);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (declaredLabels.addressOfFirst(tokens[1]) != -1) {
                            lineHexDump.add((byte) 0x8D);
                            byte[] operands = parseAbsLabel(tokens, declaredLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Assembling Error: invalid operand or label use. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "ADC":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0x6D);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (declaredLabels.addressOfFirst(tokens[1]) != -1) {
                            lineHexDump.add((byte) 0x6D);
                            byte[] operands = parseAbsLabel(tokens, declaredLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Assembling Error: invalid operand or label use. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "LDX":
                        if (tokens[1].startsWith("#$")) {
                            lineHexDump.add((byte) 0xA2);
                            lineHexDump.add(parseConst(tokens));
                        } else if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xAE);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (declaredLabels.addressOfFirst(tokens[1]) != -1) {
                            lineHexDump.add((byte) 0xAE);
                            byte[] operands = parseAbsLabel(tokens, declaredLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Assembling Error: invalid operand or label use. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "LDY":
                        if (tokens[1].startsWith("#$")) {
                            lineHexDump.add((byte) 0xA0);
                            lineHexDump.add(parseConst(tokens));
                        } else if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xAC);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (declaredLabels.addressOfFirst(tokens[1]) != -1) {
                            lineHexDump.add((byte) 0xAC);
                            byte[] operands = parseAbsLabel(tokens, declaredLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Assembling Error: invalid operand or label use. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "CPX":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xEC);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (declaredLabels.addressOfFirst(tokens[1]) != -1) {
                            lineHexDump.add((byte) 0xEC);
                            byte[] operands = parseAbsLabel(tokens, declaredLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Assembling Error: invalid operand or label use. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "BNE":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xD0);
                            lineHexDump.add(parseRelAddr(tokens));
                        } else if (declaredLabels.addressOfFirst(tokens[1]) != -1) {
                            lineHexDump.add((byte) 0xD0);
                            lineHexDump.add(parseRelLabel(tokens, declaredLabels, calledLabels, lineNum));
                        } else {
                            outputArea.setText("Assembling Error: invalid operand or label use. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "INC":
                        if (tokens[1].startsWith("$")) {
                            lineHexDump.add((byte) 0xEE);
                            byte[] operands = parseAbsAddr(tokens);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else if (declaredLabels.addressOfFirst(tokens[1]) != -1) {
                            lineHexDump.add((byte) 0xEE);
                            byte[] operands = parseAbsLabel(tokens, declaredLabels);
                            lineHexDump.add(operands[0]);
                            lineHexDump.add(operands[1]);
                        } else {
                            outputArea.setText("Assembling Error: invalid operand or label use. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "SYS":
                        lineHexDump.add((byte) 0xFF);
                        if (tokens.length > 1) {
                            if (tokens[1].startsWith("$")) {
                                byte[] operands = parseAbsAddr(tokens);
                                lineHexDump.add(operands[0]);
                                lineHexDump.add(operands[1]);
                            } else if (declaredLabels.addressOfFirst(tokens[1]) != -1) {
                                byte[] operands = parseAbsLabel(tokens, declaredLabels);
                                lineHexDump.add(operands[0]);
                                lineHexDump.add(operands[1]);
                            } else {
                                outputArea.setText("Assembling Error: invalid operand or label use. Line " + (lineNum + 1));
                                return;
                            }
                        }
                        break;
                    case "DAT":
                        if (tokens[1].startsWith("$")) {
                            String hexString = tokens[1].substring(1);
                            if (hexString.length() % 2 != 0) {
                                hexString = "0" + hexString;
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
                            outputArea.setText("Assembling Error: invalid operand. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "TXA":
                        if (tokens.length == 1) {
                            lineHexDump.add((byte) 0x8A);
                        } else {
                            outputArea.setText("Assembling Error: instruction requires no operand. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "TYA":
                        if (tokens.length == 1) {
                            lineHexDump.add((byte) 0x98);
                        } else {
                            outputArea.setText("Assembling Error: instruction requires no operand. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "TAX":
                        if (tokens.length == 1) {
                            lineHexDump.add((byte) 0xAA);
                        } else {
                            outputArea.setText("Assembling Error: instruction requires no operand. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "TAY":
                        if (tokens.length == 1) {
                            lineHexDump.add((byte) 0xA8);
                        } else {
                            outputArea.setText("Assembling Error: instruction requires no operand. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "NOP":
                        if (tokens.length == 1) {
                            lineHexDump.add((byte) 0xEA);
                        } else {
                            outputArea.setText("Assembling Error: instruction requires no operand. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    case "BRK":
                        if (tokens.length == 1) {
                            lineHexDump.add((byte) 0x00);
                        } else {
                            outputArea.setText("Assembling Error: instruction requires no operand. Consider adding a ';' to make the following text a comment. Line " + (lineNum + 1));
                            return;
                        }
                        break;
                    default:
                        outputArea.setText("Assembling Error: invalid instruction/token. Line " + (lineNum + 1));
                        return;
                }
            } catch (InvalidTokenException e) {
                outputArea.setText("Assembling Error: " + e.getMessage() + ". Line " + (lineNum + 1));
                return;
            }

            for (int i = 0; i < lineHexDump.size(); i++) {
                hexDumpSb.append(String.format("%02X", lineHexDump.get(i)));
                outputSb.append(String.format("0x%02X", lineHexDump.get(i)));
                if (i < lineHexDump.size() - 1) {
                    hexDumpSb.append(" ");
                }
                if (lineNum < endLine - 1 || i < lineHexDump.size() - 1) {
                    outputSb.append(", ");
                }
            }
            if (lineNum < textArea.getLineCount() - 2) {
                hexDumpSb.append("\n");
            }
        }

        // Update the address area and hex dump area
        addressArea.setText(addressSb.toString());
        hexDumpArea.setText(hexDumpSb.toString());
        outputArea.setText(outputSb.toString());
    }

    /**Verifies the constant value in the operand.*/
    private byte parseConst(String[] tokens) throws InvalidTokenException {
        String operand = tokens[1].substring(tokens[1].indexOf("$") + 1);
        if (operand.length() == 2) {
            return Byte.parseByte(operand, 16);
        }
        throw new InvalidTokenException("Invalid operand length. Should be 2 digit hex number");
    }

    /**Verifies the relative address.*/
    private byte parseRelAddr(String[] tokens) throws InvalidTokenException {
        String operand = tokens[1].substring(tokens[1].indexOf("$") + 1);
        if (operand.length() == 2) {
            return Byte.parseByte(operand, 16);
        }
        throw new InvalidTokenException("Invalid operand length. Should be 2 digit hex number");
    }

    /**Converts the absolute address to little-endian.*/
    private byte[] parseAbsAddr(String[] tokens) throws InvalidTokenException {
        String operand = tokens[1].substring(tokens[1].indexOf("$") + 1);
        if (operand.length() == 4) {
            int intValue = Integer.parseInt(operand, 16);
            return new byte[] {(byte) (intValue & 0xFF), (byte) ((intValue >> 8) & 0xFF)};
        }
        throw new InvalidTokenException("Invalid operand length. Should be 4 digit hex number");
    }

    /**Gets the relative address the label refers to.*/
    private byte parseRelLabel(String[] tokens, LabelList decLabels, LabelList callLabels, int line) throws InvalidTokenException {
        int decAddr = decLabels.addressOfFirst(tokens[1]);
        int callAddr = callLabels.addressOfAt(tokens[1], line);
        if (decAddr == -1 || callAddr == -1) {
            throw new InvalidTokenException("Could not find label: " + tokens[1]);
        }
        int difference = decAddr - (callAddr + 1);
        if (difference < -128 || difference > 127) {
            throw new InvalidTokenException("Target address is too far for relative addressing. The change is " + difference + " and must be between -128 and 127 inclusive. Consider chain branching");
        }
        return (byte) difference;
    }

    /**Gets the absolute address a label refers to.*/
    private byte[] parseAbsLabel(String[] tokens, LabelList decLabels) throws InvalidTokenException {
        int decAddress = decLabels.addressOfFirst(tokens[1]);
        if (decAddress == -1) {
            throw new InvalidTokenException("Could not find label: " + tokens[1]);
        }
        return new byte[] {(byte) (decAddress & 0xFF), (byte) ((decAddress >> 8) & 0xFF)};
    }

    /**Thrown to indicate the instruction got a bad operand.*/
    private static class InvalidTokenException extends Exception {
        private InvalidTokenException(String message) {
            super(message);
        }
    }

    /**Custom data structure to keep track of labels.*/
    private static class LabelList {
        private final List<Label> labels;
        private void add(String n, int l, int a) {labels.add(new Label(n, l, a));}

        private LabelList() {
            labels = new ArrayList<>();
        }

        private int lineNumberOf(String n) {
            for (Label label : labels) {
                if (label.name.equals(n)) {
                    return label.lineNumber;
                }
            }
            return -1;
        }

        private int addressOfFirst(String n) {
            for (Label label : labels) {
                if (label.name.equals(n)) {
                    return label.address;
                }
            }
            return -1;
        }

        private int addressOfAt(String n, int l) {
            for (Label label : labels) {
                if (label.name.equals(n) && label.lineNumber == l) {
                    return label.address;
                }
            }
            return -1;
        }

        private static class Label {
            private final String name;
            private final int lineNumber, address;

            private Label(String n, int l, int a) {
                name = n;
                lineNumber = l;
                address = a;
            }
        }
    }

    /**Entry point.*/
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}