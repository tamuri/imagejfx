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
package ijfx.ui.project_manager.projectdisplay.card;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import ijfx.core.project.Project;
import javafx.beans.property.Property;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Label;
import mongis.utils.FakeTask;

/**
 *
 * @author cyril
 */
public class DummyCard extends Label implements ProjectCard {

   
    
    public DummyCard() {
        setText("This set of cards give you quick information about your database. Some cards also analyse your database and help you getting the best out of it.");
    }
    
    @Override
    public Node getContent() {
        return this;
    }

    @Override
    public Task<Boolean> update(Project project) {
        return new FakeTask<>(10);
    }

    @Override
    public String getName() {
        return "Welcome";
    }

    @Override
    public FontAwesomeIcon getIcon() {
        return FontAwesomeIcon.SMILE_ALT;
    }

    
     DismissableCardDecorator<Project> decorator = new DismissableCardDecorator<>(this);
    
    @Override
    public Property<Boolean> dismissable() {
        return decorator.dismissable();
        
    }

    @Override
    public Property<Boolean> dismissed() {
        return decorator.dismissed();
    }
    
}