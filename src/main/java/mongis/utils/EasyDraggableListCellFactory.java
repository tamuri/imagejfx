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
package mongis.utils;

import ijfx.ui.main.ImageJFX;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.Observable;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import mongis.utils.EasyCellFactory;
import mongis.utils.ListCellController;

/**
 *
 * @author Cyril MONGIS, 2015
 */
public abstract class EasyDraggableListCellFactory<T, C extends Node & ListCellController<T>> implements Callback<ListView<T>, ListCell<T>> {

    Class<C> clazz;
    public EasyDraggableListCellFactory(Class<C> clazz) {
        this.clazz = clazz;
    }

    
    public abstract void afterCreation(C ctrl);
    

    @Override
    public ListCell<T> call(ListView<T> param) {
        return new DraggableListCell<T>() {
            C ctrl;

            public C getCtrl() {
                if (ctrl == null) {
                    try {
                        ctrl = clazz.getDeclaredConstructor().newInstance();
                        afterCreation(ctrl);
                    }  catch (Exception ex) {
                       ImageJFX.getLogger().log(Level.SEVERE, null, ex);
                    }
                }
                return ctrl;
            }

           
           

            @Override
            protected void onItemChanged(Observable obs, T oldValue, T newValue) {
                
                if(newValue == null) setGraphic(null);
                else {
                    getCtrl().setItem(newValue);
                    setGraphic(getCtrl());
                }
                
                
            }
        };
    }

}