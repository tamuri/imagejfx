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
package ijfx.core.assets.loader;

import ijfx.core.Handles;
import ijfx.core.assets.Asset;
import ijfx.core.assets.AssetLoader;
import ijfx.core.assets.AssetService;
import ijfx.core.assets.DatasetAsset;
import ijfx.core.assets.FlatfieldAsset;
import ijfx.plugins.flatfield.DarkfieldSubstraction;
import ijfx.plugins.flatfield.GenerateFlatfield;
import ijfx.plugins.flatfield.GenerateMultiChannelFlatfield;
import ijfx.service.ui.CommandRunner;
import io.scif.services.DatasetIOService;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.imagej.Dataset;
import net.imagej.types.DataTypeService;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Cyril MONGIS, 2016
 */
@Plugin(type = AssetLoader.class)
@Handles(type = FlatfieldAsset.class)
public class FlatfieldAssetLoader implements AssetLoader<Dataset> {

    @Parameter
    DatasetIOService datasetIoService;

    @Parameter
    Context context;

    @Parameter
    DataTypeService dataTypeService;

    @Parameter
    ModuleService moduleService;

    @Parameter
    CommandService commandService;

    @Override
    public Dataset load(Asset<Dataset> asset) {
        try {

            Dataset dataset = datasetIoService.open(asset.getFile().getAbsolutePath());
            
            // if the asset is a flatfield asset
            if (asset instanceof FlatfieldAsset) {
                FlatfieldAsset flAsset = (FlatfieldAsset) asset;
                
                // if the asset also comes with a darkfield,
                // the flatfield overgoes darkfield substraction
                if (flAsset.getDarkfield() != null) {
                    new CommandRunner(context)
                            .set("dataset", dataset)
                            .set("file", flAsset.getDarkfield())
                            .set("multichannel", flAsset.isMultiChannel())
                            .runSync(DarkfieldSubstraction.class);

                }
                
                // if the asset is multichannel
                // a multichannel flatfield image is generated
                if (flAsset.isMultiChannel()) {
                    return new CommandRunner(context)
                            .set("input", dataset)
                            .runSync(GenerateMultiChannelFlatfield.class)
                            .getOutput("output");
                }

            }

            // happends is it wasn't a flatfield asset or if the flatfield asset
            // is not multichannel
            return new CommandRunner(context)
                    .set("dataset", dataset)
                    .runSync(GenerateFlatfield.class)
                    .getOutput("dataset");

            //return dataset;
        } catch (IOException ex) {
            Logger.getLogger(FlatfieldAssetLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
