import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("dev.jamiecraane.plugins.kmmresources")
}

repositories {
    google()
    jcenter()
    maven {
        setUrl("https://jitpack.io")
    }
    maven {
        setUrl("https://maven.google.com/")
    }
}

kotlin {
    android()
    ios {
        binaries {
            framework {
                baseName = "shared"
            }
        }
    }

    js {
        browser()
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.google.android.material:material:1.2.1")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13")
            }
        }
        val iosMain by getting
        val iosTest by getting
    }
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.2"

    defaultConfig {
        applicationId = "nl.jcraane.androidapp"
        minSdkVersion(22)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.21")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    testImplementation("junit:junit:4.+")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

kmmResourcesConfig {
    androidApplicationId.set("nl.jcraane.androidapp")
    androidSourceFolder.set("main")
    packageName.set("com.example.project")
    defaultLanguage.set("nl")
    input.set(File(project.projectDir.path, "generic.yaml"))
    output.set(project.projectDir)
    useDefaultTranslationIfNotInitialized.set(true)
    iosSourceFolder.set("iosX64Main")
}

val plutil = tasks["executePlutil"]
val generateLocalizations = tasks["generateLocalizations"]
plutil.dependsOn(generateLocalizations)

//tasks["preBuild"].dependsOn(generateLocalizations)
tasks["preBuild"].dependsOn(plutil)

val packForXcode by tasks.creating(Sync::class) {
    group = "build"
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"
    val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
    val framework = kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)
    val targetDir = findProperty("configuration.build.dir")
    println("targetDir = $targetDir")
    if (targetDir == null) {
        System.err.println("configuration.build.dir is not defined. Please pass this property from the XCode build.")
    }
    from({ framework.outputDirectory })
    into(targetDir)

    doLast {
        println("COPY TO  = ${targetDir}/shared.framework")
        copy {
            from("${project.rootDir}/android-app/src/commonMain/resources/ios")
            into("${targetDir}/shared.framework")
        }
    }
}

tasks {
    /**
     * This sets up dependencies between the plutil task and compileKotlinIos* tasks. This
     * way common is recompiled if something in generic.yaml changes (so new ios resources
     * are generated). If the generic.yaml file is not changed, the resources are considered
     * up to date by Gradle.
     */
/*    named("compileKotlinIos32") {
        dependsOn(plutil)
    }*/
    named("compileKotlinIosArm64") {
        dependsOn(plutil)
    }
    named("compileKotlinIosX64") {
        dependsOn(plutil)
    }
}
