/*
 * Script to explore segmentation results.
 *
 * The UI allows to easily show/hide classified annotations and detections.
 *
 * For detections, you can show/hide the stroke.
 *
 * If new annotations or detections are added, you will need to update the panel.
 *
 * @author: Isaac Vieco-Martí
 *
 */
 
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.control.Tab
import javafx.scene.layout.GridPane
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.fx.dialogs.Dialogs
import qupath.lib.gui.tools.GuiTools
import qupath.fx.utils.GridPaneUtils
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjectTools
import qupath.lib.objects.PathObjects
import qupath.lib.objects.classes.PathClass
import qupath.lib.roi.GeometryTools
import javafx.collections.FXCollections
import qupath.fx.utils.FXUtils
import qupath.lib.plugins.parameters.ParameterList;
import qupath.process.gui.commands.ml.ClassificationResolution;
import qupath.lib.images.ImageData;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Spinner;
import ij.IJ;
import qupath.opencv.tools.MultiscaleFeatures.MultiscaleFeature;
import javafx.scene.control.ScrollPane



def customId = "Explore Segmentations"
Platform.runLater {
    gui = QuPathGUI.getInstance()
    panelTabs = gui.getAnalysisTabPane().getTabs()
    RemoveTab(panelTabs,customId)
    
    def pane = buildPane()
    Tab newTab = new Tab("Explore Segmentations", pane)
    newTab.setId(customId)
    panelTabs.add(newTab)
    //This selects the new tab
    gui.getAnalysisTabPane().getSelectionModel().select(newTab);
    gui.mainPaneManager.getAnalysisTabPane().makeTabsUndockable()
}

// Remove all the additions made to the Analysis panel, based on the id above
def RemoveTab(panelTabs, id) {
    while(1) {
        hasElements = false
        for (var tabItem : panelTabs) {
            if (tabItem.getId() == id) {
                panelTabs.remove(tabItem)
                hasElements = true
                break
            }
        }
        if (!hasElements) break
    }
}


ScrollPane buildPane() {
    def qupath = QuPathGUI.getInstance()
    ScrollPane sp = new ScrollPane();
    sp.setFitToWidth(true);
    def pane = new GridPane()

    int row = 0  
    
    //////////
    //TITLE//
    //////////
    
    row++
    row++
    firstTitle = new Label("GUI MAIN COMMANDS")
    firstTitle.setStyle("-fx-font-weight: bold")
    pane.add(firstTitle, 0,row, 1, 1)
    
    
    //////////////////
    ///UPDATE BUTTON//
    //////////////////
    row++
    row++
     
    
    def updateBtn = new Button("Update Panel")
    pane.add(updateBtn, 0, row, 1, 1)
    
    def showAllBtn = new Button("Show all")
    pane.add(showAllBtn, 1, row, 1, 1)
    
    def hideAllBtn = new Button("Hide all")
    pane.add(hideAllBtn, 2, row, 1, 1)
    GridPaneUtils.setToExpandGridPaneWidth(updateBtn,showAllBtn,hideAllBtn)
    
    
    updateBtn.setOnAction { e ->
        // Clear the pane but keep the first title and the button
        pane.getChildren().clear()

        // Re-add title and update button after clearing
        pane.add(firstTitle, 0, 2, 1, 1)
        pane.add(updateBtn, 0, 4, 1, 1)
        pane.add(showAllBtn, 1, 4, 1, 1)
        pane.add(hideAllBtn, 2, 4, 1, 1)
        GridPaneUtils.setToExpandGridPaneWidth(updateBtn,showAllBtn,hideAllBtn)

        // Rebuild the blocks for annotations and detections
        int newRow = 6 // Starting row after the button

        clustersTitle = new Label("ANNOTATIONS")
        clustersTitle.setStyle("-fx-font-weight: bold")
        pane.add(clustersTitle, 0, newRow, 1, 1)

        updatedAnnotationList = updateAnnotationsList()
        newRow = creteBlocksFromList(updatedAnnotationList, newRow, pane)

        newRow++
        newRow++
        newRow++
        newRow++
        detectionsTitle = new Label("DETECTIONS")
        detectionsTitle.setStyle("-fx-font-weight: bold")
        pane.add(detectionsTitle, 0, newRow, 1, 1)

        updatedDetectionList = updateDetectionsList()
        newRow = creteBlocksFromList(updatedDetectionList, newRow, pane)
        
        if(updatedDetectionList.size() > 0 ) {
           showHideStrokeDetectionButtons(newRow, pane) 
        }
        
        
    }
    
    
    
    //show all button
    showAllBtn.setOnAction { e->
    
     objectsToShow = updateAllObjectsList()
     
     
     objectsToShow.forEach {
         actionShow(it.toString())
     }
        
        
    }
    
    //hide all button
    hideAllBtn.setOnAction { e->
        
        objectsToHide = updateAllObjectsList()
        
        objectsToHide.forEach {
         actionHide(it.toString())
         }
       
    }
    
   
    
  
    
    
    pane.setHgap(10)
    pane.setVgap(5)
    
    sp.setContent(pane);
    
    return sp
}



