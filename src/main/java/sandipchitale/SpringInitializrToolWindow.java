package sandipchitale;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SpringInitializrToolWindow {
    private final JPanel contentToolWindow;

    public SpringInitializrToolWindow(Project project) {
        this.contentToolWindow = new SimpleToolWindowPanel(true, true);
        this.contentToolWindow.setPreferredSize(new Dimension(1080, 880));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.add(new JLabel("<html>Click on <b>+ New Project...</b> button and select the <b>start.spring.io</b> generator in the <b>New Project</b> dialog."));
        JButton newProjectButton = new JButton("+ New Project...");
        toolbar.add(newProjectButton);
        newProjectButton.addActionListener((ActionEvent actionEvent) -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                ActionManager actionManager = ActionManager.getInstance();
                AnAction newProjectAction = actionManager.getAction("NewProject");
                actionManager.tryToExecute(
                    newProjectAction,
                    null,
                    this.contentToolWindow,
                    "Spring Initializr Tool Window",
                    true
                );
            });
        });
        this.contentToolWindow.add(toolbar, BorderLayout.NORTH);
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        statusBar.add(new JLabel("<html>Spring Initializr tool window will be deprecated in near future. You can directly invoke <b>+ New Project...</b> action in IDE toolbar."));
        this.contentToolWindow.add(statusBar, BorderLayout.SOUTH);

    }

    public JComponent getContent() {
        return this.contentToolWindow;
    }
}