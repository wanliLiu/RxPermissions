#!/usr/bin/env bash

#rm -rf $HOME/.gradle/caches
#rm -rf .gradle

./gradlew clean build bintrayUpload

#./gradlew -P repositoryUrl=file://C://Users/Soli/.m2/repository uploadArchives
#
#./gradlew -P repositoryUrl=file://D://AndroidStudio/sdk/extras/taihe/m2repository/ uploadArchives