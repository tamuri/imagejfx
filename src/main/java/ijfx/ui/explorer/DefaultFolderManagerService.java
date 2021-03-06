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

import ijfx.core.imagedb.MetaDataExtractionService;
import ijfx.core.metadata.MetaData;
import ijfx.core.metadata.MetaDataSetType;
import ijfx.core.stats.DefaultIjfxStatisticService;
import ijfx.service.ui.JsonPreferenceService;
import ijfx.service.ui.LoadingScreenService;
import ijfx.service.uicontext.UiContextService;
import ijfx.ui.explorer.event.FolderAddedEvent;
import ijfx.ui.explorer.event.FolderDeletedEvent;
import ijfx.ui.explorer.event.FolderUpdatedEvent;
import ijfx.ui.main.ImageJFX;
import ijfx.service.notification.DefaultNotification;
import ijfx.service.notification.NotificationService;
import ijfx.ui.activity.ActivityService;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import mongis.utils.AsyncCallable;
import mongis.utils.CallbackTask;
import mongis.utils.ProgressHandler;
import mongis.utils.SilentProgressHandler;
import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.scijava.Context;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 *
 * @author Cyril MONGIS, 2016
 */
@Plugin(type = Service.class)
public class DefaultFolderManagerService extends AbstractService implements FolderManagerService {

    List<Folder> folderList;

    Folder currentFolder;

    @Parameter
    ExplorerService explorerService;

    @Parameter
    EventService eventService;

    @Parameter
    Context context;

    @Parameter
    UiContextService uiContextService;

    @Parameter
    JsonPreferenceService jsonPrefService;

    @Parameter
    MetaDataExtractionService metaDataExtractionService;

    @Parameter
    LoadingScreenService loadingScreenService;

    @Parameter
    DefaultIjfxStatisticService statsService;

    @Parameter
    NotificationService notificationService;

    @Parameter
    ImageDisplayService imageDisplayService;

    @Parameter
    ActivityService activityService;

    Logger logger = ImageJFX.getLogger();

    ExplorationMode currentExplorationMode;

    List<Explorable> currentItems;

    ExecutorService executorService = Executors.newFixedThreadPool(1);

    private Folder recentFileFolder;

    @Override
    public Folder addFolder(File file) {
        Folder f = new DefaultFolder(file);
        context.inject(f);
        return addFolder(f);
    }

    protected Folder addFolder(Folder f) {
        getFolderList().add(f);

        if (folderList.size() == 1) {
            setCurrentFolder(f);
        }

        eventService.publish(new FolderAddedEvent().setObject(f));
        save();
        return f;
    }

    @Override
    public List<Folder> getFolderList() {

        if (folderList == null) {
            folderList = new ArrayList<>();
            load();
        }

        return folderList;
    }

    @Override
    public Folder getCurrentFolder() {
        return currentFolder;
    }

    @Override
    public void setCurrentFolder(Folder folder) {
        currentFolder = folder;

        if (currentFolder == null) {

            setItems(new ArrayList<>());
            updateExploredElements();
        } else {

            logger.info("Setting current folder " + folder.getName());

            explorerService.setItems(currentFolder.getFileList());

            uiContextService.enter("explore-files");
            uiContextService.update();
            updateExploredElements();
        }
    }

    @EventHandler
    public void onFolderUpdated(FolderUpdatedEvent event) {
        logger.info("Folder updated ! " + event.getObject().getName());
        if (currentFolder == event.getObject()) {
            updateExploredElements();
        }
    }

    @Override
    public void setExplorationMode(ExplorationMode mode) {
        if (mode == currentExplorationMode) {
            return;
        }
        currentExplorationMode = mode;
        eventService.publish(new ExplorationModeChangeEvent().setObject(mode));
        updateExploredElements();
    }

    private void updateExploredElements() {

        ExplorationMode mode = currentExplorationMode;
        logger.info("Updating current elements");
        AsyncCallable<List<Explorable>> task = new AsyncCallable<>();
        task.setTitle("Fetching elements...");

        if (currentFolder == null) {
            return;
        } else {

            if (mode != null) {
                switch (mode) {
                    case FILE:
                        task.run(currentFolder::getFileList);
                        break;
                    case PLANE:
                        task.run(currentFolder::getPlaneList);
                        break;
                    case OBJECT:
                        task.run(currentFolder::getObjectList);

                }
            }
            task.then(this::setItems);
            task.start();
            loadingScreenService.frontEndTask(task, false);
        }
        if (mode != null) {
            logger.info("Exploration mode changed : " + mode.toString());
        }
    }

