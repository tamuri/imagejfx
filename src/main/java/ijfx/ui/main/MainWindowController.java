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
package ijfx.ui.main;

import ijfx.ui.context.animated.AnimatedPaneContextualView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import ijfx.bridge.ImageJContainer;
import ijfx.ui.notification.Notification;
import ijfx.ui.notification.NotificationEvent;
import ijfx.service.ui.AppService;
import ijfx.service.uiplugin.DefaultUiPluginService;
import ijfx.service.uicontext.UiContextService;
import ijfx.service.log.LogService;
import ijfx.service.ui.FontEndTaskSubmitted;
import ijfx.service.ui.HintService;
import ijfx.service.ui.hint.DefaultHint;
import ijfx.service.ui.hint.Hint;
import ijfx.service.ui.hint.HintRequestEvent;
import ijfx.service.uiplugin.UiPluginReloadedEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;
import net.imagej.ImageJ;
import org.controlsfx.control.Notifications;
import org.scijava.event.EventHandler;
import org.scijava.plugin.Parameter;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import mongis.utils.BindingsUtils;
import ijfx.ui.UiPlugin;
import ijfx.ui.UiConfiguration;
import ijfx.service.uiplugin.UiPluginService;
import ijfx.ui.IjfxCss;
import ijfx.ui.UiContexts;
import ijfx.ui.UiPluginSorter;
import ijfx.ui.activity.Activity;
import ijfx.ui.activity.ActivityChangedEvent;
import ijfx.ui.activity.ActivityService;
import ijfx.ui.context.ContextualWidget;
import ijfx.ui.plugin.DebugEvent;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import mongis.utils.MemoryUtils;
import ijfx.ui.context.animated.Animations;
import ijfx.ui.explorer.ExplorerActivity;
import java.io.IOException;
import java.util.Collection;
import javafx.scene.Scene;
import mongis.utils.AnimationChain;
import mongis.utils.AsyncCallback;
import mongis.utils.FXUtilities;
import mongis.utils.ProgressHandler;

/**
 * FXML Controller class
 *
 * @author Cyril MONGIS, 2015
 */
public class MainWindowController extends AnchorPane {

    public static int ANIMATION_DURATION = 300;

    private static final int MEMORY_REFRESH_RATE = 1000;
    /**
     * Initializes the controller class.
     */

    @FXML
    private HBox topLeftHBox;

    @FXML
    private HBox topCenterHBox;

    @FXML
    private HBox topRightHBox;

    @FXML
    private VBox leftVBox;

    @FXML
    private VBox rightVBox;

    @FXML
    private HBox bottomLeftHBox;

    @FXML
    private HBox bottomRightHBox;

    @FXML
    private HBox bottomCenterHBox;

    @FXML
    private AnchorPane mainAnchorPane;

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private VBox topToolBarVBox;

    //@FXML
    //private StackPane centerStackPane;
    @FXML
    private BorderPane sideMenu;

    @FXML
    private VBox sideMenuMainTopVBox;

    @FXML
    private VBox sideMenuTopVBox;

    @FXML
    private VBox sideMenuBottomVBox;

    @FXML
    private ProgressBar memoryProgressBar;

    @FXML
    private Label memoryLabel;

    //protected final LoadingScreen loadingScreen = LoadingScreen.getInstance();
    protected ImageJ imageJ;

    protected final Logger logger = ImageJFX.getLogger();

    HashMap<String, AnimatedPaneContextualView> uiPluginCtrl = new HashMap<>();

    protected BooleanBinding isSideMenuHidden;

    @Parameter
    UiPluginService uiPluginService;

    MainWindowController thisController;

    @Parameter
    UiContextService uiContextService;

    @Parameter
    AppService appService;

    @Parameter
    LogService logErrorService;

    @Parameter
    HintService hintService;

    @Parameter
    ActivityService activityService;

    LoadingPopup loadingPopup = new LoadingPopup();

    Queue<Hint> hintQueue = new LinkedList<>();

    boolean isHintDisplaying = false;

    private Thread memoryThread = new Thread(() -> {

        while (true) {

            Platform.runLater(() -> updateMemoryUsage());
            try {
                Thread.sleep(MEMORY_REFRESH_RATE);
            } catch (Exception e) {
            }
        }

    });

