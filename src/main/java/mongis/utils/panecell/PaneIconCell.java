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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import ijfx.ui.main.ImageJFX;
import ijfx.ui.main.LoadingIcon;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import mongis.utils.CallbackTask;

import mongis.utils.BindingsUtils;
import mongis.utils.FXUtilities;
import mongis.utils.FailableCallback;

/**
 * The PaneIconCell is a generic class used to display items in form of Icons
 * with a text and subtext under. The PaneIconCell is configured by setting a
 * Model object which contain the data and callback that define how to get the
 * informations from the models. In this way, when the model is changed, the
 * titleIconView update itself automatically using the defined callback;
 *
 * Example, if I want to create a PaneIconCell that display a File.
 *
 * PaneIconCell<File> titleIconView = new PaneIconCell();
 * titleIconView.setTitleCallback(f-> f.getName());
 * titleIconView.setSubtitleCallback(f->f.getLength());
 * titleIconView.setImageCallback(f->f.getName().endsWith(".jpg") ? new
 * Image(f.getAbsoluthPath()) : new Image("/path/to/file.jpg");
 *
 * The last callback will return an Image
 *
 * @author Cyril MONGIS
 * @param <T> Contained model
 */
public class PaneIconCell<T> extends BorderPane implements PaneCell<T> {

    @FXML
    private FontAwesomeIconView titleIconView;
    @FXML
    private ImageView imageView;

    @FXML
    protected Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private VBox titleVBox;

    @FXML
    private BorderPane imageViewContainer;

    private final ObjectProperty<T> item = new SimpleObjectProperty<T>();

    // Callback 
    private FailableCallback<T, String> titleFactory = T -> "No title factory";

    private FailableCallback<T, String> subtitleFactory = T -> "No subtitle factory";

    private FailableCallback<T, String> additionalInfoFactory = T -> "you could benefit from **Awesome display**\n";

    private FailableCallback<T, Image> imageFactory;

    private FailableCallback<T, FontAwesomeIconView> iconFactory;

    private Task currentImageSearch;

    Logger logger = ImageJFX.getLogger();

    private boolean isInsideScrollWindow = false;

    Image currentImage = null;

   // private final static FXMLLoader LOADER = new FXMLLoader(PaneIconCell.class.getResource("/ijfx/ui/explorer/ImageIconItem.fxml"));

  

   

    private final BooleanProperty isSelectedProperty = new SimpleBooleanProperty(false);

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private static final ExecutorService refreshThreadPool = Executors.newFixedThreadPool(3);
    
    private final BooleanProperty onScreenProperty = new SimpleBooleanProperty(false);
    
   LoadingIcon loadingIcon = new LoadingIcon(20);

