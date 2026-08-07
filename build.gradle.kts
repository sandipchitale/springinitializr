import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "sandipchitale"
version = "1.0.62"

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2026.2.0.1") {
            useInstaller = true
            useCache = true
        }
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.modules.jcef")
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginVerification {
        ides {
            recommended()
        }
        failureLevel.set(listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.INVALID_PLUGIN
        ))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    runIde {
        // Without this, JCEF's own process sandbox fails to initialize when launched
        // from the unpacked Gradle runIde distribution (not a signed .app bundle), so
        // the start.spring.io browser never renders. Installed builds are unaffected,
        // since they run from a properly signed IDE installation.
        jvmArgs("-Dide.browser.jcef.sandbox.enable=false")
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.release.set(21)
    }

    patchPluginXml {
        sinceBuild.set("262")
        untilBuild.set("262.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(providers.gradleProperty("intellijPublishToken"))
    }
}
