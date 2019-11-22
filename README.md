# openroberta-connector [![Build Status](https://travis-ci.org/OpenRoberta/openroberta-connector.svg?branch=master)](https://travis-ci.org/OpenRoberta/openroberta-connector)

### Supported robots
- EV3Lejos
- Arduino Uno
- Arduino Mega
- Arduino Nano
- BOB3
- Bot'n'Roll
- mBot
- NAO
- Festo Bionics4Education (may require [this driver](https://www.silabs.com/products/development-tools/software/usb-to-uart-bridge-vcp-drivers) on macOS)

Standalone program for connecting robot hardware to the Open Roberta lab using
an usb or ssh connection.

### Fast installation with maven

#### Clone the repository and compile
    git clone git://github.com/OpenRoberta/openroberta-connector.git
    cd openroberta-connector
    mvn clean install

### Run Open Roberta Connector
For running the Connector use Java.

    java -jar target/OpenRobertaConnector-*.jar

### Development notes

You can follow the test status on https://travis-ci.org/OpenRoberta/.

Development happens in the `develop` branch. Please sent PRs against that
branch.

    git clone git://github.com/OpenRoberta/openroberta-connector.git
    cd openroberta-connector
    git checkout -b develop origin/develop
    
To regenerate the `esptool.exe` when upgrading the `esptool` version download the wanted version and run this:
```
pip install pyinstaller
pyinstaller --onefile \
            --specpath build_tmp \
            --workpath build_tmp/build \
            --distpath build_tmp/dist \
            esptool.py
```
to generate the binary.
    
### Installer creation

#### Automatically

TravisCI automatically generates installers for Linux, Windows and OSX when a tag is created.
These are added to a GitHub Releases draft for the tag.
 
#### Manual

Linux:
- run `mvn clean install` in the project directory
- run `release.sh linux` in the `installers` directory
- add the version to the resulting file

Windows:
- download and install [WiX Toolset](https://github.com/wixtoolset/wix3/releases)
- download and install [WDK 8.1](https://www.microsoft.com/en-us/download/details.aspx?id=42273)
  - or run
    - `curl 'https://download.microsoft.com/download/0/8/C/08C7497F-8551-4054-97DE-60C0E510D97A/wdk/wdksetup.exe' --output wdksetup.exe`
    - `./wdksetup.exe //features + //q //norestart //ceip off`
- add a environment variable `WIX=<wix-install-path>\bin`
- run `mvn clean install` in the project directory
- run `release.sh windows` in the `installers` directory (e.g. in Git Bash)
- add the version to the resulting files

Mac:
- run `mvn clean install` in the project directory
- run `release.sh osx` in the `installers` directory
- add the version to the resulting file

### Release
To release run `mvn release:clean release:prepare` on the `develop` branch.
The maven release plugin will ask for some information regarding the version numbers and automatically create commits and a tag with the updated versions.
Now, merge the used branch into `master` and remove the last (`prepare for next development iteration`) commit with `git reset HEAD~1 --hard`, as it is not needed on `master`.
Finally, push `master`, `develop` and the tag (`git push --tags`).