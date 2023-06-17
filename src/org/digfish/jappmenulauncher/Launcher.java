package org.digfish.jappmenulauncher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

class AppMenuLauncherItem implements JSONAware {
    public String title;
    public String exe_path;
    public String env_ini;
    public String tmpicon;

    public AppMenuLauncherItem(String exe_path) {
        String sep = File.separator;
        this.exe_path = exe_path;
        if (exe_path.lastIndexOf(sep) >= 0) {
            this.title = exe_path.substring(exe_path.lastIndexOf(sep) + 1, exe_path.lastIndexOf('.'));
        }
    }

    public AppMenuLauncherItem() {
        new AppMenuLauncherItem("");
    }


    public static AppMenuLauncherItem fromMap(Map<String,String> mapItem) {
        AppMenuLauncherItem item = new AppMenuLauncherItem();
        item.title = (String) mapItem.get("title");
        item.exe_path = (String) mapItem.get("exe_path");
        item.env_ini = (String) mapItem.get("env_ini");
        item.tmpicon = (String) mapItem.get("tmpicon");
        return item;
    }

    @Override
    public String toString() {
        return this.title;
    }

    @Override
    public String toJSONString() {
        Map map = new LinkedHashMap();
        Field [] fields = getClass().getDeclaredFields();
        try {
            for (Field field : fields) {
                map.put(field.getName(), field.get(this));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            return JSONObject.toJSONString(map);
        }
    }
}


public class Launcher extends JFrame {

    final String APP_JSON = "launcher.json";
    private JList list1;
    private JPanel panel1;
    private JPanel panel2;

    private JMenuBar menubar;

    private String dirpath;
    private TrayIcon trayIcon;


    public Launcher(String dirpath) {
        this.createUIComponents(dirpath);
        this.dirpath = dirpath;
    }

