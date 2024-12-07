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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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

        JPanel savedConfigsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        JLabel bookmarksHintLabel = new JLabel("Use built-in bookmarks ( ☆ )");
        bookmarksHintLabel.setToolTipText("Use built-in bookmarks to save ( ... > BOOKMARK ) and load configurations ( ☆ ).");
        savedConfigsPanel.add(bookmarksHintLabel);

        progressBarWrapper.add(savedConfigsPanel, BorderLayout.WEST);

        progressBarLabel = new JLabel(" ");
        progressBarWrapper.add(progressBarLabel, BorderLayout.CENTER);

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
        client.addDownloadHandler(new DownloadHandler(this, moduleBuilder, context, contentToolWindow, progressBar, progressBarLabel), browser.getCefBrowser());

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
                    url = Urls.parseEncoded(urlString.replace("/starter.zip?", "/#!"));
                    String name = Arrays.stream(Objects.requireNonNull(Objects.requireNonNull(url).getParameters()).split("&"))
                            .filter((String parameter) -> {
                                String[] parameterParts = parameter.split("=");
                                return (parameterParts[0].equals("name"));
                            })
                            .map((String nameParameter) -> nameParameter.split("=")[1])
                            .findFirst().orElse(null);
                    if (name != null) {
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

    private record DownloadHandler(StartSpringIOModuleWizardStep startSpringIOModuleWizardStep,
                                   StartSpringIOModuleBuilder moduleBuilder,
                                   WizardContext context,
                                   JComponent parent,
                                   JProgressBar progressBar,
                                   JLabel progressBarLabel) implements CefDownloadHandler {

        @Override
        public void onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName, CefBeforeDownloadCallback callback) {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            progressBarLabel.setText("Generating and downloading project '" + suggestedName + "' zip.");
            progressBar.setIndeterminate(true);
            callback.Continue(downloadItem.getFullPath(), false);
        }

        @Override
        public void onDownloadUpdated(CefBrowser browser, CefDownloadItem downloadItem, CefDownloadItemCallback callback) {
            try {
                if (downloadItem.isComplete()) {
                    startSpringIOModuleWizardStep.setDownloadCalled(true);
                    String downloadItemLocation = downloadItem.getFullPath();
                    String suggestedFileName = downloadItem.getSuggestedFileName();
                    String suggestedFileNameSansExtension = suggestedFileName.replaceFirst("\\.zip", "");
                    context.putUserData(StartSpringIOModuleBuilder.START_SPRING_IO_DOWNLOADED_ZIP_LOCATION, downloadItemLocation);
                    moduleBuilder.setProjectName(suggestedFileNameSansExtension);
                    progressBarLabel.setText("Downloaded project '" + suggestedFileNameSansExtension + "' zip to: '" + downloadItemLocation + "'. Click Next below.");
                } else if (downloadItem.isCanceled()) {
                    progressBarLabel.setText("Downloaded cancelled.");
                }
            } finally {
                parent.setCursor(Cursor.getDefaultCursor());
                progressBar.setIndeterminate(false);
            }
        }
    }
}
