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
package ijfx.ui.explorer;

import ijfx.ui.explorer.event.FolderAddedEvent;
import ijfx.ui.explorer.event.FolderUpdatedEvent;
import ijfx.ui.main.ImageJFX;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import mongis.utils.AsyncCallback;
import org.scijava.Context;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 *
 * @author cyril
 */
@Plugin(type = Service.class)
public class DefaultFolderManagerService extends AbstractService implements FolderManagerService {

    List<Folder> folderList = new ArrayList<>();

    Folder currentFolder;

    @Parameter
    ExplorerService explorerService;

    @Parameter
    EventService eventService;
    
    @Parameter
    Context context;
    
    
    Logger logger = ImageJFX.getLogger();
    
    @Override
    public Folder addFolder(File file) {
        Folder f = new DefaultFolder(file);
        
        context.inject(f);
        
        folderList.add(f);
        
        if(folderList.size() == 1) {
            setCurrentFolder(f);
        }
        
        eventService.publish(new FolderAddedEvent().setObject(f));
        
        
        
        return f;
    }

    @Override
    public List<Folder> getFolderList() {
        return folderList;
    }

    @Override
    public Folder getCurrentFolder() {
        return currentFolder;
    }

    @Override
    public void setCurrentFolder(Folder folder) {
        currentFolder = folder;
        
        logger.info("Setting current folder " + folder.getName());
        
        explorerService.setItems(currentFolder.getItemList());

    }
    
    @EventHandler
    public void onFolderUpdated(FolderUpdatedEvent event) {
        logger.info("Folder updated ! "+event.getObject().getName());
        if(currentFolder == event.getObject()) {
            explorerService.setItems(event.getObject().getItemList());
        }
    }

}