#!/bin/bash

APP_NAME="sourcebox"
JAR_FILE="sourcebox-fat.jar"
INSTALL_DIR="$HOME/.local/share/$APP_NAME"
SERVICE_DIR="$HOME/.config/systemd/user"
SERVICE_FILE="$SERVICE_DIR/${APP_NAME}.service"
TARGET_JAR="$INSTALL_DIR/lib/$JAR_FILE"

if [ -f "$TARGET_JAR" ]; then
    echo "🔄 $APP_NAME existed，updating JAR ..."
else
    echo "⬇️  installing $APP_NAME..."
    mkdir -p "$INSTALL_DIR"
fi

cp -r ./* $INSTALL_DIR/

mkdir -p "$SERVICE_DIR"

echo "📄 creating systemd file..."
cat > "$SERVICE_FILE" <<EOF
[Unit]
Description=The SourceBox Web Service
After=network.target

[Service]
Type=simple
ExecStart=java -Dlogging.type=net.cofcool.sourcebox.logging.JULLogger -jar $TARGET_JAR --mode=WEB
Restart=on-failure

[Install]
WantedBy=default.target
EOF

systemctl --user daemon-reexec
systemctl --user daemon-reload

systemctl --user enable "${APP_NAME}.service"
systemctl --user restart "${APP_NAME}.service"

echo "✅ $APP_NAME installed and started"
