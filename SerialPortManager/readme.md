Compile with below flag (in Eclipse)
-Djava.library.path=${project_loc:/SerialPortManager}/lib

Installing RXTX library as local Maven artifact
mvn install:install-file -Dfile=RXTXcomm.jar -DgroupId=org.rxtx -DartifactId=RXTXcomm -Dversion=1.0 -Dpackaging=jar

Running app (assuming that in the same directiory rxtsSerial.dll was placed)
java -jar SerialPortManager-0.0.1-jar-with-dependencies.jar