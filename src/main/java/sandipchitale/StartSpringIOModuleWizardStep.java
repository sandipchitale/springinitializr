package sandipchitale;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.ui.JBImageIcon;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandler;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;

public class StartSpringIOModuleWizardStep extends ModuleWizardStep {
    private static final Logger LOG = Logger.getInstance(StartSpringIOModuleWizardStep.class);
    static final String SPRINGINITIALIZR_URL_PREFIX_DEFAULT_VALUE = "https://start.spring.io";

    private final StartSpringIOModuleBuilder moduleBuilder;
    private final WizardContext context;
    private final Disposable parentDisposable;

    private SimpleToolWindowPanel contentToolWindow;

    private JLabel progressBarLabel;
    private JButton openDownloadButton;

    private JProgressBar progressBar;

    private boolean downloadCalled;
    private JBCefBrowser browser;

    public StartSpringIOModuleWizardStep(StartSpringIOModuleBuilder moduleBuilder, WizardContext context, Disposable parentDisposable) {
        this.moduleBuilder = moduleBuilder;
        this.context = context;
        this.parentDisposable = parentDisposable;
    }

    /**
     * Update UI from ModuleBuilder and WizardContext
     */
    public void updateStep() {
    }

    @Override
    public JComponent getComponent() {
        contentToolWindow = new SimpleToolWindowPanel(true, true);
        this.contentToolWindow.setPreferredSize(new Dimension(1080, 200));
        JPanel progressBarWrapper = new JPanel(new BorderLayout(10, 0));
        progressBarWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel bookmarksHintLabel = new JLabel("Use built-in bookmarks ( ☆ )");
        bookmarksHintLabel.setToolTipText("Use built-in bookmarks to save ( ... > BOOKMARK ) and load configurations ( ☆ ).");

        progressBarWrapper.add(bookmarksHintLabel, BorderLayout.WEST);

        JPanel progressBarPanel = new JPanel(new BorderLayout(10, 0));

        progressBarLabel = new JLabel(" ");

        progressBarPanel.add(progressBarLabel, BorderLayout.CENTER);

        openDownloadButton = new JButton(UIManager.getIcon("Tree.openIcon"));
        openDownloadButton.setEnabled(false);
        openDownloadButton.addActionListener((ActionEvent e) -> {
            String downloadItemLocation = context.getUserData(StartSpringIOModuleBuilder.START_SPRING_IO_DOWNLOADED_ZIP_LOCATION);
            if (downloadItemLocation != null) {
                File downloadItemLocationFile = new File(downloadItemLocation);
                if (downloadItemLocationFile.isFile()) {
                    try {
                        Desktop.getDesktop().open(downloadItemLocationFile.getParentFile());
                    } catch (IOException ignore) {
                    }
                }
            }
        });
        progressBarPanel.add(openDownloadButton, BorderLayout.EAST);

        progressBarWrapper.add(progressBarPanel, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBarWrapper.add(progressBar, BorderLayout.EAST);

        browser = new JBCefBrowser(SPRINGINITIALIZR_URL_PREFIX_DEFAULT_VALUE);
        browser.getComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                e.consume();
            }
        });

        JBCefClient client = browser.getJBCefClient();

        client.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser,
                                                                       CefFrame frame,
                                                                       CefRequest request,
                                                                       boolean isNavigation,
                                                                       boolean isDownload,
                                                                       String requestInitiator,
                                                                       BoolRef disableDefaultHandling) {
                String urlString = request.getURL();
                Url url = Urls.parseEncoded(urlString);
                if (Objects.requireNonNull(url).getPath().equals("/starter.zip")) {
                    String name = Arrays.stream(Objects.requireNonNull(Objects.requireNonNull(url).getParameters()).split("&"))
                            .filter((String parameter) -> {
                                String[] parameterParts = parameter.split("=");
                                return (parameterParts[0].equals("name"));
                            })
                            .map((String nameParameter) -> nameParameter.split("=")[1])
                            .findFirst().orElse(null);
                    if (name != null) {
                        contentToolWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        progressBarLabel.setText("Generating and downloading project '" + name + "' zip.");
                        progressBar.setIndeterminate(true);
                        openDownloadButton.setEnabled(false);
                        openDownloadButton.setToolTipText("");
                        context.putUserData(StartSpringIOModuleBuilder.START_SPRING_IO_DOWNLOADED_ZIP_LOCATION, null);
                        SwingUtilities.invokeLater(() -> {
                            try {
                                File tempDir = Files.createTempDirectory("start.spring.io").toFile();
                                File targetFile = new File(tempDir, String.format("%s.zip", name));

                                // Download the file
                                try {
                                    URL downloadURL = new URI(urlString).toURL();
                                    HttpURLConnection connection = (HttpURLConnection) downloadURL.openConnection();

                                    try (InputStream inputStream = connection.getInputStream()) {
                                        Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    } finally {
                                        connection.disconnect();
                                        setDownloadCalled(true);
                                        String downloadItemLocation = targetFile.getAbsolutePath();
                                        String suggestedFileName = targetFile.getName();
                                        String suggestedFileNameSansExtension = suggestedFileName.replaceFirst("\\.zip", "");
                                        context.putUserData(StartSpringIOModuleBuilder.START_SPRING_IO_DOWNLOADED_ZIP_LOCATION, downloadItemLocation);
                                        moduleBuilder.setProjectName(suggestedFileNameSansExtension);
                                        progressBarLabel.setText("Downloaded project '" + suggestedFileNameSansExtension + "' zip to: '" + downloadItemLocation + "'. Click Next below.");
                                        openDownloadButton.setEnabled(true);
                                        openDownloadButton.setToolTipText("Open in file manager: " + downloadItemLocation);
                                        contentToolWindow.setCursor(Cursor.getDefaultCursor());
                                        progressBar.setIndeterminate(false);
                                    }

                                } catch (MalformedURLException | URISyntaxException e) {
                                    throw new RuntimeException(e);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
                return null;
            }
        }, browser.getCefBrowser());

        contentToolWindow.add(browser.getComponent(), BorderLayout.CENTER);
        contentToolWindow.add(progressBarWrapper, BorderLayout.SOUTH);

        return contentToolWindow;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return browser.getComponent();
    }

    @Override
    public void _init() {
        _reset();
    }

    public boolean validate() {
        if (!downloadCalled) {
            Messages.showWarningDialog(contentToolWindow,
                    "You need to generate the project first! Click on Generate button on the start.spring.io page.",
                    "Must Generate Project First");
        }
        return downloadCalled;
    }

    @Override
    public void updateDataModel() {
        _reset();
    }

    @Override
    public void _commit(boolean finishChosen) {
        // Nothing to do here
    }

    private void _reset() {
        setDownloadCalled(false);
        progressBarLabel.setText("<html>Configure project and then click <b>[ GENERATE CTRL + ⏎ ]</b> button above.");
        progressBar.setIndeterminate(false);
    }

    private void setDownloadCalled(boolean downloadCalled) {
        this.downloadCalled = downloadCalled;
    }
}
