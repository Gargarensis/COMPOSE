import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingsConfigurable implements SearchableConfigurable {

    SettingsConfigurableGUI mGUI;
    SettingsState settings;

    public SettingsConfigurable() {
        settings = SettingsState.getInstance();
    }

    @Override
    public void disposeUIResources() {
        mGUI = null;
    }

    @NotNull
    @Override
    public String getId() {
        return "compose.SettingsConfigurable";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "COMPOSE";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String s) {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mGUI = new SettingsConfigurableGUI();
        return mGUI.getRootPanel();
    }

    @Override
    public boolean isModified() {
        return mGUI.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        mGUI.apply();
    }

    @Override
    public void reset() {
        mGUI.reset();
    }
}
