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
package ijfx.ui.filter;

import java.util.ArrayList;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.math3.random.RandomDataGenerator;

/**
 *
 * @author cyril
 */
public class FilterTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        NumberFilter filter = new DefaultNumberFilter();
        Scene scene = new Scene((Parent)filter.getContent());
        primaryStage.setScene(scene);

        ArrayList<Double> numbers = new ArrayList<>();
        RandomDataGenerator generator = new RandomDataGenerator();

        for (int i = 0; i != 300; i++) {
            numbers.add(new Double(generator.nextInt(0, 3)));
        }

        filter.setAllPossibleValue(numbers);
        
        
        primaryStage.show();
        
    }
    public static void main(String... args) {
        launch(args);
    }
}
