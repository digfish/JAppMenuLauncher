package org.digfish.jappmenulauncher;

import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.*;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

public class FileListTransferHandler extends TransferHandler {

    private JList list1;
    private int[] indices;
    private int addIndex = -1;
    private int addCount = 0;

    public FileListTransferHandler(JList list1) {
        this.list1 = list1;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        // TODO Auto-generated method stub
        //return super.canImport(support);
        System.out.print("canImport => ");
        System.out.println(Arrays.toString(support.getDataFlavors()));
        if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                support.isDataFlavorSupported(DataFlavor.stringFlavor))
            return true;
        else return false;
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        // TODO Auto-generated method stub
        return super.canImport(comp, transferFlavors);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        // TODO Auto-generated method stub

        this.indices = this.list1.getSelectedIndices();
        Object[] values = this.list1.getSelectedValues();

        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < values.length; i++) {
            Object val = values[i];
            buff.append(val == null ? "" : val.toString());
            if (i != values.length - 1) {
                buff.append("\n");
            }
        }

        return new StringSelection(buff.toString());
        //return super.createTransferable(c);
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        //System.out.println(String.join(",", "exportAsDrag", "" + comp, "" + e, "" + action));
        // TODO Auto-generated method stub
        super.exportAsDrag(comp, e, action);
    }

    private int foundLauncherItemByTitle(String title,Collection<AppMenuLauncherItem> list) {
        Iterator<AppMenuLauncherItem> it = list.iterator();
        int i=0;
        while (it.hasNext()) {
            AppMenuLauncherItem thisItem = it.next();
            if (thisItem.title.equals(title)) {
                return i;
            } else i++;
        }
        return -1;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        System.out.println(String.join(",", "exportDone", "" + source, "" + action, "" + action));
        // TODO Auto-generated method stub
        //super.exportDone(source, data, action);
        DefaultListModel<AppMenuLauncherItem> listModel  = (DefaultListModel<AppMenuLauncherItem>)this.list1.getModel();
        String title = null;
        try {
            title = (String) data.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<AppMenuLauncherItem> arrayList = new ArrayList<AppMenuLauncherItem>();
        for (int i=0; i< listModel.size();i++) {
            arrayList.add(listModel.elementAt(i));
        }

        int foundIdx = this.foundLauncherItemByTitle(title, arrayList);
        // drag and drop interrupted, the drag was to the outside, so let's remove this item !
        if (action == TransferHandler.MOVE && this.addIndex == -1 && foundIdx >=0) {
            System.out.println("Removing item " + title);
            listModel.remove(foundIdx);
        }

        this.indices = null;
        this.addCount = 0;
        this.addIndex = -1;
        this.list1.clearSelection();
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        // TODO Auto-generated method stub
        super.exportToClipboard(comp, clip, action);
    }

    @Override
    public Image getDragImage() {
        // TODO Auto-generated method stub
        System.out.println("getDragImage");
        return super.getDragImage();
    }

    @Override
    public Point getDragImageOffset() {
        // TODO Auto-generated method stub
        System.out.print("getDragImageOffset");
        return super.getDragImageOffset();
    }

    @Override
    public int getSourceActions(JComponent c) {
        // TODO Auto-generated method stub
        return COPY_OR_MOVE;
    }

    @Override
    public Icon getVisualRepresentation(Transferable t) {
        // TODO Auto-generated method stub
        System.out.println("getVisualRepresentation");
        return super.getVisualRepresentation(t);
    }

    @Override
    public boolean importData(TransferSupport support) {
        try {
            @SuppressWarnings("rawtypes")
            JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
            int index = dropLocation.getIndex();
            boolean insert = dropLocation.isInsert();
            List fileDragged = null;
            String stringDragged = null;
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                stringDragged = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            } else {
                fileDragged = (List) support.getTransferable().getTransferData(
                        DataFlavor.javaFileListFlavor);
            }
            if (fileDragged != null && fileDragged.size() > 0) {  // if a file was dropped
                DefaultListModel<AppMenuLauncherItem> listModel = (DefaultListModel<AppMenuLauncherItem>) list1.getModel();
                for (Object item : fileDragged) {
                    File file = (File) item;
                    System.out.println("Added " + file);
                    AppMenuLauncherItem newItem = new AppMenuLauncherItem();
                    newItem.exe_path = ((File) item).getAbsolutePath();
                    String sep = File.separator;
                    newItem.title = newItem.exe_path.substring(newItem.exe_path.lastIndexOf(sep) + 1, newItem.exe_path.lastIndexOf('.'));
                    listModel.addElement(newItem);
                }
            } else if (stringDragged != null) { // if a list item was reordered
                // Wherever there is a newline in the incoming data,
                // break it into a separate item in the list.
                String[] values = stringDragged.split("\n");

                this.addIndex = index;
                this.addCount = values.length;

                DefaultListModel<AppMenuLauncherItem> listModel = (DefaultListModel<AppMenuLauncherItem>) list1.getModel();
                // Perform the actual import.
                for (int i = 0; i < values.length; i++) {
                    String title = values[i];

                    Enumeration<AppMenuLauncherItem> launcherItemsEnum  = listModel.elements();
                    //Iterator<AppMenuLauncherItem> it = launcherItemsEnum.asIterator();
                    AppMenuLauncherItem launcherItemFound = null;
                    int foundIdx = 0;
                    while (launcherItemsEnum.hasMoreElements()) {
                        AppMenuLauncherItem thisItem = launcherItemsEnum.nextElement();
                        if (thisItem.title.equals(title)) {
                            launcherItemFound = thisItem;
                            break;
                        }
                        foundIdx++;
                    }
                    if (foundIdx == listModel.size()) {
                        return false;
                    }
                    if (index == -1) {
                        index = listModel.getSize()-1;
                        this.addIndex = index;
                    }
                    AppMenuLauncherItem itemToBeMoved = listModel.remove(foundIdx);
                    listModel.add(index++,  itemToBeMoved);

                    }
                }



            return true;

        } catch (UnsupportedFlavorException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean importData(JComponent comp, Transferable t) {
        // TODO Auto-generated method stub
        return super.importData(comp, t);
    }

    @Override
    public void setDragImage(Image img) {
        // TODO Auto-generated method stub
        super.setDragImage(img);
    }

    @Override
    public void setDragImageOffset(Point p) {
        // TODO Auto-generated method stub
        super.setDragImageOffset(p);
    }


}
