import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Login implements ActionListener {
    /** UI CONSTANTS */
    private static JFrame frame;
    private static final JLabel[] labels = new JLabel[5];
    private static final JTextField[] textFields = new JTextField[5];
    private static JLabel fail;
    private static final int START_Y = 100;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;
    private static final int LABEL_WIDTH = 120;
    private static final int FIELD_WIDTH = 180;
    private static final int COMP_HEIGHT = 25;

    /** CONSTANTS GETTER METHODS */
    public static JFrame getFrame() { return frame; }
    public static int getWidth() { return WIDTH; }
    public static int getHeight() { return HEIGHT; }
    public static int getCompHeight() { return COMP_HEIGHT; }


    /** Opens JavaFX login page*/
    public static void main(String[] args) {
        RequestsManager.init();

        frame = new JFrame();
        JPanel panel = new JPanel();
        frame.setSize(WIDTH, HEIGHT);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        // frame.add(Home.homePanel());

        panel.setLayout(null);

        addField("Sender ID", 0, false, TemporaryCredentials.sender_id);
        addField("Sender Password", 1, true, TemporaryCredentials.sender_password);
        addField("Company ID", 2, false, TemporaryCredentials.company_id);
        addField("User ID", 3, false, TemporaryCredentials.user_id);
        addField("User Password", 4, true, TemporaryCredentials.user_password);

        for (int i = 0; i < labels.length; i++) {
            panel.add(labels[i]);
            panel.add(textFields[i]);
        }

        JButton button = new JButton("Login");
        button.setBounds(WIDTH / 2 - 40, START_Y + 350, 80, COMP_HEIGHT);
        button.addActionListener(new Login());
        panel.add(button);

        fail = new JLabel("", SwingConstants.CENTER);
        fail.setForeground(Color.RED);
        fail.setBounds(WIDTH / 8, START_Y + 375, (int) (WIDTH * .75), 50);
        panel.add(fail);

        frame.setVisible(true);
        frame.getContentPane().requestFocusInWindow();
    }


    /** ADD LOGIN FIELDS */
    private static void addField(String label, int index, boolean isPassword) {
        addField(label, index, isPassword, "");
    }
    private static void addField(String label, int index, boolean isPassword, String defaultText) {
        labels[index] = new JLabel(label, SwingConstants.CENTER);
        textFields[index] = isPassword ? new JPasswordField(defaultText, SwingConstants.CENTER) : new JTextField(defaultText, SwingConstants.CENTER);

        labels[index].setBounds(
                (WIDTH - LABEL_WIDTH) / 2,
                (int) (START_Y + (index * COMP_HEIGHT * 2.5)),
                LABEL_WIDTH,
                COMP_HEIGHT
        );
        textFields[index].setBounds(
                (WIDTH - FIELD_WIDTH) / 2,
                (int) (START_Y + (index * COMP_HEIGHT * 2.5) + COMP_HEIGHT),
                FIELD_WIDTH,
                COMP_HEIGHT
        );
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean allFilled = true;
        for (JTextField textField : textFields) {
            if (textField.getText().trim().length() == 0) {
                allFilled = false;
                break;
            }
        }

        if (!allFilled) {
            fail.setText("<html>Login failed. Please fill in all fields.</html>");
        } else {
            RequestsManager.setLoginCredentials(
                    textFields[0].getText().trim(),
                    textFields[1].getText().trim(),
                    textFields[2].getText().trim(),
                    textFields[3].getText().trim(),
                    textFields[4].getText().trim()
            );
            try {
                RequestsManager.generateAPISession();
                fail.setText("");
                System.out.println("Login successful");
                frame.setContentPane(SelectMode.selectMode());
                frame.revalidate();
                frame.getContentPane().requestFocusInWindow();
            } catch (IOException | ParserConfigurationException | SAXException | NullPointerException ex) {
                fail.setText("<html>Login failed. Please check your credentials and try again.</html>");
            }
        }
    }
}
