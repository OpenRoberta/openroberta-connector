#!/bin/bash
# release - Release and installer creation script for the Open Roberta Lab connection program

CURRENT_TAG=$(git describe --tags)

usage() {
    echo "Usage: release <linux/windows/osx>"
    echo ""
    echo "Specify the operating system that a release should be created for."
    exit
}

create_linux() {
    echo "Creating Linux package"
    cp -R linux OpenRobertaConnector
    chmod +x OpenRobertaConnector/resources/linux/arduino/avrdude-32
    chmod +x OpenRobertaConnector/resources/linux/arduino/avrdude-64
    chmod +x OpenRobertaConnector/resources/linux/arduino/avrdude-arm32
    tar -zcvf OpenRobertaConnectorLinux-$CURRENT_TAG.tar.gz OpenRobertaConnector
    rm -rf OpenRobertaConnector
}

create_windows() {
    echo "Creating Windows installers"
    cd windows
    echo $WIX
    # create wxs files for folders, they are referenced via their -cg argument in the setup.wxs, -var argument will be passed in candle call
    "$WIX"/heat.exe dir resources -ag -cg Resources -dr INSTALLDIR -var var.ResourcesDir -o resources.wxs
    "$WIX"/heat.exe dir libs -ag -cg Libs -dr INSTALLDIR -var var.LibsDir -o libs.wxs
    # heat on a folder does not add the folder itself to the SourceDir, add custom variables via -d and -var arguments
    "$WIX"/candle.exe -dResourcesDir=resources -dLibsDir=libs setup.wxs resources.wxs libs.wxs
    "$WIX"/light.exe -out OpenRobertaConnectorSetupDE-$CURRENT_TAG.msi -ext WixUIExtension -cultures:de-DE setup.wixobj resources.wixobj libs.wixobj
    "$WIX"/light.exe -out OpenRobertaConnectorSetupEN-$CURRENT_TAG.msi -ext WixUIExtension -cultures:en-US setup.wixobj resources.wixobj libs.wixobj
    mv OpenRobertaConnectorSetupDE-$CURRENT_TAG.msi ..
    mv OpenRobertaConnectorSetupEN-$CURRENT_TAG.msi ..
}

create_osx() {
    echo "Creating OSX package"
    chmod +x osx/resources/osx/arduino/avrdude
    cd osx && ./appify start.sh OpenRobertaConnector
    mkdir -p package/ORCON.pkg/Payload/Applications/OpenRobertaConnector.app/
    cp -a resources OpenRobertaConnector.app/Contents/
    cp -a libs OpenRobertaConnector.app/Contents/
    cp -a OpenRobertaConnector.app package/ORCON.pkg/Payload/Applications/
    pkgutil --flatten-full package OpenRobertaConnectorMacOSX-$CURRENT_TAG.pkg
    mv OpenRobertaConnectorMacOSX-$CURRENT_TAG.pkg ..
    rm -rf OpenRobertaConnector.app
}


if [ "$1" == "" ]; then
    usage
else
    case $1 in
        linux) create_linux ;;
        windows) create_windows ;;
        osx) create_osx ;;
    esac
fi
echo "Release finished"
