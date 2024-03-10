import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class Main extends JFrame {
    private JTextArea textArea;
    private JTextArea lineNumberArea;
    private JPanel contentPane;

    public Main() {
        setTitle("Simple Text Editor");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        contentPane = new JPanel(new BorderLayout());

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
        JScrollPane lineNumberScrollPane = new JScrollPane(lineNumberArea);
        lineNumberScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        lineNumberScrollPane.setPreferredSize(new Dimension(40, textArea.getHeight()));
        contentPane.add(lineNumberScrollPane, BorderLayout.WEST);

        setContentPane(contentPane);
        setVisible(true);

        updateLineNumbers();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
