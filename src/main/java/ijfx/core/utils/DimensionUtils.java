/*
    This file is part of ImageJ FX.

    ImageJ FX is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ImageJ FX is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
    
     Copyright 2015,2016 Cyril MONGIS, Michael Knop
	
 */
package ijfx.core.utils;

import mongis.ndarray.NDimensionalArray;
import net.imagej.display.ImageDisplay;
import net.imglib2.RandomAccessibleInterval;

/**
 * Set of methods easing dealing with dimension arrays in Datasets
 * @author cyril
 */
public class DimensionUtils {

    
    public static long[] getDimension(RandomAccessibleInterval interval) {
        
       long[] dimension = new long[interval.numDimensions()];
       interval.dimensions(dimension);
       
       return dimension;
        
    }
    
    public static long[] getDimensions(ImageDisplay imageDisplay) {
        
       long[] dimension = new long[imageDisplay.numDimensions()];
       imageDisplay.dimensions(dimension);
       
       return dimension;
        
    }
    
    /**
     * Transform a string containing a array of numbers in the form [x,y,z] into a long array
     * @param number array e.g [13,24,2,41]
     * @return long array
     */
    public static long[] readLongArray(String str) {
        // the string is usually a string of type "[12,324,32]
        // deleting the []
        
        if(str == null) return new long[]{};
        if(str.equals("null")) return new long[]{};
        int begin = 1;
        int end = str.length() - 1;
        str = str.substring(begin, end);
        String[] numbers = str.split(",");
        long[] longs = new long[numbers.length];
        for (int i = 0; i != numbers.length; i++) {
            longs[i] = Long.decode(numbers[i].trim());
        }
        return longs;
    }
    
    /**
     * Transform a non planar set of dimension ([2,1,4] into a plane set of dimension [0,0,2,1,4].
     * It basically creates a new long array with 0,0 in front of.
     * @param array
     * @return
     */
    public static long[] nonPlanarToPlanar(long[] array) {
      long[] result = new long[2+array.length];
      
      result[0] = 0;
      result[1] = 0;
      if(result.length == 2) return result;
      System.arraycopy(array, 0, result, 2, array.length);
      
      
      
      return result;
    }
    
    public static long[] planarToNonPlanar(long[] array) {
        if(array.length <= 2) return new long[0];
        else {
            
            long[] result = new long[array.length -2];
            System.arraycopy(array, 2, result, 0, array.length-2);
            
            return result;
        }
    }
    
    public static long[][] allNonPlanarPossibilities(long[] dimensions) {
        dimensions = planarToNonPlanar(dimensions);
        NDimensionalArray ndArray = new NDimensionalArray(dimensions);
        return ndArray.getPossibilities();
    }
    
    public static long[][] allPossibilities(long[] dimensions) {
         NDimensionalArray ndArray = new NDimensionalArray(dimensions);
        return ndArray.getPossibilities();
    }
   
    public static long[][] allPossibilities(ImageDisplay imageDisplay) {
        return allPossibilities(planarToNonPlanar(getDimensions(imageDisplay)));
    }
   
    
    
            
    
}
