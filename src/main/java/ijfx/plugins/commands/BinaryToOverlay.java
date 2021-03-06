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
package ijfx.plugins.commands;

import ij.ImagePlus;
import ij.blob.Blob;
import ij.blob.ManyBlobs;
import ij.process.ByteProcessor;
import ijfx.service.ImagePlaneService;
import ijfx.ui.main.ImageJFX;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.overlay.Overlay;
import net.imagej.overlay.PolygonOverlay;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.PolygonRegionOfInterest;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Cyril MONGIS, 2016
 */
@Plugin(type = Command.class, menuPath = "Image > Binary > Convert to overlay")
public class BinaryToOverlay implements Command {

    private final String USE_WHITE_PIXEL = "White pixels";

    private final String USE_BLACK_PIXEL = "Black pixels";

    @Parameter(required = true, label = "Object color", choices = {USE_BLACK_PIXEL, USE_WHITE_PIXEL})
    String objectColor;

    @Parameter
    DatasetService datasetService;

    @Parameter(type = ItemIO.INPUT)
    ImageDisplay display;

    @Parameter(type = ItemIO.OUTPUT)
    Overlay[] overlays;

    @Parameter
    ImagePlaneService imagePlaneService;

    @Parameter
    ImageDisplayService imageDisplayService;

    @Parameter
    Context context;

    Logger logger = ImageJFX.getLogger();

    @Override
    public void run() {

        Dataset dataset = imageDisplayService.getActiveDataset(display);

        long[] position = new long[dataset.numDimensions()];

        display.localize(position);

        Dataset isolatedPlane = imagePlaneService.isolatePlane(dataset, position);

        perform(isolatedPlane);

    }

    private <T extends RealType<T>> void perform(Dataset dataset) {

        overlays = transform(context, (RandomAccessibleInterval<T>) dataset, USE_WHITE_PIXEL.equals(objectColor));

        if (overlays != null && display != null) {
            logger.info(String.format("%d overlays added to %s", overlays.length, display.toString()));
        } else {
            logger.info("No overlay added");
        }

    }

    public static Overlay[] transform(Context context, Dataset dataset, boolean recognizeWhiteObject) {
        return transform(context, (RandomAccessibleInterval) dataset, recognizeWhiteObject);
    }

    public static <T extends RealType<T>> Overlay[] transform(Context context, RandomAccessibleInterval<T> dataset, boolean recognizeWhiteObject) {
        // converting 

        int width = (int) dataset.max(0);
        int height = (int) dataset.max(1);

        RandomAccess<T> randomAccess = dataset.randomAccess();

        ByteProcessor byteProcessor = new ByteProcessor(width, height);

        long[] position = new long[dataset.numDimensions()];

        for (int x = 0; x != width; x++) {
            for (int y = 0; y != height; y++) {

                position[0] = x;
                position[1] = y;

                randomAccess.setPosition(position);
                double p = randomAccess.get().getRealDouble();
                byteProcessor.set(x, y, (p > 0.0 ? 255 : 0));

            }
        }

        ImagePlus imp = new ImagePlus("Temporary mask", byteProcessor);

        ManyBlobs blobs = new ManyBlobs(imp);

        // inverting if necessary because the
        // CC algorithm recognise black objects
        if (recognizeWhiteObject) {
            imp.getProcessor().invert();
        }
        //launching the search
        blobs.findConnectedComponents();

        // creating a list
        List<Overlay> listOverlays = new ArrayList<>(blobs.size());

        int count = 1;

        // converting each Blob into a PolygonOverlay
        for (Blob b : blobs) {

            PolygonOverlay po = new PolygonOverlay(context);
            po.setName(String.format("%d", count++));

            // we modifiy the PolygonOverlay by modifying it region of interest
            PolygonRegionOfInterest roi = po.getRegionOfInterest();

            Polygon polygon = b.getOuterContour();

            if (polygon.getBounds().getWidth() <= 2 && polygon.getBounds().getHeight() <= 2) {

                continue;
            }

            int pointCount = polygon.npoints;
            //System.out.printf("Point count = %d\n",pointCount);
            //System.out.println(polygon);
            for (int i = 0; i != pointCount; i++) {
                RealLocalizable localizable = new RealPoint(polygon.xpoints[i], polygon.ypoints[i]);

                //System.out.println(String.format("%.0f,%.0f",localizable.getDoublePosition(0),localizable.getDoublePosition(1)));
                roi.addVertex(i, localizable);

            }
            po.setLineWidth(2.0);
            listOverlays.add(po);

        }

        return listOverlays.toArray(new Overlay[listOverlays.size()]);
    }

}
