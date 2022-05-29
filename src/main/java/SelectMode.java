import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SelectMode implements ActionListener {
    private final static String updateText = "Add/Update Fields";
    private final static String migrateText = "Migration Tool";
    /** @return panel where user can select 'update' or 'migration tool' mode */
    public static JPanel selectMode() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridLayout layout = new GridLayout(0,2, 20, 20);
        panel.setLayout(layout);

        Button update = new Button(updateText);
        update.addActionListener(new SelectMode());
        panel.add(update);

        Button migrate = new Button(migrateText);
        migrate.addActionListener(new SelectMode());
        panel.add(migrate);

        return panel;
    }


    /** List of variables for update mode */
    public static final String[] updateModes = {
        "Create Contract",
        "Create Contract CO",
        "Update Project/Job",
        "Update Employee",
        "Update Purchasing Tx. Definitions",
        "Update Standard Cost Codes",
    };
    private static final Button[] updateButtons = new Button[updateModes.length];
    private static int updateMode = -1;
    public static int getUpdateMode() { return updateMode; }
    public static boolean getAndResetUpdateMode() {
        boolean b = updateMode > -1;
        updateMode = -1;
        return b;
    }

    /** @return panel where user can select which update mode */
    public static JPanel selectUpdateMode() {
        return getUpdatePanel(updateButtons, updateModes);
    }


    /** List of variables for migration mode */
    public static final String[] migrationModes = {
            "Project Estimate"
    };
    private static final Button[] migrateButtons = new Button[migrationModes.length];
    private static int migrationMode = -1;
    public static int getMigrationMode() { return migrationMode; }
    public static boolean getAndResetMigrationMode() {
        boolean b = migrationMode > -1;
        migrationMode = -1;
        return b;
    }

    /** @return panel where user can select which migration mode */
    public static JPanel selectMigrationMode() {
        return getUpdatePanel(migrateButtons, migrationModes);
    }


    /** Helper method for selectMigrationMode() and selectUpdateMode() */
    private static JPanel getUpdatePanel(Button[] updateButtons, String[] updateModes) {
        JPanel panel = new JPanel();
        panel.setLayout(null);

        JPanel subPanel = new JPanel();
        GridLayout layout = new GridLayout(0,2, 20, 20);
        subPanel.setLayout(layout);
        subPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        subPanel.setBounds(0, 55, Login.getWidth(), Login.getHeight() - 75);

        BasicArrowButton back = new BasicArrowButton(BasicArrowButton.WEST);
        back.setBounds(20, 20, 45, 35);
        back.addActionListener(new Upload.BackAction());
        panel.add(back);

        for (int i = 0; i < updateButtons.length; i++) {
            updateButtons[i] = new Button(updateModes[i]);
            updateButtons[i].addActionListener(new SelectMode());
            subPanel.add(updateButtons[i]);
        }

        panel.add(subPanel);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(updateText)) {
            Login.getFrame().setContentPane(SelectMode.selectUpdateMode());
            Login.getFrame().revalidate();
            Login.getFrame().getContentPane().requestFocusInWindow();
        } else if (e.getActionCommand().equals(migrateText)) {
            Login.getFrame().setContentPane(SelectMode.selectMigrationMode());
            Login.getFrame().revalidate();
            Login.getFrame().getContentPane().requestFocusInWindow();
        } else {
            for (int i = 0; i < updateModes.length; i++) {
                if (e.getActionCommand().equals(updateModes[i])) {
                    updateMode = i;
                    break;
                }
            }
            if (updateMode == -1) {
                for (int i = 0; i < migrationModes.length; i++) {
                    if (e.getActionCommand().equals(migrationModes[i])) {
                        migrationMode = i;
                        break;
                    }
                }
            }

            if (updateMode != -1) {
                Login.getFrame().setContentPane(Upload.uploadPanel(updateModes[updateMode]));
                Login.getFrame().revalidate();
            } else {
                Login.getFrame().setContentPane(Upload.uploadPanel(migrationModes[migrationMode]));
                Login.getFrame().revalidate();
            }
        }
    }
}
