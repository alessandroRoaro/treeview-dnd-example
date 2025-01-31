package treedrag;

import java.util.Objects;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;

public class TaskCellFactory implements Callback<TreeView<TaskNode>, TreeCell<TaskNode>> {
    private static final DataFormat JAVA_FORMAT = new DataFormat("application/x-java-serialized-object");
    private static final String DROP_HINT_STYLE = "-fx-border-color: #eea82f; -fx-border-width: 0 0 2 0; -fx-padding: 3 3 1 3";
    private static final Image TASKS_IMAGE = new Image("/treedrag/tasks.png");
    private static final Image PIN_IMAGE = new Image("/treedrag/pin.png");
    private TreeCell<TaskNode> dropZone;
    private TreeItem<TaskNode> draggedItem;

    @Override
    public TreeCell<TaskNode> call(TreeView<TaskNode> treeView) {
        TreeCell<TaskNode> cell = new TreeCell<TaskNode>() {
            @Override
            protected void updateItem(TaskNode item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) return;

                ImageView iv1 = new ImageView();
                if (item.getName().equals("Tasks")) {
                    iv1.setImage(TASKS_IMAGE);
                }
                else {
                    iv1.setImage(PIN_IMAGE);
                }
                setGraphic(iv1);
                setText(item.getName());
            }
        };
        cell.setOnDragDetected((MouseEvent event) -> dragDetected(event, cell, treeView));
        cell.setOnDragOver((DragEvent event) -> dragOver(event, cell, treeView));
        cell.setOnDragDropped((DragEvent event) -> drop(event, cell, treeView));
        cell.setOnDragDone((DragEvent event) -> clearDropLocation());
        
        return cell;
    }

    private void dragDetected(MouseEvent event, TreeCell<TaskNode> treeCell, TreeView<TaskNode> treeView) {
        draggedItem = treeCell.getTreeItem();

        // root can't be dragged
        if (draggedItem.getParent() == null) return;
        Dragboard db = treeCell.startDragAndDrop(TransferMode.MOVE);

        ClipboardContent content = new ClipboardContent();
        content.put(JAVA_FORMAT, draggedItem.getValue());
        db.setContent(content);
        db.setDragView(treeCell.snapshot(null, null));
        event.consume();
    }

    private void dragOver(DragEvent event, TreeCell<TaskNode> treeCell, TreeView<TaskNode> treeView) {
        if (!event.getDragboard().hasContent(JAVA_FORMAT)) return;
        TreeItem<TaskNode> thisItem = treeCell.getTreeItem();

        // can't drop on itself
        if (draggedItem == null || thisItem == null || thisItem == draggedItem) return;
        // ignore if this is the root
        if (draggedItem.getParent() == null) {
            clearDropLocation();
            return;
        }

        event.acceptTransferModes(TransferMode.MOVE);
        if (!Objects.equals(dropZone, treeCell)) {
            clearDropLocation();
            this.dropZone = treeCell;
            dropZone.setStyle(DROP_HINT_STYLE);
        }
    }

    private void drop(DragEvent event, TreeCell<TaskNode> treeCell, TreeView<TaskNode> treeView) {
        Dragboard db = event.getDragboard();
		event.setDropCompleted(false);
        if (!db.hasContent(JAVA_FORMAT)) return;

        TreeItem<TaskNode> thisItem = treeCell.getTreeItem();
        TreeItem<TaskNode> droppedItemParent = draggedItem.getParent();

        // remove from previous location
        droppedItemParent.getChildren().remove(draggedItem);

        // dropping on parent node makes it the first child
        if (Objects.equals(droppedItemParent, thisItem)) {
            thisItem.getChildren().add(0, draggedItem);
            treeView.getSelectionModel().select(draggedItem);
        }
        else {
            // add to new location
            int indexInParent = thisItem.getParent().getChildren().indexOf(thisItem);
            thisItem.getParent().getChildren().add(indexInParent + 1, draggedItem);
        }
        treeView.getSelectionModel().select(draggedItem);
        event.setDropCompleted(true);
    }

    private void clearDropLocation() {
        if (dropZone != null) dropZone.setStyle("");
    }
}
