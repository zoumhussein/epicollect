package uk.ac.imperial.epi_collect.maps;

import com.google.android.maps.GeoPoint;

public class MapLocation {
	
	public String descrip;
	public double latitude, longitude;
	public long id;
	public GeoPoint geoPoint;
	
	public MapLocation(long thisid, String des, GeoPoint geo, double lat, double lon){
		
		id = thisid;
		descrip = des;
		geoPoint = geo;
		latitude = lat;
		longitude = lon;
	}
	
	public GeoPoint getGeoPoint(){
		return geoPoint;
	}
}