    private void setItems(List<Explorable> items) {

        if (items != null) {
            logger.info(String.format("Setting %d items", items.size()));
        } else {
            logger.info("Items are null");
        }
        explorerService.setItems(items);
    }

    private Integer fetchMoreStatistics(ProgressHandler progress, List<Explorable> explorableList) {
        if (progress == null) {
            progress = new SilentProgressHandler();
        }
        progress.setStatus("Completing statistics of the objects");
        Integer elementAnalyzedCount = 0;
        int elements = explorableList.size();
        int i = 0;
        for (Explorable e : explorableList) {
            if (!e.getMetaDataSet().containsKey(MetaData.STATS_PIXEL_MIN)) {
                progress.setProgress(i, elements);
                MetaDataSetType type = e.getMetaDataSet().getType();
                if (type == MetaDataSetType.FILE || type == MetaDataSetType.PLANE) {

                    SummaryStatistics stats = statsService.getSummaryStatistics(e.getDataset());

                    e.getMetaDataSet().merge(statsService.summaryStatisticsToMap(stats));
                    elementAnalyzedCount++;
                }
            }
            i++;
        }
        return elementAnalyzedCount;
    }

    @Override
    public ExplorationMode getCurrentExplorationMode() {
        return currentExplorationMode;
    }

    private void save() {
        HashMap<String, String> folderMap = new HashMap<>();
        for (Folder f : getFolderList()) {
            folderMap.put(f.getName(), f.getDirectory().getAbsolutePath());
        }
        jsonPrefService.savePreference(folderMap, FOLDER_PREFERENCE_FILE);
    }

    private synchronized void load() {

        //recentFileFolder = new RecentFileFolder(getContext());
        //folderList.add(recentFileFolder);
        Map<String, String> folderMap = jsonPrefService.loadMapFromJson(FOLDER_PREFERENCE_FILE, String.class, String.class);
        folderMap.forEach((name, folderPath) -> {
            File file = new File(folderPath);
            if (file.exists() == false) {
                return;
            }
            DefaultFolder folder = new DefaultFolder(file);
            context.inject(folder);
            folder.setName(name);
            folderList.add(folder);
        });
    }

    @Override
    public void completeStatistics() {
        loadingScreenService.frontEndTask(new CallbackTask<List<Explorable>, Integer>()
                .run(this::fetchMoreStatistics)
                .setInput(getCurrentFolder().getFileList())
                .then(this::onStatisticComputingEnded)
                .start()
        );

    }

    private void onStatisticComputingEnded(Integer computedImages) {
        notificationService.publish(new DefaultNotification("Statistic Computation", String.format("%d images where computed.", computedImages)));
        eventService.publish(new FolderUpdatedEvent().setObject(getCurrentFolder()));
    }

    public void removeFolder(Folder folder) {
        folderList.remove(folder);
        eventService.publish(new FolderDeletedEvent().setObject(folder));
    }

    @Override
    public Folder getFolderContainingFile(File f) {
        return getFolderList()
                .stream()
                .filter(folder -> folder != null && folder.isFilePartOf(f))
                .findFirst()
                .orElse(null);
    }

    public void openImageFolder(String imagePath) {
        File sourceFile = new File(imagePath);

        if (sourceFile.exists()) {

            Folder folderContainingFile = getFolderContainingFile(new File(imagePath));

            if (folderContainingFile == null) {
                folderContainingFile = addFolder(new File(imagePath).getParentFile());
            }

            setCurrentFolder(folderContainingFile);
            setExplorationMode(ExplorationMode.FILE);
        }
        activityService.openByType(ExplorerActivity.class);
    }

    public void openImageFolder(ImageDisplay display) {

        Dataset activeDataset = imageDisplayService.getActiveDataset(display);

        if (activeDataset != null) {

            String source = activeDataset.getSource();
            if (source == null) {
                source = (String) activeDataset.getProperties().get(MetaData.ABSOLUTE_PATH);
            }

            openImageFolder(source);

        }

    }

}