    public PaneIconCell() {
        try {
            FXUtilities.injectFXML(this, "/ijfx/ui/explorer/ImageIconItem.fxml");
            /*
            synchronized (LOADER) {

                LOADER.setController(this);
                LOADER.setRoot(this);
                LOADER.load();
            }*/

            titleLabel.setPrefHeight(100);
             imageView.setSmooth(false);
             
            //imageView.fitWidthProperty().bind(widthProperty());
            //imageView.fitHeightProperty().bind(widthProperty());
            imageView.setFitWidth(120);
            //imageView.fitWidthProperty().bind(imageViewContainer.widthProperty());
            imageView.setPreserveRatio(true);
            //imageView.fitHeightProperty().bind(imageViewContainer.widthProperty());
           
           
            item.addListener(this::onItemChanged);

            addEventHandler(ScrollWindowEvent.SCROLL_WINDOW_ENTERED, this::onScrollWindow);

            addEventHandler(ScrollWindowEvent.SCROLL_WINDOW_EXITED, this::onScrollWindow);

            addEventHandler(MouseEvent.MOUSE_PRESSED, this::onClick);
            BindingsUtils.bindNodeToPseudoClass(SELECTED, this, selectedProperty());

            getStyleClass().add("pane-icon-cell");

            subtibleVisibleProperty().addListener(this::onSubtitleVisibleChanged);
            
            onScreenProperty().addListener(this::onAppearingOnScreen);
            

        } catch (IOException ex) {
            Logger.getLogger(PaneIconCell.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setIcon(FontAwesomeIconView icon) {
        setCenter(this.titleIconView);
        this.titleIconView = icon;
    }

    public void onScrollWindow(ScrollWindowEvent event) {
        onScreenProperty().setValue(event.getEventType() == ScrollWindowEvent.SCROLL_WINDOW_ENTERED);
    }

    /**
     * Set the Model item contained by the PaneIconCell. The PaneCell will call
     * the different callback to update the view.
     *
     * @param item
     */
    @Override
    public void setItem(T item) {
        this.item.setValue(item);
    }

    /**
     *
     * @return the item displayed the cell
     */
    @Override
    public T getItem() {
        return this.item.getValue();
    }

    /**
     *
     * @return the Node representing the PaneCell
     */
    @Override
    public Node getContent() {
        return this;
    }

    public Property<T> itemProperty() {
        return item;
    }

    /**
     * Set directly the title of the Icon. The title may be overridden later by
     * the title callback
     *
     * @param title
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    public void setAdditionalData(String text) {

    }

    public void setImage(Image image) {
        logger.info("Setting image : "+image);
        loadingIcon.stop();
        
        if(image == null) {
            setCenter(new FontAwesomeIconView(FontAwesomeIcon.ANGELLIST));
            return;
        };
        
        imageView.setSmooth(false);
       
        
        setCenter(imageView);
        imageView.setImage(image);

    }

    public void onItemChanged(Observable obs, T oldItem, T newItem) {

        // cancelling the possible image search
        if (currentImageSearch != null) {
            logger.info("Cancelling image change");
            currentImageSearch.cancel();
        }

        // setting the current image to null
        currentImage = null;
        currentImageSearch = null;

        // avoiding other task to start
        if (newItem == null) {
            return;
        }
        
        forceUpdate(newItem);

    }

    protected void forceUpdate(T newItem) {
        //otherwise starting to charge everything
        new CallbackTask<T, String>()
                .setInput(newItem)
                .run(titleFactory)
                .then(this::setTitle)
                .start();

        new CallbackTask<T, String>()
                .setInput(newItem)
                .run(subtitleFactory)
                .then(this::setSubtitle)
                .start();

        
        new CallbackTask<T, FontAwesomeIconView>()
                .setInput(newItem)
                .run(iconFactory)
                .then(this::setIcon)
                .start();

        /*
        
         */
        new CallbackTask<T, String>()
                .setInput(newItem)
                .run(additionalInfoFactory)
                .then(this::setAdditionalData)
                .start();

        if(currentImageSearch != null) {
            currentImageSearch.cancel();
            currentImage = null;
        }
        
        if(onScreenProperty.getValue() == true) {
            updateImageAsync(newItem);
        }

    }

    protected void updateImageAsync(T newItem) {

        if (newItem == null) {
            return;
        }

        setCenter(loadingIcon);
        loadingIcon.play();

        currentImageSearch = new CallbackTask<T, Image>()
                .setInput(newItem)
                .run(imageFactory)
                .then(this::setImage)
                .startIn(refreshThreadPool);
    }

    public PaneIconCell<T> setAdditionalInfoFactory(FailableCallback<T, String> additionalInfoFactory) {
        this.additionalInfoFactory = additionalInfoFactory;
        return this;
    }

    /**
     *
     * @param titleFactory Callback that takes a model item as input and return
     * a string representing the title of the titleIconView
     * @return the PaneIconCell for convenient reasons
     */
    public PaneIconCell<T> setTitleFactory(FailableCallback<T, String> titleFactory) {
        this.titleFactory = titleFactory;
        return this;
    }

    /**
     *
     * @param subtitleFactory Callback that takes a model item as input and
     * return a string representing the title of the titleIconView
     * @return the PaneIconcell for convenient reasons
     */
    public PaneIconCell<T> setSubtitleFactory(FailableCallback<T, String> subtitleFactory) {
        this.subtitleFactory = subtitleFactory;
        return this;
    }

    /**
     *
     * @param imageFactory Callback that takes a model item as input and return
     * an Image as titleIconView for the model. The callback is always executed
     * in a separated thread in order to avoid blocking the display of the
     * titleIconView.
     * @return the PaneIconCell for convenient reasons
     */
    public PaneIconCell<T> setImageFactory(FailableCallback<T, Image> imageFactory) {
        this.imageFactory = imageFactory;
        return this;
    }

    public PaneIconCell<T> setIconFactory(FailableCallback<T, FontAwesomeIconView> iconFactory) {
        this.iconFactory = iconFactory;
        return this;
    }

   

    public BooleanProperty subtibleVisibleProperty() {
        return subtitleLabel.visibleProperty();
    }

    public void setSubtitleVisible(boolean subtitleVisible) {
        subtibleVisibleProperty().setValue(subtitleVisible);

    }

    public boolean isSubtitleVisible() {
        return subtibleVisibleProperty().getValue();
    }
    
    public BooleanProperty onScreenProperty() {
        return onScreenProperty;
    }

    @Override
    public BooleanProperty selectedProperty() {
        return isSelectedProperty;
    }

    public BooleanProperty showIconProperty() {
        return titleIconView.visibleProperty();
    }
    
    private void onClick(MouseEvent event) {
        if(event.getClickCount() == 2) {
            onDoubleClick();
        }
       
            onSimpleClick();
        
    }

    protected void onSimpleClick() {
        selectedProperty().setValue(!selectedProperty().getValue());
    }

    protected void onDoubleClick() {
        
    }

    protected void onSubtitleVisibleChanged(Observable obs, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            titleVBox.getChildren().add(subtitleLabel);
        } else {
            titleVBox.getChildren().remove(subtitleLabel);
        }
    }
    
    protected void onAppearingOnScreen(Observable obs, Boolean oldValue, Boolean newValue) {
        if(newValue) {
            updateImageAsync(getItem());
        }
    }



}
