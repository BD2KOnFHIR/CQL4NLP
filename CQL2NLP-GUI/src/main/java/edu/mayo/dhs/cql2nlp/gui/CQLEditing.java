package edu.mayo.dhs.cql2nlp.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import edu.mayo.hsr.dhs.cql2nlp.CQLParser;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class CQLEditing extends JDialog {

    public static Map<String, String> valueSetsToResolve;

    private JTextPane cqlInput;
    private JList<String> valueSetList;
    private JButton submitButton;
    private JPanel cqlPane;
    private JButton refreshButton;

    public CQLEditing() {
        setContentPane(cqlPane);
        setModal(true);

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // call onCancel() on ESCAPE
        cqlPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        CQLParser parser = new CQLParser();
//        this.cqlInput.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//                try {
//                    Set<String> keys = parser.getValueSets(e.getDocument().getText(0, e.getDocument().getLength())).keySet();
//                    String[] arr = keys.toArray(new String[0]);
//                    Arrays.sort(arr);
//                    valueSetList.setListData(arr);
//                    submitButton.setEnabled(true);
//                } catch (BadLocationException | IOException t) {
//                    valueSetList.setListData(new String[]{"Invalid CQL"});
//                    submitButton.setEnabled(false);
//                }
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//                try {
//                    Set<String> keys = parser.getValueSets(e.getDocument().getText(0, e.getDocument().getLength())).keySet();
//                    String[] arr = keys.toArray(new String[0]);
//                    Arrays.sort(arr);
//                    valueSetList.setListData(arr);
//                    submitButton.setEnabled(true);
//                } catch (BadLocationException | IOException t) {
//                    valueSetList.setListData(new String[]{"Invalid CQL"});
//                    submitButton.setEnabled(false);
//                }
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//                try {
//                    Set<String> keys = parser.getValueSets(e.getDocument().getText(0, e.getDocument().getLength())).keySet();
//                    String[] arr = keys.toArray(new String[0]);
//                    Arrays.sort(arr);
//                    valueSetList.setListData(arr);
//                    submitButton.setEnabled(true);
//                } catch (BadLocationException | IOException t) {
//                    valueSetList.setListData(new String[]{"Invalid CQL"});
//                    submitButton.setEnabled(false);
//                }
//            }
//        });
        submitButton.addActionListener(e -> {
            List<String> toKeep = valueSetList.getSelectedValuesList();
            try {
                Map<String, String> vals = parser.getValueSets(this.cqlInput.getDocument().getText(0, this.cqlInput.getDocument().getLength()));
                valueSetsToResolve = new HashMap<>();
                toKeep.forEach(s -> valueSetsToResolve.put(s, vals.get(s)));
                synchronized (GUI.nextPhaseFlag) {
                    GUI.nextPhaseFlag.set(true);
                    GUI.nextPhaseFlag.notifyAll();
                }
                dispose();
            } catch (IOException | BadLocationException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred, please check your console");
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Set<String> keys = parser.getValueSets(cqlInput.getDocument().getText(0, cqlInput.getDocument().getLength())).keySet();
                    String[] arr = keys.toArray(new String[0]);
                    Arrays.sort(arr);
                    valueSetList.setListData(arr);
                    submitButton.setEnabled(true);
                } catch (BadLocationException | IOException t) {
                    valueSetList.setListData(new String[]{"Invalid CQL"});
                    submitButton.setEnabled(false);
                }
            }
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        cqlPane = new JPanel();
        cqlPane.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        cqlPane.add(panel1, new GridConstraints(0, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        valueSetList = new JList();
        scrollPane1.setViewportView(valueSetList);
        final JLabel label1 = new JLabel();
        label1.setText("Select ValueSets to Generate NLP for");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        cqlPane.add(panel2, new GridConstraints(0, 0, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel2.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        cqlInput = new JTextPane();
        cqlInput.setText("");
        scrollPane2.setViewportView(cqlInput);
        final JLabel label2 = new JLabel();
        label2.setText("Input CQL Here");
        panel2.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        submitButton = new JButton();
        submitButton.setEnabled(false);
        submitButton.setText("Submit");
        cqlPane.add(submitButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        cqlPane.add(spacer1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return cqlPane;
    }

}
