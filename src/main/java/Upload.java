import org.apache.poi.ss.usermodel.Workbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class Upload implements ActionListener {
    /** UI CONSTANTS */
    private static Button uploadButton;
    private static Button saveButton;
    private static final int START_Y = 100;
    private static JTextArea textArea;

    /** SAVE FILE */
    private static String suggestedFileName = "";
    private static Workbook workbook;
    public static void setFileToSave(String s, Workbook w) {
        suggestedFileName = s;
        workbook = w;
        saveButton.setVisible(true);
    }


    /** Returns the main upload panel */
    public static JPanel uploadPanel(String s) {
        JPanel panel = new JPanel();
        panel.setLayout(null);

        BasicArrowButton back = new BasicArrowButton(BasicArrowButton.WEST);
        back.setBounds(20, 20, 45, 35);
        back.addActionListener(new BackAction());
        panel.add(back);

        JLabel title = new JLabel(s, SwingConstants.CENTER);
        title.setFont(new Font("Sans Serif", Font.BOLD, 20));
        title.setBounds(Login.getWidth() / 2 - 150, START_Y, 300, Login.getCompHeight());
        panel.add(title);

        uploadButton = new Button("Select a file");
        uploadButton.addActionListener(new Upload());
        uploadButton.setBounds(Login.getWidth() / 2 - 150, START_Y + 50, 300, Login.getCompHeight());
        panel.add(uploadButton);

        JLabel label = new JLabel("Only .xlsx or .xls files");
        label.setBounds(Login.getWidth() / 2 - 100, START_Y + Login.getCompHeight() + 50, 200, Login.getCompHeight());
        panel.add(label);

        textArea = new JTextArea("Waiting for file upload...\n\n");
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBounds(Login.getWidth() / 6, Login.getHeight() / 4 + 75, Login.getWidth() * 2 / 3, Login.getHeight() / 3);
        panel.add(scrollPane);

        if (SelectMode.getMigrationMode() > -1) {
            saveButton = new Button("Save Output File");
            saveButton.addActionListener(new Upload());
            saveButton.setBounds(Login.getWidth() / 2 - 100, Login.getHeight() * 7 / 12 + 125, 200, Login.getCompHeight());
            saveButton.setVisible(false);
            panel.add(saveButton);
        }

        return panel;
    }


    /** Methods to update the text area (output) */
    public static void appendTextArea(String s) { textArea.append(s); }
    public static void setTextArea(String s) { textArea.setText(s); }


    /** Handles upload files */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Save Output File")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Output File");
            fileChooser.setSelectedFile(new File(suggestedFileName));
            int userSelection = fileChooser.showSaveDialog(null);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();

                try {
                    FileOutputStream os = new FileOutputStream(fileToSave);
                    workbook.write(os);
                    os.close();
                    workbook.close();
                    saveButton.setVisible(false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel Files", "xlsx", "xls");
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(filter);
            int response = fileChooser.showOpenDialog(null);

            if (response == JFileChooser.APPROVE_OPTION) {
                textArea.setText("");
                File file = new File(fileChooser.getSelectedFile().getAbsolutePath());
                uploadButton.setLabel(file.getName());
                System.out.println(file);
                try {
                    SpreadsheetManager.parseSpreadsheet(file);
                } catch (IOException | InterruptedException ex) {
                    JOptionPane.showMessageDialog(Login.getFrame(), ex);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static class BackAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame frame = Login.getFrame();
            if (SelectMode.getAndResetUpdateMode()) {
                frame.setContentPane(SelectMode.selectUpdateMode());
            } else if (SelectMode.getAndResetMigrationMode()) {
                frame.setContentPane(SelectMode.selectMigrationMode());
            } else {
                frame.setContentPane(SelectMode.selectMode());
            }
            frame.revalidate();
            frame.getContentPane();
        }
    }
}
