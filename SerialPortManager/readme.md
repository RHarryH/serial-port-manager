Compile with below flag (in Eclipse/Maven config)
-Djava.library.path=${project_loc:/SerialPortManager}/lib

Installing RXTX library as local Maven artifact
mvn install:install-file -Dfile=RXTXcomm.jar -DgroupId=org.rxtx -DartifactId=RXTXcomm -Dversion=1.0 -Dpackaging=jar

Running app (assuming that in the same directiory rxtsSerial.dll was placed)
java -jar SerialPortManager-0.0.1-jar-with-dependencies.jar

Full app requires:
- e(fx)clips (for Java 1.8)
https://projects.eclipse.org/projects/technology.efxclipse
- ArcGIS Runtime SDK for Java (Version 100.1.0)
https://developers.arcgis.com/java/
with configuration:
- system variable: 
ARCGISRUNTIMESDKJAVA_100_1_0

Useful links:
http://www.movable-type.co.uk/scripts/latlong.html
https://developers.google.com/maps/documentation/javascript/examples/map-coordinates
https://en.wikibooks.org/wiki/Trigonometry/Compass_Bearings#/media/File:Compass_rose.png
http://www.gpsinformation.org/dale/nmea.htm