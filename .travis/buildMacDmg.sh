#!/bin/bash

set -ev

DMG_FILE=artifacts/triplea_$TRAVIS_TAG_mac.dmg
APP_FILE=release/Triplea.app
hdiutil detach /Volumes/TripleA


TMP_DIR=release.mac

mkdir -p $TMP_DIR/Contents/MacOS $TMP_DIR/Contents/Resources/Java
echo "copy file="/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub todir={app.file}/Contents/MacOS/"

cp bin/triplea.jar $TMP_DIR/Contents/Resources/Java/
cp Info.plist $TMP_DIR/Contents/
cp icons/icons.icns $TMP_DIR/Contents/Resources/
cp -r icons doc assets license dice_servers maps old $TMP_DIR/Contents/Resources/
cp readme.html MacOS_users_read_this_first.txt TripleA_RuleBook.pdf run-headless-game-host-mac-os.sh $TMP_DIR/Contents/Resources/
chmod 755 $TMP_DIR/Contents/Resources/*.sh
 
