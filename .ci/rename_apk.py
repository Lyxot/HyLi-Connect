#!/usr/bin/python3
import os
import json

ANDROID_KEYSTORE_FILE=os.getenv("ANDROID_KEYSTORE_FILE")

with open('app/build/outputs/apk/release/output-metadata.json', 'r') as file:
    data = json.load(file)
    VERSION_NAME = data['elements'][0]['versionName']
    VERSION_CODE = data['elements'][0]['versionCode']
    os.rename('app/build/outputs/apk/release/app-release.apk', "app/build/outputs/apk/release/Hyli-Connect-"+str(VERSION_NAME)+"("+str(VERSION_CODE)+").apk")
    os.rename('app/build/outputs/apk/release/app-release.apk.idsig', "app/build/outputs/apk/release/Hyli-Connect-"+str(VERSION_NAME)+"("+str(VERSION_CODE)+").apk.idsig")

if os.path.exists(ANDROID_KEYSTORE_FILE):
    os.remove(ANDROID_KEYSTORE_FILE)