    public MainWindowController() throws IOException {

        FXUtilities.injectFXML(this, "/ijfx/ui/main/MainWindow.fxml");

        Font.loadFont(FontAwesomeIcon.class.getResource("fontawesome-webfont.ttf").toExternalForm(), 16);

        // binding the sides to the pseudo class empty
        final PseudoClass empty = PseudoClass.getPseudoClass("empty");
        final PseudoClass hidden = PseudoClass.getPseudoClass("hidden");

        BindingsUtils.bindNodeToPseudoClass(empty, leftVBox, new SimpleListProperty<Node>(leftVBox.getChildren()).emptyProperty());
        BindingsUtils.bindNodeToPseudoClass(empty, rightVBox, new SimpleListProperty<Node>(rightVBox.getChildren()).emptyProperty());
        BindingsUtils.bindNodeToPseudoClass(hidden, sideMenu, Bindings.createBooleanBinding(() -> sideMenu.getTranslateX() <= -1.0 * sideMenu.getWidth() + 2, sideMenu.translateXProperty()));

        leftVBox.pseudoClassStateChanged(empty, true);

        Bindings.isEmpty(rightVBox.getChildren()).addListener((obs, oldValue, newValue) -> {
            rightVBox.pseudoClassStateChanged(empty, newValue);
        });

        //loadingScreen.setDefaultPane(mainAnchorPane);
        sideMenu.setTranslateZ(0);

        mainAnchorPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {

            if (event.getButton() == MouseButton.PRIMARY && event.getSceneX() >= sideMenu.getWidth() && sideMenu.getTranslateX() > -1 * sideMenu.getWidth()) {
                hideSideMenu();
            }

        });

        memoryThread.start();

        initMenuAction();

