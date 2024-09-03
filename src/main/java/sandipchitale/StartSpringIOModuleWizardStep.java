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
import com.intellij.util.ui.EmptyClipboardOwner;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class StartSpringIOModuleWizardStep extends ModuleWizardStep {

    private final StartSpringIOModuleBuilder moduleBuilder;
    private final WizardContext context;
    private final Disposable parentDisposable;

    private SimpleToolWindowPanel contentToolWindow;

    private JLabel progressBarLabel;
    private JProgressBar progressBar;

    private boolean downloadCalled;

    public StartSpringIOModuleWizardStep(StartSpringIOModuleBuilder moduleBuilder, WizardContext context, Disposable parentDisposable) {
        this.moduleBuilder = moduleBuilder;
        this.context = context;
        this.parentDisposable = parentDisposable;
    }

    /** Update UI from ModuleBuilder and WizardContext */
    public void updateStep() {
    }

    @Override
    public JComponent getComponent() {
        contentToolWindow = new SimpleToolWindowPanel(true, true);
        JPanel progressBarWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        progressBarWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        progressBarLabel = new JLabel(" ");
        progressBarWrapper.add(progressBarLabel);

        progressBar = new JProgressBar();
        progressBarWrapper.add(progressBar);

        JBCefBrowser browser = new JBCefBrowser("https://start.spring.io");
        JBCefClient client = browser.getJBCefClient();
        client.addDownloadHandler(new DownloadHandler(this, context, contentToolWindow, progressBar, progressBarLabel), browser.getCefBrowser());

        contentToolWindow.add(browser.getComponent(), BorderLayout.CENTER);
        contentToolWindow.add(progressBarWrapper, BorderLayout.SOUTH);

        return contentToolWindow;
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
    }

    private void _reset() {
        setDownloadCalled(false);
        progressBar.setIndeterminate(false);
        progressBarLabel.setText("Configure project and then click Generate to download project.");
    }

    private void setDownloadCalled(boolean downloadCalled) {
        this.downloadCalled = downloadCalled;
    }
    
    private record DownloadHandler(StartSpringIOModuleWizardStep startSpringIOModuleWizardStep,
                                   WizardContext context,
                                   JComponent parent,
                                   JProgressBar progressBar,
                                   JLabel progressBarLabel) implements CefDownloadHandler {

        @Override
        public void onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName, CefBeforeDownloadCallback callback) {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            progressBar.setIndeterminate(true);
            progressBarLabel.setText("Generating, downloading project" + suggestedName + ".");
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
                parent.getToolkit().getSystemClipboard().setContents(new StringSelection(suggestedFileNameSansExtension), EmptyClipboardOwner.INSTANCE);
                parent.setCursor(Cursor.getDefaultCursor());
                progressBar.setIndeterminate(false);
                progressBarLabel.setText("Downloaded project at: " + downloadItemLocation + " . Click Next and paste project name in clipboard " + suggestedFileNameSansExtension);
            }
        }
    }
}