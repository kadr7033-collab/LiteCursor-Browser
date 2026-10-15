#!/bin/bash
echo "=== Compilador de LiteCursor Browser ==="
if ! command -v gradle &> /dev/null; then
    echo "Instalando Gradle y OpenJDK 17..."
    sudo apt update && sudo apt install -y openjdk-17-jdk gradle android-sdk
fi

export ANDROID_HOME=/usr/lib/android-sdk
echo "Compilando APK..."
gradle assembleDebug

echo "¡Listo! Tu APK está en: app/build/outputs/apk/debug/app-debug.apk"
