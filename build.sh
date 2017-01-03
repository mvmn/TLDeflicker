XPROJECT_NAME=TLDeflicker

rm -rf ./target
mvn clean package
XPROJECTVERSION=$(cat pom.xml| grep version | head -n 1 | cut -d ">" -f 2 | cut -d "<" -f 1)
XPROJECTFOLDER=$XPROJECT_NAME-$XPROJECTVERSION

mkdir -p ./target/$XPROJECTFOLDER/lib/
mv ./target/lib/*.jar ./target/$XPROJECTFOLDER/lib/
mv ./target/*.jar ./target/$XPROJECTFOLDER/
mv ./target/run.bat ./target/$XPROJECTFOLDER/
chmod +x ./target/run.sh
mv ./target/run.sh ./target/$XPROJECTFOLDER/

cd target
zip -r ./$XPROJECTFOLDER.zip ./$XPROJECTFOLDER
cd ..
