/*
 * /*
 *     This file is part of ImageJ FX.
 *
 *     ImageJ FX is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ImageJ FX is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
 *
 * 	Copyright 2015,2016 Cyril MONGIS, Michael Knop
 *
 */
package ijfx.plugins.adapter;

import ij.ImagePlus;
import static java.lang.Math.toIntExact;
import java.util.stream.IntStream;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;

import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;

/**
 *
 * @author Cyril MONGIS, 2015
 * @author Tuan anh TRINH
 */
public abstract class AbstractImageJ1PluginAdapter implements Command {

    @Parameter
    public DatasetService datasetService;

    @Parameter
    ImageDisplayService imageDisplayService;

    @Parameter
    EventService eventService;

    @Parameter
    boolean createCopy;

    boolean wholeWrap = false;

    public abstract ImagePlus processImagePlus(ImagePlus input);

    public ImagePlus getInput(Dataset dataset) {
        return unwrapDataset(dataset);
    }

    public Dataset setOutput(ImagePlus imp, Dataset dataset) {
        if (!wholeWrap) {
            dataset = wrapDataset(imp);
        } else {
            ImagePlus resultCopy = imp.duplicate();
            if (createCopy) {
                dataset = emptyDataset(dataset, dataset.numDimensions());
            }
            Dataset dataset2 = wrapDataset(resultCopy);
            for (int i = 0; i < getNumberOfSlices(dataset); i++) {
                dataset.setPlane(i, dataset2.getPlane(i));
            }

        }
        return dataset;
    }

    public static ImagePlus unwrapDataset(Dataset dataset) {
        RandomAccessibleInterval<UnsignedShortType> r = (RandomAccessibleInterval<UnsignedShortType>) dataset.getImgPlus();
        ImagePlus wrapImage = ImageJFunctions.wrap(r, "");
        return wrapImage;
    }

    public Dataset wrapDataset(ImagePlus imp) {
        Img img = ImageJFunctions.wrap(imp.duplicate());
        return datasetService.create(img);
    }

    public static void configureImagePlus(ImagePlus imp, ImageDisplay imageDisplay) {

        imp.setC(imageDisplay.getIntPosition(Axes.CHANNEL));
        imp.setZ(imageDisplay.getIntPosition(Axes.Z));
        imp.setT(imageDisplay.getIntPosition(Axes.TIME));

    }

    private Dataset emptyDataset(Dataset input, int sizeDims) {
        AxisType[] axisType = new AxisType[input.numDimensions()];
        CalibratedAxis[] axeArray = new CalibratedAxis[input.numDimensions()];
        input.axes(axeArray);

        long[] dims = new long[sizeDims];
        for (int i = 0; i < sizeDims; i++) {
            axisType[i] = axeArray[i].type();
            dims[i] = toIntExact(input.max(i) + 1);
        }
        return datasetService.create(dims, input.getName(), axisType, input.getValidBits(), input.isSigned(), false);
    }

    private Dataset chooseDataset(Dataset dataset) {
        if (createCopy) {
            return dataset.duplicateBlank();
        }
        return dataset;
    }

    public int getNumberOfSlices(Dataset dataset) {

        return (int) (dataset.getImgPlus().size() / (dataset.dimension(0) * dataset.dimension(1)));

    }

    public Dataset processDataset(Dataset dataset) {
        Dataset datasetToModify = chooseDataset(dataset);

        for (int i = 0; i < getNumberOfSlices(dataset); i++) {
            Dataset datasetOnePlane = emptyDataset(dataset, 2);
            datasetOnePlane.setPlane(0, dataset.getPlane(i));
            ImagePlus result = processImagePlus(getInput(datasetOnePlane));
            setOutput(result.duplicate(), datasetOnePlane);
            datasetToModify.setPlane(i, datasetOnePlane.getPlane(0));
        }
        dataset = datasetToModify;
        return dataset;
    }
//
//    /**
//     * Wrap the whole Dataset. Use more memory
//     *
//     * @param dataset
//     * @return
//     */
//    public Dataset processDatasetWholeWrap(Dataset dataset) {
//        ImagePlus result = processImagePlus(getInput(dataset));
//        ImagePlus resultCopy = result.duplicate();
//        if (createCopy) {
//            dataset = emptyDataset(dataset, dataset.numDimensions());
//        }
//        Dataset dataset2 = wrapDataset(resultCopy);
//        for (int i = 0; i < getNumberOfSlices(dataset); i++) {
//            dataset.setPlane(i, dataset2.getPlane(i));
//        }
//        return dataset;
//    }

    public boolean isWholeWrap() {
        return wholeWrap;
    }

    public void setWholeWrap(boolean wholeWrap) {
        this.wholeWrap = wholeWrap;
    }
}