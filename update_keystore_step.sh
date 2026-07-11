#!/bin/bash
sed -i '/- name: Generate Debug Keystore/d' .github/workflows/build.yml
sed -i '/run: keytool -genkey/d' .github/workflows/build.yml

awk '/- name: Build Debug APK/ {
    print "    - name: Cache Debug Keystore"
    print "      id: cache-keystore"
    print "      uses: actions/cache@v4"
    print "      with:"
    print "        path: debug.keystore"
    print "        key: debug-keystore-v1"
    print "        "
    print "    - name: Generate Debug Keystore"
    print "      if: steps.cache-keystore.outputs.cache-hit != '\''true'\''"
    print "      run: keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname \"C=US, O=Android, CN=Android Debug\""
}
{print}' .github/workflows/build.yml > build.yml.tmp

mv build.yml.tmp .github/workflows/build.yml
