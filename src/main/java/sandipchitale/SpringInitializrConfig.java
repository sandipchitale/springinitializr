package sandipchitale;

class SpringInitializrConfig {
    private static String springInitializrUrl = "https://start.spring.io/";

    public static String getSpringInitializrUrl() {
        return springInitializrUrl;
    }

    public static void setSpringInitializrUrl(String springInitializrUrl) {
        SpringInitializrConfig.springInitializrUrl = springInitializrUrl;
    }
}
