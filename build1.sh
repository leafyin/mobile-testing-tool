#!/bin/bash
set -e

JAVA_HOME=your_jdk_path
APP_NAME=""

echo "=== 清理 ==="
rm -rf out ./jar/app.jar "$APP_NAME.app"
mkdir -p ./jar

echo "=== 编译 ==="
mkdir -p out/production/mobile-testing-tool
$JAVA_HOME/bin/javac -d out/production/mobile-testing-tool \
  src/com/leaf/utils/*.java \
  src/com/leaf/model/*.java \
  src/com/leaf/service/*.java \
  src/com/leaf/gui/*.java \
  src/GUI.java

echo "=== 打包 jar ==="
$JAVA_HOME/bin/jar cfve ./jar/app.jar GUI -C out/production/mobile-testing-tool .

echo "=== jpackage 生成 .app ==="
$JAVA_HOME/bin/jpackage \
  --name Logan \
  --input ./jar \
  --main-jar app.jar \
  --main-class GUI \
  --type app-image \
  --dest . \
  --add-modules java.base,java.desktop \
  --mac-package-name Logan \
  --app-version 1.0 \
  --verbose

echo "=== 清除 macOS 安全属性 + ad-hoc 签名 ==="
sudo xattr -rd com.apple.macl "$APP_NAME.app" 2>/dev/null
sudo codesign --force --deep --sign - "$APP_NAME.app"

echo ""
echo "完成: ./$APP_NAME.app"
