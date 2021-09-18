export ALIAS='ktor'
export PASSWD=''
export KEYSTORE_FILE_NAME='tagged-notes-ktor'
export JKS_KEYSTORE_PATH="./$KEYSTORE_FILE_NAME.jks"
keytool -importkeystore -alias $ALIAS -srckeystore $JKS_KEYSTORE_PATH -srcstoretype JKS \
  -srcstorepass $PASSWD -storepass $PASSWD \
  -deststoretype BKS -providerpath './bcprov-jdk15on-169.jar' \
  -provider org.bouncycastle.jce.provider.BouncyCastleProvider -destkeystore $KEYSTORE_FILE_NAME.bks
