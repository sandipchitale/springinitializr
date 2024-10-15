package sandipchitale;

import com.intellij.ide.util.PropertiesComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class SpringInitializrConfig {
    static final String SPRINGINITIALIZR_NAME_DEFAULT_VALUE = "DEFAULT";
    static final String SPRINGINITIALIZR_URL_DEFAULT_VALUE = "https://start.spring.io/";

    record SavedConfig(String name, String url) {
        public String toString() {
            return name();
        }

        static SavedConfig of(String savedConfigAsString) {
            String[] savedConfigParts = savedConfigAsString.split("\\|");
            return new SavedConfig(savedConfigParts[0],
                    savedConfigParts[1]);
        }
    }

    static final String SPRINGINITIALIZR_SAVED_CONFIGS = "sandipchitale.springinitializr.savedConfigs";
    static final SavedConfig SPRINGINITIALIZR_SAVED_CONFIG_DEFAULT_VALUE = new SavedConfig(SPRINGINITIALIZR_NAME_DEFAULT_VALUE, SPRINGINITIALIZR_URL_DEFAULT_VALUE);

    static List<SavedConfig> getSpringInitializrSavedConfigs() {
        List<String> savedConfigsList = PropertiesComponent.getInstance().getList(SPRINGINITIALIZR_SAVED_CONFIGS);
        if (savedConfigsList == null) {
            return resetSpringInitializrSavedConfigs();
        } else {
            return savedConfigsList
                    .stream()
                    .map(SavedConfig::of)
                    .collect(Collectors.toList());
        }
    }

    static List<SavedConfig> resetSpringInitializrSavedConfigs() {
        List<SavedConfig> savedConfigs = new ArrayList<>();
        savedConfigs.add(SPRINGINITIALIZR_SAVED_CONFIG_DEFAULT_VALUE);
        PropertiesComponent.getInstance().setList(SPRINGINITIALIZR_SAVED_CONFIGS,
                savedConfigs
                        .stream()
                        .map((SavedConfig savedConfig) -> String.format("%s|%s", savedConfig.name(), savedConfig.url()))
                        .collect(Collectors.toList())
        );
        return savedConfigs;
    }

    static void deleteSavedConfig(String name) {
        List<SavedConfig> springInitializrSavedConfigs = getSpringInitializrSavedConfigs();
        springInitializrSavedConfigs = springInitializrSavedConfigs
                .stream()
                .filter((SavedConfig sc) -> !sc.name().equals(name))
                .toList();
        PropertiesComponent.getInstance().setList(SPRINGINITIALIZR_SAVED_CONFIGS,
                springInitializrSavedConfigs.stream()
                        .sorted(Comparator.comparing(SavedConfig::name))
                        .map(sc -> String.format("%s|%s", sc.name(), sc.url()))
                        .collect(Collectors.toList()));
    }

    static SavedConfig setConfig(String name, String url) {
        List<SavedConfig> springInitializrSavedConfigs = getSpringInitializrSavedConfigs();
        springInitializrSavedConfigs = springInitializrSavedConfigs
                .stream()
                .filter((SavedConfig sc) -> !sc.name().equals(name))
                .collect(Collectors.toList());
        SavedConfig savedConfig = new SavedConfig(name, url);
        springInitializrSavedConfigs.add(savedConfig);

        PropertiesComponent.getInstance().setList(SPRINGINITIALIZR_SAVED_CONFIGS,
                springInitializrSavedConfigs.stream()
                        .sorted(Comparator.comparing(SavedConfig::name))
                        .map(sc -> String.format("%s|%s", sc.name(), sc.url()))
                        .collect(Collectors.toList()));
        return savedConfig;
    }
}
