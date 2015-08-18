#!/bin/bash

set -ev

DMG_FILE=artifacts/triplea_$TRAVIS_TAG_mac.dmg
APP_FILE=release/Triplea.app
hdiutil detach /Volumes/TripleA


TMP_DIR=release.mac

mkdir -p $TMP_DIR/Contents/MacOS $TMP_DIR/Contents/Resources/Java
echo "copy file="/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub todir={app.file}/Contents/MacOS/"



