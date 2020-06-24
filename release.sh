#!/usr/bin/env bash

#rm -rf $HOME/.gradle/caches
#rm -rf .gradle

echo "BINTRAY_USERNAME：$BINTRAY_USERNAME"
echo "BINTRAY_API_KEY：$BINTRAY_API_KEY"

./gradlew clean build bintrayUpload

#./gradlew -P repositoryUrl=file:~/Documents/project/m2/repository uploadArchives
#
#./gradlew -P repositoryUrl=file://D://AndroidStudio/sdk/extras/taihe/m2repository/ uploadArchives