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
package mongis.utils.panecell;

import ijfx.ui.explorer.Explorable;
import ijfx.ui.main.ImageJFX;
import ijfx.ui.main.LoadingPopup;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import mercury.core.MercuryTimer;
import mongis.utils.CallbackTask;
import mongis.utils.properties.ServiceProperty;

/**
 * The PaneController takes care of filling a Pane with PaneCells. Like a
 * ListView, PaneCells are just container that update themselves when changes of
 * the model occurs. The PaneController cache used PaneCell and create new when
 * necessary. When the list of items change, the PaneCellController update the
 * children nodes of the associated Pane.
 *
 * @author Cyril MONGIS
 */
public class PaneCellController<T extends Object> {

    private List<T> currentItems;
    private LinkedList<PaneCell<T>> itemControllerList = new LinkedList<PaneCell<T>>();
    private LinkedList<PaneCell<T>> cachedControllerList = new LinkedList<>();

    private Callable<PaneCell<? extends T>> cellFactory;

    private Logger logger = ImageJFX.getLogger();

    private Pane pane;

    private ObservableSet<T> selectedItems = FXCollections.observableSet();

    public PaneCellController(Pane pane) {
        setPane(pane);
    }

    /**
     * Set the pane that should be updated by the controller
     *
     * @param pane
     */
    public void setPane(Pane pane) {
        this.pane = pane;
    }

    public void setCellFactory(Callable<PaneCell<? extends T>> cellFactory) {
        this.cellFactory = cellFactory;
    }

    PaneCellUpdateProcess<T> updateProcess;

    /**
     * Give it a list of items coming from the model and the controller will
     * update the pane. If necessary, new PanelCell will be created. Unnecessary
     * PaneCells will be cached.
     *
     * @param items List of items coming from the model
     */
    public synchronized CallbackTask update(List<T> items) {

        /*
        return new CallbackTask<Integer, List<PaneCell<T>>>()
                .setInput(items.size())
                .setName("Loading...")
                .run(this::retrieve)
                .then(controllers -> {
                    MercuryTimer timer = new MercuryTimer("Browser view");
                    timer.start();
                    pane.getChildren().clear();
                    pane.getChildren().addAll(getContent(controllers));
                    timer.elapsed("Adding all the controllers");
                    for (int i = 0; i != items.size(); i++) {
                      controllers.get(i).setItem(items.get(i));
                    }
                    timer.elapsed("Updating all the controllers");

                })
                .start();
                
                * 
         */
        if (updateProcess != null) {
            updateProcess.cancel();
        }

        updateProcess = new PaneCellUpdateProcess(items, cachedControllerList, pane.getChildren(), cellFactory);

        return new CallbackTask<Void, Void>().start();

    }

    public synchronized void update3DList(List<List<List<T>>> items, int size, List<String> metaDatas) {
        new CallbackTask<Integer, List<PaneCell<T>>>()
                .setInput(size)
                .run(this::retrieve)
                .then(controllers -> {
                    MercuryTimer timer = new MercuryTimer("Browser view");
                    timer.start();
                    pane.getChildren().clear();
                    int cpt = 0;
                    for (int x = 0; x < items.size(); x++) {
                        GridPane gridPane = new GridPane();
                        for (int i = 0; i < items.get(x).size(); i++) {
                            for (int j = 0; j < items.get(x).get(i).size(); j++) {
                                gridPane.add(controllers.get(cpt).getContent(), j, i);
                                controllers.get(cpt).setItem(items.get(x).get(i).get(j));
                                String informations = "";
                                for (String metaData : metaDatas) {
                                    informations = informations + metaData + " = " + ((Explorable) items.get(x).get(i).get(j)).getMetaDataSet().get(metaData).getStringValue() + "\n";
                                }
                                Tooltip tooltip = new Tooltip(informations);
                                Tooltip.install(controllers.get(cpt).getContent(), tooltip);
                                cpt++;

                            }
                        }
                        gridPane.getStyleClass().add("grid-pane-view");

                        pane.getChildren().add(gridPane);

                    }
                    timer.elapsed("Updating3D all the controllers");
                })
                .start();

    }

    private List<PaneCell<T>> retrieve(Integer number) {

        MercuryTimer timer = new MercuryTimer("Browser view");
        int cacheSize = cachedControllerList.size();
        int missingControllers = number - cacheSize;
        timer.start();
        if (missingControllers > 0) {
            fillCache(missingControllers);
        }
        timer.elapsed("Time to fetch controllers");
        return cachedControllerList.subList(0, number);
    }

    // get the list of cells
    protected Collection<Node> getContent(Collection<PaneCell<T>> cellList) {
        return cellList.stream().map(PaneCell::getContent).collect(Collectors.toList());
    }

    // fills the cache by creating a certain number of cells
    private void fillCache(int number) {

        cachedControllerList.addAll(IntStream.range(0, number + 1).parallel().mapToObj(n -> createPaneCell()).collect(Collectors.toList()));

    }

    PseudoClass SELECTED_PSEUDO_CLASS = new PseudoClass() {
        private final static String SELECTED = "selected";

        @Override
        public String getPseudoClassName() {
            return SELECTED;
        }
    };

    // creates a pane cell
    private PaneCell<T> createPaneCell() {
        try {

            PaneCell cell = cellFactory.call();

            return cell;

        } catch (Exception ex) {
            Logger.getLogger(PaneCellController.class.getName()).log(Level.SEVERE, "Error when creating cell", ex);
        }
        return null;
    }

    public Boolean isSelected(T item) {
        return selectedItems.contains(item);
    }

    public void setSelected(T item, Boolean selection) {
        if (selection) {
            selectedItems.add(item);
        } else {
            selectedItems.remove(item);
        }
        Platform.runLater(this::updateSelection);
    }

    public void select(List<T> items) {
        selectedItems.addAll(items);
        Platform.runLater(this::updateSelection);
    }

    public void unselected(List<T> items) {
        Platform.runLater(this::updateSelection);
    }

    public Property<Boolean> getSelectedProperty(T item) {
        return new ServiceProperty<>(item, this::setSelected, this::isSelected);
    }

    public void updateSelection() {
        itemControllerList.forEach(this::updateSelection);
    }

    public void updateSelection(PaneCell<T> cell) {
        cell.getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected(cell.getItem()));
    }

    public List<T> getItems() {
        return currentItems;
    }

    public List<PaneCell> getCells() {
        return pane
                .getChildren()
                .stream()
                .map(child -> (PaneCell) child)
                .collect(Collectors.toList());
    }
}
