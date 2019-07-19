#!/bin/bash

dir="$( pwd )"

echo "[Desktop Entry]" > "/usr/share/applications/ORCON.desktop"
echo "Version=${version}" >> "/usr/share/applications/ORCON.desktop"
echo "Name=Open Roberta Connector" >> "/usr/share/applications/ORCON.desktop"
echo "Exec=java -jar -Dfile.encoding=utf-8 $dir/${artifactId}.jar" >> "/usr/share/applications/ORCON.desktop"
echo "Path=$dir" >> "/usr/share/applications/ORCON.desktop"
echo "Icon=$dir/OR.png" >> "/usr/share/applications/ORCON.desktop"
echo "Terminal=false" >> "/usr/share/applications/ORCON.desktop"
echo "Type=Application" >> "/usr/share/applications/ORCON.desktop"
#echo "StartupNotify=True" >> "/usr/share/applications/ORCON.desktop"
echo "Categories=Application;Development;" >> "/usr/share/applications/ORCON.desktop"

gpasswd -a ${SUDO_USER:-$USER} dialout
udevadm control --reload-rules

#useradd ${SUDO_USER:-$USER} dialout
exec su -l ${SUDO_USER:-$USER}

chmod u+x "/usr/share/applications/ORCON.desktop"
