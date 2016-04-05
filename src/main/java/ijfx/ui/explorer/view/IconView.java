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
package ijfx.ui.explorer.view;

import ijfx.ui.explorer.Explorable;
import ijfx.ui.explorer.ExplorerIconCell;
import ijfx.ui.explorer.ExplorerView;
import ijfx.ui.explorer.Iconazable;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;
import mongis.utils.panecell.PaneCell;
import mongis.utils.panecell.PaneCellController;
import mongis.utils.panecell.ScrollBinder;

/**
 *
 * @author cyril
 */
public class IconView extends ScrollPane implements ExplorerView {

    private final TilePane tilePane = new TilePane();

    private ScrollBinder binder;

    private final PaneCellController<Iconazable> cellPaneCtrl = new PaneCellController<>(tilePane);

    public IconView() {
        setContent(tilePane);
        setPrefWidth(400);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tilePane.prefWidthProperty().bind(widthProperty());
        tilePane.setPrefTileWidth(170);
        tilePane.setPrefTileHeight(270);
        //tilePane.setPrefTileHeight(Control.USE_PREF_SIZE);
        tilePane.setVgap(5);
        tilePane.setHgap(5);
        binder = new ScrollBinder(this);
        cellPaneCtrl.setCellFactory(this::createIcon);
    }

    @Override
    public Node getNode() {

        return this;
    }

    @Override
    public void setItem(List<? extends Explorable> items) {
        cellPaneCtrl.update(new ArrayList<Iconazable>(items));
    }

    private PaneCell<Iconazable> createIcon() {
        return new ExplorerIconCell();
    }

}