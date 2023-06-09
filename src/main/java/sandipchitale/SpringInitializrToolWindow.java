package sandipchitale;

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

    public SpringInitializrToolWindow()
    {
        this.contentToolWindow = new SimpleToolWindowPanel(true, true);
        this.contentToolWindow.setPreferredSize(new Dimension(1080, 880));

        BorderLayoutPanel progressBarWrapper = new BorderLayoutPanel(5, 5);
        progressBarWrapper.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        contentToolWindow.add(progressBarWrapper, BorderLayout.NORTH);

        JLabel progressBarLabel = new JLabel(" ");
        progressBarWrapper.add(progressBarLabel, BorderLayout.SOUTH);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBarWrapper.add(progressBar, BorderLayout.NORTH);

        JBCefBrowser browser = new JBCefBrowser("https://start.spring.io");
        JBCefClient client = browser.getJBCefClient();
        client.addDownloadHandler(new DownloadHandler(getContent(), progressBar, progressBarLabel), browser.getCefBrowser());
        contentToolWindow.add(browser.getComponent(), BorderLayout.CENTER);
    }

    public JComponent getContent()
    {
        return this.contentToolWindow;
    }

    private record DownloadHandler(JComponent parent, JProgressBar progressBar, JLabel progressBarLabel) implements CefDownloadHandler {
        @Override
        public void onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName, CefBeforeDownloadCallback callback) {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            progressBar.setIndeterminate(true);
            progressBarLabel.setText("Generating, downloading, extracting and opening " + suggestedName +" in IntelliJ.");
        }

        @Override
        public void onDownloadUpdated(CefBrowser browser, CefDownloadItem downloadItem, CefDownloadItemCallback callback) {
            if (downloadItem.isComplete()) {
                String fullPath = downloadItem.getFullPath();
                String suggestedFileName = downloadItem.getSuggestedFileName();
                SwingUtilities.invokeLater(() -> {
                    try {
                        JFileChooser saveDirectoryChooserDialog = new JFileChooser();
                        saveDirectoryChooserDialog.setDialogTitle("Select directory to extract " + suggestedFileName + " and open in IntelliJ");
                        saveDirectoryChooserDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        int option = saveDirectoryChooserDialog.showOpenDialog(parent);
                        if (option == JFileChooser.APPROVE_OPTION) {
                            File saveDirectory = saveDirectoryChooserDialog.getSelectedFile();
                            String suggestedFileNameSansExtension = suggestedFileName.replaceFirst("\\.zip", "");
                            if (Paths.get(saveDirectory.getAbsolutePath(), suggestedFileNameSansExtension).toFile().exists()) {
                                JOptionPane.showMessageDialog(parent
                                        ,"Folder exists: " +Paths.get(saveDirectory.getAbsolutePath(), suggestedFileNameSansExtension)
                                        ,"Folder exists"
                                        ,JOptionPane.ERROR_MESSAGE
                                );
                            } else {
                                try {
                                    extractZip(fullPath, saveDirectory.getAbsolutePath());
                                    ProjectManagerEx.getInstanceEx().loadAndOpenProject(Paths.get(saveDirectory.getAbsolutePath(), suggestedFileNameSansExtension).toString());
                                } catch (IOException | ArchiveException | JDOMException ignored) {
                                }
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