//FUNCTION TO CREATE THE GUI
def createBlockSelection(classificationString,row,pane) {
   
    row++
    row++
    verySmallLabel= new Label(classificationString)
    verySmallLabel.setStyle("-fx-underline: true")
    pane.add(verySmallLabel, 0,row, 1, 1)
    
    row++
    row++
    
    
  
    def show = new Button("Show")
    def hide = new Button("Hide")
    def select= new Button("Select")
    
    pane.add(show, 0, row, 1, 1)
    pane.add(hide, 1, row, 1, 1)
    pane.add(select, 2, row, 1, 1)
    
    classObject = getPathClass(classificationString)
    
    GridPaneUtils.setToExpandGridPaneWidth(show,hide,select)
    
    
    
    show.setOnAction {e ->
    
        actionShow(classificationString)
        
        }
    
    hide.setOnAction {e ->
    
        actionHide(classificationString)
        
        }
    
    select.setOnAction {e ->
    
        actionSelect(classificationString)
        
        }
   
    return row
   
   
}



def actionShow(classificationString) {
   
    getCurrentViewer().getOverlayOptions().setPathClassHidden(getPathClass(classificationString),false)
   
 
   
}


def actionHide(classificationString) {
   
    getCurrentViewer().getOverlayOptions().setPathClassHidden(getPathClass(classificationString),true)
   
 
   
}


def actionSelect(classificationString) {
   
    objects = getAllObjects().findAll {
       it.getPathClass() == getPathClass(classificationString) 
       
    }
    
    selectObjects(objects)

   
}



//Update list of classificaitons

def updateAnnotationsList() {
   
   anotations = getAnnotationObjects()
   
   annotationsList =  new HashSet()
 
   anotations.forEach {
    
     annoClass = it.getPathClass().toString()
       if(annoClass != "null") {
           annotationsList.add(annoClass) 
       }
         
      
   }
  
   //sorts annotations by name
   annotationsList = annotationsList.sort()
   
   return annotationsList
   
}



def updateDetectionsList() {
    
    detections = getDetectionObjects()
    detectionsList = new HashSet()
    
    detections.forEach {
        detClass = it.getPathClass().toString()
        if(detClass != "null") {
            detectionsList.add(detClass) 
         }
        
    }
    
    detectionsList = detectionsList.sort()
    return detectionsList
    
}


def updateAllObjectsList() {
    
    objects = getAllObjects(false)
    objectsList = new HashSet()
    
    objects.forEach {
        objectsClass = it.getPathClass().toString()
        if(objectsClass != "null") {
            objectsList.add(objectsClass) 
         }
        
    }
    
    objectsList = objectsList.sort()
    
    return objectsList
    
}

//create a funciton to create the blocks from the list

def creteBlocksFromList(blocksList,row,pane) {
   
   blocksList.forEach {
       
       print(it.toString())
       rowUpdate= createBlockSelection(it.toString(), row, pane )
       row = rowUpdate
   }
   
   
   return row
   
}



import qupath.lib.gui.prefs.PathPrefs
def showHideStrokeDetectionButtons(row, pane) {
    
    row+=10
    
    strokeTitle = new Label("Stroke Show")
    strokeTitle.setStyle("-fx-font-weight: bold")
    pane.add(strokeTitle, 0, row, 1, 1)
    
    row+=3
    
   def hideStroke = new Button("Hide Detection Stroke")
    pane.add(hideStroke, 0, row, 1, 1)
    
    def showStroke = new Button("Show Detection Stroke")
    pane.add(showStroke, 1, row, 1, 1)
    GridPaneUtils.setToExpandGridPaneWidth(hideStroke,showStroke)
    
    
    hideStroke.setOnAction{ e->
        strokeThicknessProperty = PathPrefs.detectionStrokeThicknessProperty()
        strokeThicknessProperty.set(0.1)
        
    }
    
    showStroke.setOnAction{ e->
        strokeThicknessProperty = PathPrefs.detectionStrokeThicknessProperty()
        strokeThicknessProperty.set(2.0)
        
    }
    
   
   
}

