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
package ijfx.plugins;

import static java.lang.Math.toIntExact;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mongis.ndarray.NDimensionalArray;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Tuan anh TRINH
 */
@Plugin(type = Command.class, menuPath = "Plugins>Projection", attrs = {
    @Attr(name = "no-legacy")})
public class Projection implements Command {

    @Parameter
    DatasetService datasetService;

    @Parameter(type = ItemIO.INPUT)
    Dataset dataset;

    @Parameter(type = ItemIO.OUTPUT)
    Dataset datasetOutput;

    @Parameter
    ProjectionMethod projectionMethod;

    @Parameter
    AxisType axisTypeParameter;

    @Override
    public void run() {
        AxisType[] axisType = new AxisType[dataset.numDimensions()];
        CalibratedAxis[] axeArray = new CalibratedAxis[dataset.numDimensions()];
        dataset.axes(axeArray);
        long[] dims = new long[axeArray.length];
        for (int i = 0; i < dims.length; i++) {
            axisType[i] = axeArray[i].type();
            dims[i] = toIntExact(dataset.max(i) + 1);
            if (i == dataset.dimensionIndex(axisTypeParameter)) {
                dims[i] = 1;
            }
        }
        datasetOutput = datasetService.create(dims, dataset.getName(), axisType, dataset.getValidBits(), dataset.isSigned(), false);

        project(dataset, datasetOutput, projectionMethod);
    }

    public < T extends RealType<T>> void project(Dataset input, Dataset output, ProjectionMethod projectionMethod) {
        int indexToModify = input.dimensionIndex(axisTypeParameter);
        Img<?> inputImg = input.getImgPlus().getImg();
        long[] dims = new long[input.numDimensions()];
        input.dimensions(dims);

        RandomAccess<T> randomAccess = (RandomAccess<T>) inputImg.randomAccess();
        RandomAccess<T> randomAccessOutput = (RandomAccess<T>) output.randomAccess();
        long[] position = new long[input.numDimensions()];
        List<T> listToProcess = new ArrayList<>();
        
        //In order to avoid creation of too big array
        long[] dimensionToGenerate = Arrays.copyOfRange(dims, 2, dims.length);
        
        //Set the dimension to 1 in order to reduce the size of the array
        //Cannot set to 0
        dimensionToGenerate[indexToModify-2] = 1;
        NDimensionalArray nDimensionalArray = new NDimensionalArray(dimensionToGenerate);
        long[][] possibilities = nDimensionalArray.getPossibilities();

        for (int x = 0; x < dims[input.dimensionIndex(Axes.X)]; x++) {
            for (int y = 0; y < dims[input.dimensionIndex(Axes.Y)]; y++) {
                for (long[] possibilitie : possibilities) {
                    
                    //Go throug th dimension to project
                    for (int m = 0; m < dims[indexToModify]; m++) {
                        position[0] = x;
                        position[1] = y;
                        System.arraycopy(possibilitie, 0, position, 2, possibilitie.length);
                        position[indexToModify] = m;

                        randomAccess.setPosition(position);

                        listToProcess.add(randomAccess.get().copy());
                        if (m == 0) {
                            randomAccessOutput.setPosition(position);
                        } 
                        else if (m == dims[indexToModify] - 1) {
                            projectionMethod.process(listToProcess,randomAccessOutput);
                            listToProcess.removeAll(listToProcess);
                        }
                    }
                }
            }
        }
    }
}