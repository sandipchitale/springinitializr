package sandipchitale;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandler;

import javax.swing.*;
import java.awt.*;

public class StartSpringIOModuleWizardStep extends ModuleWizardStep {

    private final StartSpringIOModuleBuilder moduleBuilder;
    private final WizardContext context;

    private SimpleToolWindowPanel contentToolWindow;

    private JLabel progressBarLabel;
    private JProgressBar progressBar;

    private boolean downloadCalled;
    private JBCefBrowser browser;

    public StartSpringIOModuleWizardStep(StartSpringIOModuleBuilder moduleBuilder, WizardContext context, Disposable parentDisposable) {
        this.moduleBuilder = moduleBuilder;
        this.context = context;
    }

    /** Update UI from ModuleBuilder and WizardContext */
    public void updateStep() {
    }

    @Override
    public JComponent getComponent() {
        contentToolWindow = new SimpleToolWindowPanel(true, true);
        JPanel progressBarWrapper = new JPanel(new BorderLayout(10, 0));
        progressBarWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        progressBarLabel = new JLabel(" ");
        progressBarWrapper.add(progressBarLabel, BorderLayout.WEST);

        progressBar = new JProgressBar();
        progressBarWrapper.add(progressBar, BorderLayout.EAST);

        browser = new JBCefBrowser("https://start.spring.io");
        JBCefClient client = browser.getJBCefClient();
        client.addDownloadHandler(new DownloadHandler(this, moduleBuilder, context, contentToolWindow, progressBar, progressBarLabel), browser.getCefBrowser());

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

    public boolean validate() throws ConfigurationException {
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
    public void _commit(boolean finishChosen) throws CommitStepException {
        // Nothing to do here
    }

    private void _reset() {
        setDownloadCalled(false);
        progressBarLabel.setText("<html>Configure project and then click <b>[ GENERATE Ctrl + ⏎ ]</b> button above.");
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
            if (downloadItem.isComplete()) {
                startSpringIOModuleWizardStep.setDownloadCalled(true);
                String downloadItemLocation = downloadItem.getFullPath();
                String suggestedFileName = downloadItem.getSuggestedFileName();
                String suggestedFileNameSansExtension = suggestedFileName.replaceFirst("\\.zip", "");
                context.putUserData(StartSpringIOModuleBuilder.START_SPRING_IO_DOWNLOADED_ZIP_LOCATION, downloadItemLocation);
                moduleBuilder.setProjectName(suggestedFileNameSansExtension);
                parent.setCursor(Cursor.getDefaultCursor());
                progressBarLabel.setText("Downloaded project '\" + suggestedFileNameSansExtension + \"' zip to: '" + downloadItemLocation + "'. Click Next below.");
                progressBar.setIndeterminate(false);
            }
        }
    }
}
