export ALIAS=''
export PASSWD=''
export KEYSTORE_FILE_NAME='ktor-keystore'
export JKS_KEYSTORE_PATH=".../android/key-stores/$KEYSTORE_FILE_NAME.jks"
keytool -importkeystore -alias $ALIAS -srckeystore $JKS_KEYSTORE_PATH -srcstoretype JKS \
  -srcstorepass $PASSWD -storepass $PASSWD \
  -deststoretype BKS -providerpath '.../jdk-11.0.12/bin/bcprov-jdk15on-169.jar' \
  -provider org.bouncycastle.jce.provider.BouncyCastleProvider -destkeystore $KEYSTORE_FILE_NAME.bks
