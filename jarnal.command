#! /bin/bash

cd `dirname $0`
if [ -d upgrade-lib ]; then
	cp -fr lib old-lib
	cp -fr upgrade-lib lib
	rm -fr upgrade-lib 
fi
if [ -f upgrade-jarnal.jar ]; then
	cp -f jarnal.jar old-jarnal.jar
	cp -f upgrade-jarnal.jar jarnal.jar
	rm -f upgrade-jarnal.jar
fi
java -Dfile.encoding=UTF-8 -Xmx192m -jar jarnal.jar -t templates/default.jaj "$1" "$2" "$3" "$4" "$5"
