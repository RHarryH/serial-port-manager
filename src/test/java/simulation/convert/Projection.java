package simulation.convert;

import java.awt.Point;
import java.awt.geom.Point2D;

import com.navigation.GPSData;

/**
 * Odwzorowanie współrzędnych geograficznych z wykorzystaniem 
 * odwzorowania walcowego równokątnego (odwzorowanie Merkatora).
 * Bazuje na rozwiązaniach Google Maps API.
 * latitude - szerokość geograficzna (współrzędna y)
 * longitude - długość geograficzna (współrzedna x)
 * Pipeline: współrzędne geograficzne (polarne) -> współrzędne świata (Merkator) -> 
 * współrzędne pikseli (z uwzględnieniem zoomu) -> współrzędne okna (obszar roboczy)
 * @author Harry
 *
 */
public final class Projection {
    private final int TILE_SIZE = 256; // rozmiar kafelka
    
    private Point2D.Double pixelOrigin;
    private double pixelsPerLonDegree;
    private double pixelsPerLonRadian;
    
    private int numTiles; // liczba kafelków
    private Point windowOrigin;

    /**
     * Konstruktor
     * @param zoom zbliżenie
     */
    public Projection(int zoom) {
        this.pixelOrigin = new Point2D.Double(TILE_SIZE / 2.0,TILE_SIZE / 2.0);
        this.pixelsPerLonDegree = TILE_SIZE / 360.0;
        this.pixelsPerLonRadian = TILE_SIZE / (2 * Math.PI);
        this.numTiles = 1 << zoom;
    }

    /**
     * Konwersja współrzednych geograficznych na współrzedne świata
     * @param data
     * @return world coordinate
     */
    public Point2D.Double fromGeoToWorld(GPSData data) {
    	Point2D.Double point = new Point2D.Double(0, 0);

        point.x = pixelOrigin.x + data.getLongitude() * pixelsPerLonDegree;
        
        double siny = Math.sin(Math.toRadians(data.getLatitude()));

        // Truncating to 0.9999 effectively limits latitude to 89.189. This is
        // about a third of a tile past the edge of the world tile.
        siny = Math.min(
    		Math.max(
    				siny
    				, -0.9999)
    		,0.9999);
        
        point.y = pixelOrigin.y + 0.5 * Math.log((1 + siny) / (1 - siny)) *- pixelsPerLonRadian;

        //point.x = point.x * numTiles;
        //point.y = point.y * numTiles;
        return point;
    }

    /**
     * Konwersja współrzednych świata na współrzedne geograficzne
     * @param point
     * @return współrzedne geograficzne
     */
    public GPSData fromWorldToGeo(Point2D.Double point) {
        //point.x = point.x / numTiles;
        //point.y = point.y / numTiles;       

        double lng = (point.x - pixelOrigin.x) / pixelsPerLonDegree;
        double latRadians = (point.y - pixelOrigin.y) / - pixelsPerLonRadian;
        double lat = Math.toDegrees(2 * Math.atan(Math.exp(latRadians)) - Math.PI / 2.0);
        return new GPSData(lat, lng);
    }
    
    /**
     * Konwersja współrzednych świata na współrzędne pikseli
     * @param worldCoordinate
     * @return
     */
    private Point getPixelCoordinate(Point2D.Double worldCoordinate) {	
    	return new Point(
    			(int)Math.floor(worldCoordinate.x * numTiles), 
    			(int)Math.floor(worldCoordinate.y * numTiles));
    }
    
    /**
     * Konwersja współrzednych świata na współrzędne kafelka
     * @param worldCoordinate
     * @return
     */
    private Point getTileCoordinate(Point2D.Double worldCoordinate) {
    	return new Point(
    			(int)Math.floor(worldCoordinate.x * numTiles / TILE_SIZE), 
    			(int)Math.floor(worldCoordinate.y * numTiles / TILE_SIZE));
    }
    
    /**
     * Ustaw środek okna
     * @param worldCoordinate
     * @param width
     * @param height
     * @return
     */
    public void setWindowOrigin(Point2D.Double worldCoordinate, int width, int height) {
    	Point pixelCoordinate = getPixelCoordinate(worldCoordinate);
    	this.windowOrigin = new Point(pixelCoordinate.x - width/2, pixelCoordinate.y - height/2);
    }
    
    /**
     * Zwraca środek okna
     * @return środek okna
     */
    public Point getWindowOrigin() {
    	return windowOrigin;
    }
    
    /**
     * Konwertuje współrzedne świata na współrzedne okna
     * @param worldCoordinate
     * @return
     */
    public Point toWindowCoords(Point2D.Double worldCoordinate) {
    	Point pixelCoordinate = getPixelCoordinate(worldCoordinate);
    	return new Point((int)(pixelCoordinate.x - windowOrigin.x), (int)(pixelCoordinate.y - windowOrigin.y));
    }
    
    /**
     * Konwertuje punkt (współrzędne okna) na współrzędne świata
     * @param x
     * @param y
     * @return
     */
    public Point2D.Double toWorldCoords(int x, int y) {
    	Point pixelCoordinate = new Point(x + windowOrigin.x, y + windowOrigin.y);
    	return new Point2D.Double(pixelCoordinate.x / (double)numTiles, pixelCoordinate.y / (double)numTiles);
    }

    public static void main(String args[]) {
        Projection gmap2 = new Projection(20);

        GPSData data = new GPSData(41.850033, -87.6500523);
        System.out.println("Chicago: " + data);
        
        Point2D.Double worldCoordinate = gmap2.fromGeoToWorld(data);
        System.out.println("World cordinates: " + worldCoordinate.x + " " + worldCoordinate.y);
        
        System.out.println("Pixel coordinates: " + gmap2.getPixelCoordinate(worldCoordinate));
        System.out.println("Tile coordinates: " + gmap2.getTileCoordinate(worldCoordinate));
        
        gmap2.setWindowOrigin(worldCoordinate, 800, 600);

        System.out.println("Window origin: " + gmap2.getWindowOrigin());
        
        Point window = gmap2.toWindowCoords(worldCoordinate);
        System.out.println("Window coords: " + window);
        
        System.out.println("Back world cordinates: " + gmap2.toWorldCoords(window.x, window.y));

        GPSData coordinates = gmap2.fromWorldToGeo(worldCoordinate);
        System.out.println("Chicago back: " + coordinates);
    }
}