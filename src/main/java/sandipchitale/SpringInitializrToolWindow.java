package sandipchitale;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandler;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SpringInitializrToolWindow {
    private final JPanel contentToolWindow;

    private static File projectsDirectory;
    private final JTextField locationField;

    static {
        String userHome = System.getProperty("user.home");
        projectsDirectory = Paths.get(userHome, "IdeaProjects").toFile();
    }

    public SpringInitializrToolWindow(Project project)
    {
        this.contentToolWindow = new SimpleToolWindowPanel(true, true);
        this.contentToolWindow.setPreferredSize(new Dimension(1080, 880));

        JPanel locationPanel = new BorderLayoutPanel(5, 5);

        JLabel locationLabel = new JLabel("Projects Location:");
        locationLabel.setToolTipText("The location where the project (folder) will be extracted.");
        locationPanel.add(locationLabel, BorderLayout.WEST);

        locationField = new JTextField();
        locationField.setEditable(false);
        locationField.setText(projectsDirectory.getAbsolutePath());
        locationPanel.add(locationField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        JButton browserButton = new JButton("Browse...");
        browserButton.addActionListener((actionEvent) -> {
            JFileChooser saveDirectoryChooserDialog = new JFileChooser();
            saveDirectoryChooserDialog.setDialogTitle("Select project location");
            saveDirectoryChooserDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (projectsDirectory != null) {
                saveDirectoryChooserDialog.setCurrentDirectory(projectsDirectory);
            }
            int option = saveDirectoryChooserDialog.showOpenDialog(getContent());
            if (option == JFileChooser.APPROVE_OPTION) {
                projectsDirectory =  saveDirectoryChooserDialog.getSelectedFile();
                locationField.setText(projectsDirectory.getAbsolutePath());
            }
        });
        buttonPanel.add(browserButton);

        JButton exploreButton = new JButton("Explore...");
        exploreButton.addActionListener((actionEvent) -> {
            if (projectsDirectory.exists() && projectsDirectory.isDirectory()) {
                try {
                    Desktop.getDesktop().open(projectsDirectory);
                } catch (IOException ignore) {
                }
            }
        });

        buttonPanel.add(exploreButton);

        locationPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel progressBarWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        progressBarWrapper.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JLabel progressBarLabel = new JLabel(" ");
        progressBarWrapper.add(progressBarLabel);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBarWrapper.add(progressBar);

        JBCefBrowser browser = new JBCefBrowser("https://start.spring.io");
        JBCefClient client = browser.getJBCefClient();
        client.addDownloadHandler(new DownloadHandler(project, getContent(), progressBar, progressBarLabel), browser.getCefBrowser());

        contentToolWindow.add(locationPanel, BorderLayout.NORTH);
        contentToolWindow.add(browser.getComponent(), BorderLayout.CENTER);
        contentToolWindow.add(progressBarWrapper, BorderLayout.SOUTH);
    }

    public JComponent getContent()
    {
        return this.contentToolWindow;
    }

    private record DownloadHandler(Project project, JComponent parent, JProgressBar progressBar, JLabel progressBarLabel) implements CefDownloadHandler {

        @Override
        public void onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName, CefBeforeDownloadCallback callback) {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            progressBar.setIndeterminate(true);
            progressBarLabel.setText("Generating, downloading, extracting and opening " + suggestedName +" in IntelliJ.");
            callback.Continue(downloadItem.getFullPath(), false);
        }

        @Override
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public void onDownloadUpdated(CefBrowser browser, CefDownloadItem downloadItem, CefDownloadItemCallback callback) {
            if (downloadItem.isComplete()) {
                String fullPath = downloadItem.getFullPath();
                String suggestedFileName = downloadItem.getSuggestedFileName();
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        if (!projectsDirectory.exists()) {
                            // Ensure the directory exists
                            projectsDirectory.mkdirs();
                        }
                        String suggestedFileNameSansExtension = suggestedFileName.replaceFirst("\\.zip", "");
                        Path projectDirectoryPath = Paths.get(projectsDirectory.getAbsolutePath(), suggestedFileNameSansExtension);
                        File projectDirectory = projectDirectoryPath.toFile();
                        String projectDirectoryString = projectDirectoryPath.toString();
                        if (projectDirectory.exists()) {
                            Notification notification = new Notification("springinitializrNotificationGroup",
                                    "Folder exists",
                                    String.format("Folder exists %s", projectDirectoryString),
                                    NotificationType.ERROR);
                            notification.addAction(new NotificationAction("Open in file explorer") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                                    try {
                                        Desktop.getDesktop().open(projectDirectory);
                                    } catch (IOException ignored) {
                                    }
                                    notification.expire();
                                }
                            });
                            notification.notify(project);
                        } else {
                            try {
                                extractZip(fullPath, projectsDirectory.getAbsolutePath());
                                ProjectManagerEx.getInstanceEx().loadAndOpenProject(projectDirectoryString);
                                Notification notification = new Notification("springinitializrNotificationGroup",
                                        "Project opened in intelliJ",
                                        String.format("Project opened in intelliJ %s", projectDirectoryString),
                                        NotificationType.INFORMATION);
                                notification.addAction(new NotificationAction("Open in file explorer") {
                                    @Override
                                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                                        try {
                                            Desktop.getDesktop().open(projectDirectory);
                                        } catch (IOException ignored) {
                                        }
                                        notification.expire();
                                    }
                                });
                                notification.notify(project);
                            } catch (IOException | ArchiveException | JDOMException ignored) {
                            }
                        }
                    } finally {
                        // Delete downloaded file
                        try {
                            Files.delete(Paths.get(fullPath));
                        } catch (IOException ignored) {
                        }
                        parent.setCursor(null);
                        progressBar.setIndeterminate(false);
                        progressBarLabel.setText(" ");
                    }
                });
            }
        }
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void extractZip(String zipFilePath, String extractDirectory) throws IOException, ArchiveException {
            InputStream inputStream;
            Path filePath = Paths.get(zipFilePath);
            inputStream = Files.newInputStream(filePath);
            ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
            ArchiveInputStream archiveInputStream = archiveStreamFactory.createArchiveInputStream(ArchiveStreamFactory.ZIP, inputStream);
            ArchiveEntry archiveEntry;
            while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
                Path path = Paths.get(extractDirectory, archiveEntry.getName());
                File file = path.toFile();
                if (archiveEntry.isDirectory()) {
                    if (!file.isDirectory()) {
                        file.mkdirs();
                    }
                } else {
                    File parent = file.getParentFile();
                    if (!parent.isDirectory()) {
                        parent.mkdirs();
                    }
                    try (OutputStream outputStream = Files.newOutputStream(path)) {
                        IOUtils.copy(archiveInputStream, outputStream);
                    }
                }
            }
        }
    }
}