package sandipchitale;

import com.intellij.ide.util.PropertiesComponent;

final class SpringInitializrConfig {
    private static final String SPRINGINITIALIZR_LAST_URL = "sandipchitale.springinitializr.lastUrl";
    private static final String SPRINGINITIALIZR_LAST_URL_DEFAULT_VALUE = "https://start.spring.io/";

    static String getSpringInitializrUrl() {
        String savedSpringInitializrUrl = PropertiesComponent.getInstance().getValue(SPRINGINITIALIZR_LAST_URL);
        if (savedSpringInitializrUrl == null) {
            return reset();
        } else {
            return savedSpringInitializrUrl;
        }
    }

    static void setSpringInitializrUrl(String springInitializrUrl) {
        PropertiesComponent.getInstance().setValue(SPRINGINITIALIZR_LAST_URL, springInitializrUrl);
    }

    static String reset() {
        PropertiesComponent.getInstance().setValue(SPRINGINITIALIZR_LAST_URL, SPRINGINITIALIZR_LAST_URL_DEFAULT_VALUE);
        return SPRINGINITIALIZR_LAST_URL_DEFAULT_VALUE;
    }
}
