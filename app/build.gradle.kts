plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.proyectointegrador"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.proyectointegrador"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Biometría para login con huella
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Mapbox para mostrar el mapa y la ruta
    implementation("com.mapbox.maps:android:11.8.0")
    
    // BCrypt para encriptación de contraseñas (Cliente)
    implementation("org.mindrot:jbcrypt:0.4")
    
    // ZXing para escaneo de códigos QR (Ambos)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")
    
    // CameraX para escaneo de QR (Chofer)
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // SignalR para sincronización en tiempo real entre cliente y chofer
    implementation("com.microsoft.signalr:signalr:7.0.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}