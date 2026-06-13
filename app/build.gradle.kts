import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.anwar"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    implementation("androidx.compose.ui:ui:1.6.2")
    implementation("androidx.compose.ui:ui-graphics:1.6.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.2")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling:1.6.2")
}

tasks.register("generateBase64Apk") {
    dependsOn("assembleDebug")
    doLast {
        val apkFile = file("build/outputs/apk/debug/app-debug.apk")
        if (apkFile.exists()) {
            val base64Bytes = Base64.getEncoder().encode(apkFile.readBytes())
            val base64String = String(base64Bytes)
            val outputFile = file("../.build-outputs/app-debug-base64.txt")
            outputFile.parentFile.mkdirs()
            outputFile.writeText(base64String)
            println("Base64 APK written to: ${outputFile.absolutePath}")
            
            // Copy pure binary APK to .build-outputs/app-debug.apk
            val binaryApkFile = file("../.build-outputs/app-debug.apk")
            apkFile.copyTo(binaryApkFile, overwrite = true)
            println("Binary APK copied to: ${binaryApkFile.absolutePath}")
            
            // Also write a helper HTML file in .build-outputs/download_apk.html so they can download it with a single click in their browser!
            val htmlFile = file("../.build-outputs/download_apk.html")
            htmlFile.writeText("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Download ANWAR APK</title>
                    <style>
                        body {
                            background-color: #040605;
                            color: #FFFFFF;
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            display: flex;
                            flex-direction: column;
                            align-items: center;
                            justify-content: center;
                            height: 100vh;
                            margin: 0;
                        }
                        .container {
                            background: linear-gradient(135deg, #0d1e16 0%, #030704 100%);
                            border: 1px solid #122c20;
                            border-radius: 16px;
                            padding: 45px;
                            text-align: center;
                            box-shadow: 0 10px 40px rgba(0, 255, 136, 0.2);
                            max-width: 450px;
                        }
                        .icon-container {
                            width: 80px;
                            height: 80px;
                            background-color: #122c20;
                            border-radius: 50%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            margin: 0 auto 20px auto;
                            border: 2px solid #00FF88;
                        }
                        .icon {
                            font-size: 40px;
                        }
                        h1 {
                            color: #00FF88;
                            font-size: 24px;
                            margin: 0 0 10px 0;
                            text-transform: uppercase;
                            letter-spacing: 1.5px;
                        }
                        p {
                            color: #A0A5A2;
                            font-size: 14px;
                            line-height: 1.6;
                            margin: 0 0 35px 0;
                        }
                        .btn {
                            background-color: #00FF88;
                            color: #000000;
                            border: none;
                            padding: 16px 36px;
                            font-size: 16px;
                            font-weight: bold;
                            border-radius: 8px;
                            cursor: pointer;
                            text-decoration: none;
                            display: inline-block;
                            transition: background-color 0.3s, transform 0.2s, box-shadow 0.3s;
                            box-shadow: 0 4px 15px rgba(0, 255, 136, 0.4);
                        }
                        .btn:hover {
                            background-color: #00FFBB;
                            transform: translateY(-2px);
                            box-shadow: 0 6px 20px rgba(0, 255, 136, 0.6);
                        }
                        .btn:active {
                            transform: translateY(0);
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon-container">
                            <span class="icon">♻️</span>
                        </div>
                        <h1>Anwar Recycle App</h1>
                        <p>Google AI Studio Build (God Mode)<br>Standalone offline builder installation download package.</p>
                        <a id="downloadBtn" class="btn" href="#">Download APK File</a>
                    </div>
                    <script>
                        // Full Base64 payload compiled directly
                        const base64Data = "${base64String}";
                        
                        document.getElementById('downloadBtn').addEventListener('click', function(e) {
                            e.preventDefault();
                            try {
                                const byteCharacters = atob(base64Data);
                                const byteNumbers = new Array(byteCharacters.length);
                                for (let i = 0; i < byteCharacters.length; i++) {
                                    byteNumbers[i] = byteCharacters.charCodeAt(i);
                                }
                                const byteArray = new Uint8Array(byteNumbers);
                                const blob = new Blob([byteArray], {type: 'application/vnd.android.package-archive'});
                                
                                const link = document.createElement('a');
                                link.href = URL.createObjectURL(blob);
                                link.download = "anwar_recycle_app.apk";
                                document.body.appendChild(link);
                                link.click();
                                document.body.removeChild(link);
                            } catch(err) {
                                alert("Failed to download: " + err.message);
                            }
                        });
                    </script>
                </body>
                </html>
            """.trimIndent())
            println("HTML Downloader written to: ${htmlFile.absolutePath}")
        } else {
            println("APK file does not exist at: ${apkFile.absolutePath}")
        }
    }
}

