package org.tera201.vcstoolkit.services.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Hashtable;

public class VCSToolkitSettingsPage implements Configurable {

    private JPanel mainWindow;
    private JLabel gitLable;
    private JTextField repoPathTextField;
    private JLabel repoPathLable;
    private JLabel modelPathLable;
    private JTextField modelPathTextField;
    private JButton repoBrowseButton;
    private JButton modelBrowseButton;
    private JCheckBox showLogsCheckBox;
    private JSlider slider1;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel usernameLabel;
    private JLabel passworsLabel;

    public VCSToolkitSettingsPage() {

        repoBrowseButton.addActionListener(e -> {
            browseFolder(repoPathTextField);
        });

        modelBrowseButton.addActionListener(e -> {
            browseFolder(modelPathTextField);
        });

        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(0, new JLabel("Unsafe"));
        labels.put(1, new JLabel("Safe"));
        labels.put(2, new JLabel("Create commit"));
//        labels.put(3, new JLabel("Commit"));

        slider1.setLabelTable(labels);
    }

    private void browseFolder(@NotNull final JTextField target) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true,
                false, false, false, false);

        descriptor.setTitle("Select Path");
        String text = target.getText();
        final VirtualFile toSelect = text == null || text.isEmpty() ? null
                : LocalFileSystem.getInstance().findFileByPath(text);

        VirtualFile virtualFile = FileChooser.chooseFile(descriptor, null, toSelect);
        if (virtualFile != null) {
            target.setText(virtualFile.getPath());
        }
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "VCS Toolkit";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return mainWindow;
    }

    @Override
    public boolean isModified() {
        VCSToolkitSettings settings = VCSToolkitSettings.Companion.getInstance();
        return isModified(settings);
    }

    private  boolean isModified(VCSToolkitSettings settings) {
        return !repoPathTextField.getText().equals(settings.getRepoPath()) ||
                !modelPathTextField.getText().equals(settings.getModelPath()) ||
                showLogsCheckBox.isSelected() != settings.getShowGitLogs() ||
                slider1.getValue() != settings.getExternalProjectMode() ||
                !usernameField.getText().equals(settings.getUsername()) ||
                !String.valueOf(passwordField.getPassword()).equals(settings.getPassword());
    }

    @Override
    public void apply() throws ConfigurationException {
        VCSToolkitSettings settings = VCSToolkitSettings.Companion.getInstance();
        applySetting(settings);
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(VCSToolkitSettings.SettingsChangedListener.Companion.getTOPIC())
                .onSettingsChange(settings);
    }

    @Override
    public void reset() {
        VCSToolkitSettings settings = VCSToolkitSettings.Companion.getInstance();
        getSettings(settings);
    }

    private void applySetting(VCSToolkitSettings settings) {
        settings.setRepoPath(repoPathTextField.getText());
        settings.setModelPath(modelPathTextField.getText());
        settings.setShowGitLogs(showLogsCheckBox.isSelected());
        settings.setExternalProjectMode(slider1.getValue());
        settings.setUsername(usernameField.getText());
        settings.setPassword(String.valueOf(passwordField.getPassword()));
    }

    private void getSettings(VCSToolkitSettings settings) {
        repoPathTextField.setText(settings.getRepoPath());
        modelPathTextField.setText(settings.getModelPath());
        showLogsCheckBox.setSelected(settings.getShowGitLogs());
        slider1.setValue(settings.getExternalProjectMode());
        usernameField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
    }

}