    public String[] getDirTree(String dirpath,String extension) {
        File dir = new File(dirpath);
        ArrayList list = new ArrayList();
        if (dir.isDirectory()) {
            return dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(extension);
                }
            });
        } else {
            return null;
        }
    }


    public void exit() {
        this.saveJson();
        this.dispose();
        System.exit(0);
    }


    public Object[] loadJson() {
        ArrayList<AppMenuLauncherItem> arrayList = new ArrayList<AppMenuLauncherItem>();
        JSONParser parser = new JSONParser();
        try {
            JSONObject o = (JSONObject) parser.parse(new FileReader(APP_JSON));
            //System.out.println(o);
            LinkedList<String> keySet = new LinkedList((Set<String>)o.keySet());
            for (int i = 0; i<keySet.size(); i++ ) {
                String title = keySet.get(i);
                arrayList.add(AppMenuLauncherItem.fromMap((Map) o.get(title)));
            }

        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
        return arrayList.toArray();
    }
    public void saveJson() {
       ListModel model  = this.list1.getModel();
       HashMap<String,AppMenuLauncherItem> items = new HashMap<String,AppMenuLauncherItem>();

         for (int i=0; i<model.getSize(); i++) {
             AppMenuLauncherItem item = (AppMenuLauncherItem) model.getElementAt(i);
             AppMenuLauncherItem newItem = new AppMenuLauncherItem(item.exe_path);
             items.put(item.title,newItem);
         }

        //JSONObject obj = new JSONObject();
        String jsonStr = JSONObject.toJSONString(items);
         System.out.println("Saving JSON:"+jsonStr);
        try {
            FileWriter writer = new FileWriter("launcher.json");
            writer.write(jsonStr);
            writer.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
    }

    public String getDirpath() {
        return dirpath;
    }

    public class FileListRenderer extends DefaultListCellRenderer {

            String dirpath = "";

            public void setDirPath(String dirpath) {
                this.dirpath = dirpath;
            }

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
                try {
                    AppMenuLauncherItem item = (AppMenuLauncherItem) value;
                    String filepath = item.exe_path;
                    Icon ico = FileSystemView.getFileSystemView().getSystemIcon(new File(filepath));

                    //System.out.println((String)value+ico);
                    this.setIcon(ico);
                } catch (Exception ex) {
                    ex.printStackTrace();
                return null;
                }
                return this;
            }

    }

    private void exec(String cmdline) {
        try {
            Runtime.getRuntime().exec( cmdline);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createUIComponents(String dirpath) {

        TrayIcon trayIcon = null;


        Launcher outer = this;
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        Object[] appMenuLauncherItems = this.loadJson();

        this.menubar = new JMenuBar();
        panel1.add(menubar, BorderLayout.NORTH);
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        JMenu viewMenu = new JMenu("View");
        JMenuItem horizontalMenuItem = new JMenuItem("Horizontal");
        JMenuItem verticalMenuItem = new JMenuItem("Vertical");
        viewMenu.add(horizontalMenuItem);
        horizontalMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                list1.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            }
        });
        viewMenu.add(verticalMenuItem);
        verticalMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                list1.setLayoutOrientation(JList.VERTICAL_WRAP);
            }
        });
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutDialg dialog = new AboutDialg();
                dialog.pack();
                dialog.setVisible(true);
            }
        });
        helpMenu.add(aboutMenuItem);
        this.menubar.add(fileMenu);
        this.menubar.add(viewMenu);
        this.menubar.add(helpMenu);
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outer.dispose();
            }
        });
        fileMenu.add(exitMenuItem);
        //panel1.add(new JSeparator());

        this.add(panel1);
        //panel1.setSize(100, 500);

        DefaultListModel listModel = new DefaultListModel<AppMenuLauncherItem>();
        list1 = new JList<AppMenuLauncherItem>(listModel);
        FileListRenderer renderer = new FileListRenderer();
        renderer.setDirPath(dirpath);
        list1.setCellRenderer(renderer);
        //String [] dirtree = getDirTree(dirpath,"exe");
        for (int i = 0; i < appMenuLauncherItems.length; i++)
            listModel.addElement((AppMenuLauncherItem)appMenuLauncherItems[i]);

        list1.setDragEnabled(true);
        list1.setTransferHandler(new FileListTransferHandler(this.list1));
        list1.setDropMode(DropMode.USE_SELECTION);

        list1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                System.out.println("Mouse dragged:"+e);
            }
        });

        list1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    try {
                        AppMenuLauncherItem item = (AppMenuLauncherItem) list1.getSelectedValue();
                        String cmdline = item.exe_path;
                        System.out.println("Executing " + cmdline);
                        outer.exec(cmdline);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(list1);
        scrollPane.setPreferredSize(new Dimension(235, 390));
        this.panel1.add(scrollPane);

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                outer.exit();
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });
        try {
            this.setIconImage(ImageIO.read(new File("src/app-menu-launcher.png")));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.panel1.setSize(100, 500);
        this.setTitle("AppMenuLauncher");

        if (SystemTray.isSupported()) {
            this.initTrayIcon();
        }

        validate();
        this.pack();

    }

    private void initTrayIcon() {
        SystemTray sysTray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage("src/app-menu-launcher.png");
        PopupMenu trayMenu = new PopupMenu();
        ListModel listModel =  this.list1.getModel();
        Launcher outer = this;
        for ( int i=0; i< listModel.getSize(); i++) {
            AppMenuLauncherItem launcherItem = (AppMenuLauncherItem) listModel.getElementAt(i);
            MenuItem appItem = new MenuItem(launcherItem.title);
            appItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    outer.exec(launcherItem.exe_path);
                    System.out.println("Launching " + launcherItem.exe_path);
                }
            });
            trayMenu.add(appItem);
        }
        trayMenu.addSeparator();
        MenuItem trayShowMenuItem = new MenuItem("Show");
        trayShowMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                outer.setVisible(true);
            }
        });
        MenuItem trayExitMenuItem = new MenuItem("Exit");
        trayExitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outer.exit();
            }
        });
        trayMenu.add(trayShowMenuItem);
        trayMenu.add(trayExitMenuItem);

        this.trayIcon = new TrayIcon(image,this.getTitle(),trayMenu);
        this.trayIcon.addActionListener(trayShowMenuItem.getActionListeners()[0]);
        try {
            sysTray.add(this.trayIcon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            String dirpath = String.join(" ",args);
            Launcher launcher = new Launcher(dirpath);
            launcher.setVisible(true);
        });
    }

}