        /*
            The boot sequence is the following
            1. start imagej
            2. inject the context
            3. register contextuals viewx
            4. load plugins
            5. register plugins to the context wideget
            6. finish the start
         */
        mainBorderPane.setOpacity(1.0);
        mainBorderPane.setCenter(new Label("Loading..."));

    }

    private Scene myScene;

    public void setScene(Scene scene) {
        myScene = scene;
    }

    public void init() {

        Task task = new AsyncCallback<Void, Boolean>()
                .runLongCallable(this::init)
                .then(this::finishInitialization)
                .start();

        loadingPopup
                .setCanCancel(false)
                .bindTask(task)
                .closeOnFinished()
                .showOnScene(this.getScene());

        hideSideMenu();

    }

    public Boolean init(ProgressHandler handler) {
        handler.setStatus("Initializing ImageJ....");
        handler.setProgress(1, 3);
        imageJ = new ImageJ();
        handler.setProgress(2, 3);
        handler.setStatus("ImageJ initialized.");
        imageJ.getContext().inject(this);
        registerWidgetControllers();
        handler.setProgress(3, 3);
        uiPluginService.loadAll(handler).forEach(this::installPlugin);
        //finishInitialization(this);
        return true;
    }

    private void installPlugin(UiPlugin uiPlugin) {
        UiConfiguration infos = uiPluginService.getInfos(uiPlugin);

        if (infos == null) {
            logger.warning("No informations for " + uiPlugin.getClass().getName());
            return;
        }

        uiContextService.link(infos.id(), infos.context());
        loadWidget(uiPlugin);
    }

    /*
    protected Boolean bindWidgetToController(Collection<UiPlugin> uiPluginList) {
        logger.info("Getting widget list");
        // uiPluginService.getUiPluginList().forEach(widgetPlugin -> {
        for (UiPlugin uiPlugin : uiPluginList) {
            UiConfiguration infos = uiPluginService.getInfos(uiPlugin);

            if (infos == null) {
                logger.warning("No informations for " + uiPlugin.getClass().getName());
                return false;
            }

            uiContextService.link(infos.id(), infos.context());
            loadWidget(uiPlugin);
        }

        logger.info("Widget laoading done.");
        return true;
    }*/
    protected void finishInitialization(Object o) {

        logger.info("finishing initialization...");
        // entering the right context
        uiContextService.enter(UiContexts.list(UiContexts.DEBUG));

        // showing the intro app
        //appService.showApp(WebApps.PROJECT_WIZARD);
        // updating the context
        uiContextService.update();
        activityService.openByType(ImageJContainer.class);
        // sequence over

        logger.info("Start over");

    }


    /*
    public void initialized(URL url, ResourceBundle rb) {

        thisController = this;

       

        
        Platform.runLater(() -> hideSideMenu());

        // the first stack loads ImageJ
        //final Task task1 = imageJStarter;

        // the third one takes of setting up the interface and the context
        final Task<Boolean> task3 = new Task<Boolean>() {

            @Override
            protected Boolean call() throws Exception {

                //FXUtilities.runAndWait(() -> uiPluginService.getUiPluginList());
                logger.info("Loading widgets done.");

                // bind widget to controller
                startContextManager();

                bindWidgetsToControllers();

                return true;
            }

        };

        // submitting the task to the loading screen
        // when the first task is over
        task1.setOnSucceeded(result -> {
            logger.info("ImageJ Loading done");

            //injecting this controller with image services
            imageJ.getContext().inject(thisController);

            // the second one loads the FXWidgets
            final Task task2 = new AsyncCallback<Void,Collection<UiPlugin>>()
                    .runLongCallable(uiPluginService::loadAll);

            // registering the controllers
            registerWidgetControllers();

            // starting the second task
            ImageJFX.getThreadPool().submit(task2);

            // when the second task is over, starting the third task
            task2.setOnSucceeded(resul -> ImageJFX.getThreadPool().submit(task3));

            //loadingScreen.submitTask(task2, false);
            //loadingScreen.submitTask(task3, false);
            loadingPopup.bindTask(task2);
        });

        task3.setOnSucceeded(result -> {

            // entering the right context
            uiContextService.enter(UiContexts.list(UiContexts.DEBUG));

            // showing the intro app
            //appService.showApp(WebApps.PROJECT_WIZARD);
            // updating the context
            //uiContextService.update();
            activityService.openByType(ImageJContainer.class);
            // sequence over
            logger.info("Start over");

        });

        // starting the first task
        ImageJFX.getThreadPool().submit(task1);


        

    }

    protected void startContextManager() {

        logger.info("Getting widget list");
        // uiPluginService.getUiPluginList().forEach(widgetPlugin -> {
        for (UiPlugin uiPlugin : uiPluginService.getUiPluginList()) {
            UiConfiguration infos = uiPluginService.getInfos(uiPlugin);

            if (infos == null) {
                logger.warning("No informations for " + uiPlugin.getClass().getName());
                return;
            }

            uiContextService.link(infos.id(), infos.context());
        }

        logger.info("Widget laoading done.");

    }*/
    public void registerWidgetControllers() {
        registerPaneCtrl(topLeftHBox)
                .setAnimationOnHide(Animations.DISAPPEARS_LEFT)
                .setAnimationOnShow(Animations.APPEARS_LEFT);

        registerPaneCtrl(topRightHBox)
                .setAnimationOnHide(Animations.DISAPPEARS_UP)
                .setAnimationOnShow(Animations.APPEARS_UP);

        registerPaneCtrl(leftVBox)
                .setAnimationOnHide(Animations.DISAPPEARS_LEFT)
                .setAnimationOnShow(Animations.APPEARS_LEFT);

        registerPaneCtrl(rightVBox)
                .setAnimationOnHide(Animations.DISAPPEARS_RIGHT)
                .setAnimationOnShow(Animations.APPEARS_RIGHT);

        registerPaneCtrl(bottomLeftHBox)
                .setAnimationOnHide(Animations.DISAPPEARS_DOWN)
                .setAnimationOnHide(Animations.APPEARS_DOWN);

        registerPaneCtrl(bottomRightHBox)
                .setAnimationOnHide(Animations.DISAPPEARS_DOWN)
                .setAnimationOnShow(Animations.APPEARS_DOWN);

        /*
        registerPaneCtrl(centerStackPane)
                .setAnimationOnHide(Animations.FADEOUT)
                .setAnimationOnShow(Animations.FADEIN);*/
        registerPaneCtrl(topCenterHBox)
                .setAnimationOnHide(Animations.DISAPPEARS_UP)
                .setAnimationOnShow(Animations.APPEARS_UP);

        registerPaneCtrl(bottomCenterHBox)
                .setAnimationOnShow(Animations.APPEARS_DOWN)
                .setAnimationOnHide(Animations.DISAPPEARS_DOWN);

        registerPaneCtrl(topToolBarVBox)
                .setAnimationOnShow(Animations.FADEIN)
                .setAnimationOnHide(Animations.FADEOUT);
    }

    public void populateWidgets() {

    }

    public void resetWidgets() {
        //widgetLoaderService.reload();
    }

    protected void showLoading() {
        logger.info("Showing loading screen");

        //Platform.runLater(() -> loadingScreen.showOn(mainAnchorPane));
    }

    protected void hideLoading() {
        logger.info("Hiding logging screen");
        //Platform.runLater(() -> loadingScreen.hideFrom(mainAnchorPane));
    }

    private AnimatedPaneContextualView registerPaneCtrl(Pane node) {

        // The UI plugin service implements the interface used to sort nodes inside the contextual containers
        UiPluginSorter sorter = uiPluginService;

        AnimatedPaneContextualView ctrl = new AnimatedPaneContextualView(sorter, node)
                .setOnUiPluginDisplayed(this::onUiPluginDisplaed);

        uiContextService.addContextualView(ctrl);
        uiPluginCtrl.put(node.getId(), ctrl);
        return ctrl;

    }

    protected void updateMemoryUsage() {

        if (memoryLabel == null) {
            return;
        }

        int free = (int) MemoryUtils.getAvailableMemory();
        int max = (int) MemoryUtils.getTotalMemory();
        int used = (max - free);

        double progress = 1.0 * (used) / max;

        // System.out.println(progress);
        memoryProgressBar.setProgress(progress);
        memoryLabel.setText(String.format("%d / %d MB", used, max));

    }

    /*
    protected void bindWidgetsToControllers() {

        imageJ
                .getContext()
                .getService(DefaultUiPluginService.class
                )
                .getUiPluginList()
                .forEach(uiPlugin -> {
                    loadWidget(uiPlugin);
                });
    }
     */
    protected void loadWidget(UiPlugin uiPlugin) {

        // getting the localization from the Localization Plugin (which gets
        // it from the annotation
        String localization
                = uiPluginService.getLocalization(uiPlugin);

        if (uiPluginCtrl.containsKey(localization) == false) {
            String message = String.format(
                    "The localization '%s' is not registered", localization
            );
            logger.warning(message);
            imageJ.ui().showDialog(message);
            return;
        }

        // register the node the controller
        uiPluginCtrl.get(localization).registerNode(uiPlugin.getUiElement());
    }

    @EventHandler
    public void handleEvent(UiPluginReloadedEvent event) {
        // Platform.runLater(() -> loadWidget(event.getWidget()));
    }

    @EventHandler
    public void handleEvent(DebugEvent event) {

        Platform.runLater(() -> showHelpSequence(new DefaultHint().setTarget("#debug-button").setText("This is a very long text which doesn't really matter !")));
    }

    @EventHandler
    public void handleEvent(ToggleSideMenuEvent event) {

        if (event.is(ToggleSideMenuEvent.SIDE_MENU)) {

            if (sideMenu.getTranslateX() >= 0.0) {
                hideSideMenu();
            } else {
                showSideMenu();
            }
        }
        if (event.is(ToggleSideMenuEvent.RELOAD_SIDE_MENU)) {

            Platform.runLater(() -> {
                resetMenuAction();
                showSideMenu();

            });
        }
    }

    @EventHandler
    public void handleEvent(NotificationEvent event) {

        showNotification(event.getNotification());
    }

    @EventHandler
    public synchronized void onHintRequested(HintRequestEvent event) {

        event.getHintList().forEach(hint -> {

            logger.info(String.format("Displayint hint %s", hint.getId()));

            if (hintQueue.parallelStream().filter(hint2 -> hint2.equals(hint)).count() == 0) {
                hintQueue.add(hint);
            }
        });

        //hintQueue.addAll(event.getHintList());
        Platform.runLater(() -> nextHint());
    }

    @EventHandler
    public void onActivityChanged(ActivityChangedEvent event) {
        System.out.println("?");
        logger.info("Activity changed : " + event.toString());
        Task updateTask = event.getActivity().updateOnShow();
        Runnable runnable = updateTask;
        if (runnable == null) {
            runnable = () -> {
            };

        }
        Platform.runLater(() -> mainBorderPane.setCenter(event.getActivity().getContent()));
        new AnimationChain()
                .animate(mainBorderPane.getCenter(), Animations.FADEOUT)
                .then(runnable)
                .thenInFXThread(() -> mainBorderPane.setCenter(event.getActivity().getContent()))
                .animate(event.getActivity().getContent(), Animations.FADEIN)
                .execute();

    }

    @EventHandler
    public void onFrontEndTaskSubmitted(FontEndTaskSubmitted event) {

       

        if (event.getObject() != null) {
            Platform.runLater(() -> {
                System.out.println("front end task submitted");
                if(event.getObject() == null) return;
                loadingPopup.bindTask(event.getObject())
                        .closeOnFinished();
                loadingPopup.showOnScene(this.getScene());
            });
        }
    }

    private void showNotification(Notification notification) {

        Platform.runLater(() -> {
            Notifications ns = Notifications
                    .create()
                    .title(notification.getTitle())
                    .text(notification.getText());

            //ns.hideCloseButton();
            ns.position(Pos.TOP_RIGHT);
            //ns.showConfirm();
            ns.hideAfter(Duration.hours(24));
            ns.show();

        });
    }

    /**
     *
     * Side Menu
     *
     */
    // Debugging convenient method
    private void resetMenuAction() {
        sideMenuMainTopVBox.getChildren().clear();
        initMenuAction();
    }

    // animating the appearance of the Menu
    public void showSideMenu() {

        final Timeline timeline = new Timeline();

        KeyValue kv = new KeyValue(sideMenu.translateXProperty(), 0, Interpolator.LINEAR);
        KeyFrame kf = new KeyFrame(ImageJFX.getAnimationDuration(), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();

    }

    // animating the disapearance of the menu
    @FXML
    public void hideSideMenu() {
        final Timeline timeline = new Timeline();

        KeyValue kv = new KeyValue(sideMenu.translateXProperty(), -1 * sideMenu.getWidth(), Interpolator.LINEAR);
        KeyFrame kf = new KeyFrame(ImageJFX.getAnimationDuration(), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();

    }

    public void onUiPluginDisplaed(ContextualWidget<Node> uiPlugin) {

        hintService.displayHints(uiPlugin.getObject().getClass(), false);

    }

    public synchronized void nextHint() {

        if (isHintDisplaying) {
            return;
        }

        showHelpSequence(hintQueue.poll());

    }

    public void showHelpSequence(Hint hint) {

        if (hint == null) {

            return;
        }

        Node node = mainAnchorPane.getScene().lookup(hint.getTarget());
        if (node == null) {
            logger.warning("Couldn't find node " + hint.getTarget());
            nextHint();

            return;
        }

        try {
            isHintDisplaying = true;
            double hintWidth = 200;
            double hintMargin = 20;
            double rectanglePadding = 5;
            Bounds nodeBounds = node.getLocalToSceneTransform().transform(node.getLayoutBounds());

            Bounds rectangleBounds;
            Rectangle rectangle = new Rectangle(nodeBounds.getMinX() - rectanglePadding, nodeBounds.getMinY() - rectanglePadding, nodeBounds.getWidth() + rectanglePadding * 2, nodeBounds.getHeight() + rectanglePadding * 2);
            Rectangle bigOne = new Rectangle(0, 0, mainAnchorPane.getScene().getWidth(), mainAnchorPane.getScene().getHeight());

            Shape highligther = Path.subtract(bigOne, rectangle);
            highligther.setFill(Paint.valueOf("black"));
            highligther.setOpacity(0.7);

            mainAnchorPane.getChildren().add(highligther);
            Label label = new Label(hint.getText());
            label.setPadding(new Insets(hintMargin));
            label.getStyleClass().add("help-label");
            label.setMaxWidth(hintWidth);
            label.setWrapText(true);
            label.setPrefWidth(hintWidth);

            rectangleBounds = rectangle.getBoundsInLocal();

            label.translateXProperty().bind(Bindings.createDoubleBinding(() -> {
                if (rectangleBounds.getMinX() < hintWidth + hintMargin) {
                    return rectangleBounds.getMaxX() + hintMargin + highligther.getTranslateX();

                } else {
                    return rectangleBounds.getMinX() - hintWidth - hintMargin + highligther.getTranslateX();
                }
            }, highligther.translateXProperty()));

            label.translateYProperty().bind(Bindings.createDoubleBinding(() -> {
                if (rectangleBounds.getMinY() + label.getHeight() > node.getScene().getHeight()) {
                    return rectangleBounds.getMaxY() - label.getHeight();
                } else {
                    return rectangleBounds.getMinY();
                }
            }, label.heightProperty()));

            //label.setTranslateY(nodeBounds.getMinY());
            Button gotItButton = new Button("Got it !");

            gotItButton.translateXProperty().bind(label.translateXProperty());

            gotItButton.setPrefWidth(hintWidth);

            gotItButton.getStyleClass().add(IjfxCss.WARNING);

            gotItButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.CHECK));

            // when clicking on the anywhere on the screen
            highligther.setOnMouseClicked(event -> {

                // creating a transition
                Transition transition = Animations.DISAPPEARS_RIGHT.configure(highligther, ImageJFX.getAnimationDurationAsDouble());

                // and when the transition if finished
                transition
                        .setOnFinished(event2
                                -> {
                            //deleting the elements fromthe main panel
                            mainAnchorPane.getChildren().removeAll(highligther, label, gotItButton);
                        });

                // set that the hint is not displayed again
                isHintDisplaying = false;

                // displaying the next hint
                nextHint();
                transition.play();

            });
            mainAnchorPane.getChildren().addAll(label, gotItButton);

            gotItButton.translateYProperty().bind(
                    Bindings.createDoubleBinding(
                            () -> {
                                return label.translateYProperty().getValue() + label.heightProperty().getValue() + hintMargin / 2;
                            }, label.translateYProperty(), label.heightProperty()));

            gotItButton.setOnAction(event -> {
                highligther.getOnMouseClicked().handle(null);
                hint.setRead();

                //if(hintQueue.size() ==0 ) {
                //}
            });

            //if(isHintDisplaying) {
            Animations.APPEARS_LEFT.configure(highligther, ImageJFX.getAnimationDurationAsDouble()).play();
            //}

            // gotItButton.setTranslateY(label.getTranslateY()+label.getBoundsInLocal().getHeight()+hintWidth/2);
            System.out.println(highligther.getLayoutBounds().getHeight());
        } catch (Exception e) {

            isHintDisplaying = false;
            nextHint();
            logger.log(Level.SEVERE, "Error when showing hint.", e);
        }

    }

    /**
     *
     * Side Menu Actions
     *
     */
    public void initMenuAction() {

        addSideMenuButton("Explore", FontAwesomeIcon.COMPASS, ExplorerActivity.class);

        addSideMenuButton("Visualize", FontAwesomeIcon.PICTURE_ALT, ImageJContainer.class);
        addSideMenuButton("Segment", FontAwesomeIcon.EYE, null).setOnAction(event -> {
            uiContextService.enter("segment", "segmentation");
            uiContextService.update();
            hideSideMenu();

        });

        /*
        
        sideMenuMainTopVBox.getChildren().addAll(
                new SideMenuButton("Create database", WebApps.PROJECT_WIZARD).setIcon(FontAwesomeIcon.MAGIC), new SideMenuButton("Visualize", ImageJContainer.class).setIcon(FontAwesomeIcon.PHOTO), new SideMenuButton("Batch Processing", FileBatchProcessorPanel.class).setIcon(FontAwesomeIcon.TASKS), new SideMenuButton("Personal Database", ProjectManager.class).setIcon(FontAwesomeIcon.DATABASE), new Separator(Orientation.HORIZONTAL), new SideMenuButton("Setting", "index").setIcon(FontAwesomeIcon.GEAR), new SideMenuButton("Explore", ExplorerActivity.class).setIcon(FontAwesomeIcon.COMPASS)
        );*/
    }

    private SideMenuButton addSideMenuButton(String title, FontAwesomeIcon icon, Class<? extends Activity> actClass) {

        SideMenuButton sideMenuButton = new SideMenuButton(title, actClass).setIcon(icon);

        sideMenuTopVBox.getChildren().add(sideMenuButton);

        return sideMenuButton;
    }

    private class SideMenuButton extends Button {

        String appToOpen;
        Class<? extends Activity> activityClass;

        public SideMenuButton(String name) {

            super();
            setText(name);
            setOnAction(this::onAction);
            getStyleClass().add("side-menu-button");
            setMaxWidth(Double.MAX_VALUE);
        }

        public SideMenuButton(String name, String appToOpen) {
            this(name);

            this.appToOpen = appToOpen;

        }

        public SideMenuButton(String name, Class<? extends Activity> activityClass) {
            this(name);
            this.activityClass = activityClass;
        }

        public SideMenuButton setIcon(FontAwesomeIcon icon) {
            Node iconNode = new FontAwesomeIconView(icon); //GlyphsDude.createIcon(icon);
            iconNode.getStyleClass().add("side-menu-icon");
            if (icon != null) {
                setGraphic(iconNode);
            }
            return this;
        }

        public void onAction(ActionEvent event) {

            if (appToOpen != null) {

                appService.showApp(appToOpen);

            } else {
                activityService.openByType(this.activityClass);
            }

            hideSideMenu();
        }
    }

}
