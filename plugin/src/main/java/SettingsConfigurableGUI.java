import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Hashtable;

public class SettingsConfigurableGUI {
    private JSlider slider1;
    private JSpinner spinner1;
    private JPanel rootPanel;
    private JSpinner spinner2;
    private JTextField textField1;
    private JSpinner.NumberEditor editor;
    private boolean modifiedByOther = false;
    private SettingsState settings;

    SettingsConfigurableGUI() { }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private void createUIComponents() {
        // TODO: value get from state
        settings = SettingsState.getInstance();
        spinner1 = new JSpinner(new SpinnerNumberModel(settings.getAccuracy(),0 ,1.0,0.05));
        slider1 = new JSlider(0, 1000, ((int) (settings.getAccuracy() * 1000)));
        spinner2 = new JSpinner(new SpinnerNumberModel(settings.getMaxResults(),10 ,100,1));
        textField1 = new JTextField(settings.getCustomServer());

        editor = new JSpinner.NumberEditor(spinner1) ;
        spinner1.setEditor(editor);
        spinner1.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                if (modifiedByOther) {
                    return;
                }
                modifiedByOther = true;
                slider1.setValue(((int)((double) spinner1.getValue()) * 1000));
                modifiedByOther = false;
            }
        });

        // get from settings
        Hashtable labelTable = new Hashtable();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(1000, new JLabel("1"));
        slider1.setLabelTable(labelTable);
        slider1.setPaintLabels(true);
        slider1.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (modifiedByOther) {
                    return;
                }
                modifiedByOther = true;
                spinner1.setValue(((double) slider1.getValue()) / 1000);
                modifiedByOther = false;
            }
        });
    }

    public boolean isModified() {
        boolean modified = false;
        modified |= !spinner1.getValue().equals(settings.getAccuracy());
        modified |= !spinner2.getValue().equals(settings.getMaxResults());
        modified |= !textField1.getText().equals(settings.getCustomServer());
        return modified;
    }

    public void apply() {
        settings.setAccuracy((double) spinner1.getValue());
        settings.setMaxResults((int) spinner2.getValue());
        if (textField1.getText().endsWith("/"))
            settings.setCustomServer(textField1.getText().substring(0, textField1.getText().length() - 1));
        else
            settings.setCustomServer(textField1.getText());
    }

    public void reset() {
        spinner1.setValue(settings.getAccuracy());
        slider1.setValue(((int) (settings.getAccuracy() * 1000)));
        spinner2.setValue(settings.getMaxResults());
        textField1.setText(settings.getCustomServer());
    }
}
