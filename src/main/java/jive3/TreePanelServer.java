package jive3;

import javax.swing.*;
import javax.swing.tree.TreePath;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoApi.*;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import fr.esrf.tangoatk.widget.util.ATKGraphicsUtils;
import jive.JiveUtils;
import jive.ServerDlg;
import jive.DevWizard;


/**
 * A panel for selecting tango servers
 */
public class TreePanelServer extends TreePanel {

  // Filtering stuff
  String  serverFilterString="*";
  Pattern serverPattern=null;
  String[] serverList;

  public TreePanelServer(MainPanel parent) {

    this.invoker = parent;
    this.self = this;
    setLayout(new BorderLayout());

  }

  public TangoNode createRoot() {
    return new RootNode();
  }

  public void selectDevice(String className,String serverName,String devName) {

    int slash = serverName.indexOf('/');
    String server   = serverName.substring(0,slash);
    String instance = serverName.substring(slash+1);

    // Search server
    TangoNode srvNode = searchNode(root,server);
    if(srvNode==null) return;
    TangoNode instNode = searchNode(srvNode,instance);
    if(instNode==null) return;
    TangoNode classNode = searchNode(instNode,className);
    if(classNode==null) return;
    TangoNode devNode = searchNode(classNode,devName);
    if(devNode==null) return;
    TreePath selPath = new TreePath(root);
    selPath = selPath.pathByAddingChild(srvNode);
    selPath = selPath.pathByAddingChild(instNode);
    selPath = selPath.pathByAddingChild(classNode);
    selPath = selPath.pathByAddingChild(devNode);
    tree.setSelectionPath(selPath);

  }

  public void selectClass(String className,String serverName) {

    int slash = serverName.indexOf('/');
    String server   = serverName.substring(0,slash);
    String instance = serverName.substring(slash+1);

    // Search server
    TangoNode srvNode = searchNode(root,server);
    if(srvNode==null) return;
    TangoNode instNode = searchNode(srvNode,instance);
    if(instNode==null) return;
    TangoNode classNode = searchNode(instNode,className);
    if(classNode==null) return;
    TreePath selPath = new TreePath(root);
    selPath = selPath.pathByAddingChild(srvNode);
    selPath = selPath.pathByAddingChild(instNode);
    selPath = selPath.pathByAddingChild(classNode);
    tree.setSelectionPath(selPath);

  }

  public boolean isServer(String exeName) {
    TangoNode srvNode = searchNode(root,exeName);
    return srvNode!=null;
  }

  public TangoNode selectFullServer(String serverName) {

    int slash = serverName.indexOf('/');
    String server   = serverName.substring(0,slash);
    String instance = serverName.substring(slash+1);

    // Search server
    TangoNode srvNode = searchNode(root,server);
    if(srvNode==null) return null;
    TangoNode instNode = searchNode(srvNode,instance);
    if(instNode==null) return null;
    TreePath selPath = new TreePath(root);
    selPath = selPath.pathByAddingChild(srvNode);
    selPath = selPath.pathByAddingChild(instNode);
    tree.setSelectionPath(selPath);
    return instNode;

  }

  public TangoNode selectServerRoot(String serverName) {

    // Search server
    TangoNode srvNode = searchNode(root,serverName);
    if(srvNode==null) return null;
    TreePath selPath = new TreePath(root);
    selPath = selPath.pathByAddingChild(srvNode);
    tree.setSelectionPath(selPath);
    return srvNode;

  }

  public void applyFilter(String filter) {

    serverFilterString = filter;

    if( filter.equals("*") ) {
      serverPattern = null;
    } else if (filter.length()==0) {
      serverPattern = null;
    } else {
      try {
        String f = filterToRegExp(serverFilterString);
        serverPattern = Pattern.compile(f);
      } catch (PatternSyntaxException e) {
        JOptionPane.showMessageDialog(invoker,e.getMessage());
      }
    }

  }

  public String getFilter() {
    return serverFilterString;
  }

  public String[] getServerList() {
    return serverList;
  }

  // ---------------------------------------------------------------

  class RootNode extends TangoNode {

    void populateNode() throws DevFailed {
      serverList = db.get_server_name_list();
      for (int i = 0; i < serverList.length; i++) {
        if( serverPattern!=null ) {
          Matcher matcher =  serverPattern.matcher(serverList[i].toLowerCase());
          if( matcher.find() && matcher.start()==0 && matcher.end()==serverList[i].length() ) {
            add(new ServerNode(serverList[i]));
          }
        } else {
          add(new ServerNode(serverList[i]));
        }
      }
    }

    public String toString() {
      return "Server:";
    }

  }

  // ---------------------------------------------------------------

  class ServerNode extends TangoNode {

    private String server;

    ServerNode(String server) {
      this.server = server;
    }

    void populateNode() throws DevFailed {
      String[] list = db.get_instance_name_list(server);
      for (int i = 0; i < list.length; i++)
        add(new InstanceNode(server, list[i]));
    }

    public String toString() {
      return server;
    }

    ImageIcon getIcon() {
      return TangoNodeRenderer.srvicon;
    }

    int[] getAction() {
      if (JiveUtils.readOnly)
        return new int[0];
      else
        return new int[]{ACTION_RENAME};
    }

    void execAction(int actionNumber) {

      switch(actionNumber) {

        // ---------------------------------------------------------------------------
        case ACTION_RENAME:

          // Rename a server
          String newName = JOptionPane.showInputDialog(null,"Rename server",server);
          if(newName==null) return;
          if(searchNodeCaseSensitive(root,newName)!=null) {
            JiveUtils.showJiveError("Name already exists.");
            return;
          }

          // Clone instances,classes and devices
          for(int i=0;i<getChildCount();i++) {

            // Instance
            TangoNode n0 = (TangoNode)getChildAt(i);
            for(int j=0;j<n0.getChildCount();j++) {
              // Class
              TangoNode n1 = (TangoNode)n0.getChildAt(j);
              for(int k=0;k<n1.getChildCount();k++) {
                // Device
                TangoNode n2 = (TangoNode)n1.getChildAt(k);
                try {
                  db.add_device(n2.toString(),n1.toString(),newName+"/"+n0.toString());
                } catch (DevFailed e) {
                  JiveUtils.showTangoError(e);
                }
              }
            }
            // Delete the old server
            if( !newName.equalsIgnoreCase(server) ) {
              try {
                db.delete_server(server+"/"+n0.toString());
              } catch (DevFailed e) {
                JiveUtils.showTangoError(e);
              }
            }

          }

          // Refresh the tre
          refresh();
          selectServerRoot(newName);

          break;

      }

    }

  }

  // ---------------------------------------------------------------

  class InstanceNode extends TangoNode implements IServerAction {

    private String server;
    private String instance;

    InstanceNode(String server, String instance) {
      this.server = server;
      this.instance = instance;
    }

    void populateNode() throws DevFailed {

      String[] srvList = null;
      String[] dbList = null;
      int i;

      try {
        // Try to get class list through the admin device
        String admName = "dserver/" + server + "/" + instance;
        DeviceProxy adm = new DeviceProxy(admName);
        DeviceData datum = adm.command_inout("QueryClass");
        srvList = datum.extractStringArray();
      } catch (DevFailed e) {}

      // Get the list from the database
      dbList = db.get_server_class_list(server+"/"+instance);

      if(srvList!=null) {

        // Add actual class
        for (i = 0; i < srvList.length; i++)
          add(new ClassNode(server,instance,srvList[i]));

        // No add other class found in database as invalid
        for (i = 0; i < dbList.length; i++) {
          if(!JiveUtils.contains(srvList,dbList[i])) {
            add(new ClassNode(server,instance,dbList[i]));
          }
        }

      } else {

        // Old fashion
        for (i = 0; i < dbList.length; i++)
          add(new ClassNode(server,instance,dbList[i]));

      }

    }

    ImageIcon getIcon() {
      return TangoNodeRenderer.srvicon;
    }

    public String toString() {
      return instance;
    }

    String getValue() {

      String result = "";

      try {

        DbServInfo info = db.get_server_info(server + "/" + instance);
        result = info.toString();

      } catch (DevFailed e) {

        for (int i = 0; i < e.errors.length; i++) {
          result += "Desc -> " + e.errors[i].desc + "\n";
          result += "Reason -> " + e.errors[i].reason + "\n";
          result += "Origin -> " + e.errors[i].origin + "\n";
        }

      }

      return result;

    }

    String getTitle() {
      return "Server Info";
    }

    int[] getAction() {

      if (JiveUtils.readOnly)
        return new int[]{ACTION_TESTADMIN,
                ACTION_SAVESERVER
        };
      else {

          return new int[]{ACTION_RENAME,
                ACTION_DELETE,
                ACTION_ADDCLASS,
                ACTION_TESTADMIN,
                ACTION_SAVESERVER,
                ACTION_CLASSWIZ,
                ACTION_UNEXPORT,
                ACTION_DEV_DEPEND,
                ACTION_THREAD_POLL,
                ACTION_MOVE_SERVER};


      }

    }

    void execAction(int actionNumber) {

      int ok;

      switch(actionNumber) {

        // ----------------------------------------------------------------------------
        case ACTION_RENAME:

          // Rename an instance
          String newName = JOptionPane.showInputDialog(null,"Rename instance",instance);
          if(newName==null) return;
          if(searchNode((TangoNode)getParent(),newName)!=null) {
            JiveUtils.showJiveError("Name already exists.");
            return;
          }

          // Clone classes and devices
          for(int i=0;i<getChildCount();i++) {
            // Classes
            TangoNode n0 = (TangoNode)getChildAt(i);
            for(int j=0;j<n0.getChildCount();j++) {
              // Device
              TangoNode n1 = (TangoNode)n0.getChildAt(j);
                try {
                  db.add_device(n1.toString(),n0.toString(),server+"/"+newName);
                } catch (DevFailed e) {
                  JiveUtils.showTangoError(e);
                }
            }
          }
          try {
            db.delete_server(server+"/"+instance);
          } catch (DevFailed e) {
            JiveUtils.showTangoError(e);
          }

          // Refresh the tree
          refresh();
          selectFullServer(server + "/" + newName);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_DELETE:

          ok = JOptionPane.showConfirmDialog(invoker, "Delete server " + server + "/" + instance + " ?", "Confirm delete", JOptionPane.YES_NO_OPTION);
          if (ok == JOptionPane.YES_OPTION) {
            try {
              db.delete_server(server + "/" + instance);
            } catch (DevFailed e) {
              JiveUtils.showTangoError(e);
            }
            refresh();
            selectServerRoot(server);
          }
          break;

        // ----------------------------------------------------------------------------
        case ACTION_ADDCLASS:

          ServerDlg sdlg = new ServerDlg(this);
          sdlg.setClassList(invoker.getClassTreePanel().getClassList());
          sdlg.setValidFields(false, true);
          sdlg.setDefaults(server + "/" + instance, "");
          ATKGraphicsUtils.centerFrame(invoker.innerPanel,sdlg);
          sdlg.setVisible(true);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_TESTADMIN:
          testDevice("dserver/" + server + "/" + instance);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_SAVESERVER:

          FileWriter resFile;

          JFileChooser chooser = new JFileChooser(".");
          ok = JOptionPane.YES_OPTION;
          if (lastFile != null)
            chooser.setSelectedFile(lastFile);

          int returnVal = chooser.showSaveDialog(invoker);

          if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastFile = chooser.getSelectedFile();
            if (lastFile != null) {
              if (lastFile.exists()) ok = JOptionPane.showConfirmDialog(invoker, "Do you want to overwrite " + lastFile.getName() + " ?", "Confirm overwrite", JOptionPane.YES_NO_OPTION);
              if (ok == JOptionPane.YES_OPTION) {
                try {
                  resFile = new FileWriter(lastFile.getAbsolutePath());
                  Date date = new Date(System.currentTimeMillis());
                  resFile.write("#\n# Resource backup , created " + date + "\n#\n\n");
                  saveServerData(resFile,server+"/"+instance);
                  resFile.close();
                } catch (IOException e) {
                  JiveUtils.showJiveError("Failed to create resource file !\n" + e.getMessage());
                }
              }
            }
          }

          break;

        // ----------------------------------------------------------------------------
        case TreePanel.ACTION_CLASSWIZ:

          DevWizard cwdlg = new DevWizard(invoker);
          cwdlg.showClassesWizard(server + "/" + instance);
          refresh();
          break;

        // ----------------------------------------------------------------------------
        case TreePanel.ACTION_UNEXPORT:

          String srvName = server + "/" + instance;
          ok = JOptionPane.showConfirmDialog(invoker, "This will unexport all devices of " + srvName + "\n Do you want to continue ?", "Confirm unexport device", JOptionPane.YES_NO_OPTION);
          if (ok == JOptionPane.YES_OPTION) {
            try {
              //System.out.println(" Unexport device of" + srvName);
              db.unexport_server(srvName);
            } catch (DevFailed e) {
              JiveUtils.showTangoError(e);
            }
          }

          break;

        // ----------------------------------------------------------------------------
        case ACTION_DEV_DEPEND:
          launchDevDepend(server + "/" + instance);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_THREAD_POLL:
          launchPollingThreadsManager(server + "/" + instance);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_MOVE_SERVER:

          // Rename a server
          String newSName = JOptionPane.showInputDialog(null,"Rename server",server+"/"+instance);
          if(newSName==null) return;
          try {
            db.rename_server(server+"/"+instance,newSName);
            refresh();
            selectFullServer(newSName);
          } catch (DevFailed e) {
            JiveUtils.showTangoError(e);
          }
          break;

      }

    }

    // IServerAction (Call by ServerDlg)
    public void doJob(String server, String classname, String[] devices) {

      // Add devices
      try {
        for (int i = 0; i < devices.length; i++)
          db.add_device(devices[i], classname, server);
      } catch (DevFailed e) {
        JiveUtils.showTangoError(e);
      }

      refresh();
      selectClass(classname,server);

    }



  }

  // ---------------------------------------------------------------

  class ClassNode extends TangoNode implements IServerAction {

    private String server;
    private String instance;
    private String className;
    private String[] devList = new String[0];

    ClassNode(String server,String instance,String className) {
      this.server = server;
      this.instance = instance;
      this.className = className;
      try {
        devList = db.get_device_name(server+"/"+instance , className);
      } catch (DevFailed e) {}
    }

    void populateNode() throws DevFailed {
      for (int i = 0; i < devList.length; i++)
        add(new DeviceServerNode(server,instance,className,devList[i]));
    }

    public boolean isLeaf() {
      return devList.length == 0;
    }

    ImageIcon getIcon() {
      if(devList.length==0)
        return TangoNodeRenderer.uclassicon;
      else
        return TangoNodeRenderer.classicon;
    }

    public String toString() {
      return className;
    }

    int[] getAction() {
      if(JiveUtils.readOnly)
        return new int[0];
      else
        return new int[]{ACTION_RENAME,
                         ACTION_DELETE,
                         ACTION_ADDDEVICE,
                         ACTION_DEVICESWIZ
        };
    }

    void execAction(int actionNumber) {

      switch(actionNumber) {

        // ----------------------------------------------------------------------------
        case ACTION_RENAME:

          // Rename a class
          String newName = JOptionPane.showInputDialog(null,"Rename class",className);
          if(newName==null) return;
          if(searchNode((TangoNode)getParent(),newName)!=null) {
            JiveUtils.showJiveError("Name already exists.");
            return;
          }

          for(int i=0;i<getChildCount();i++) {
            // Devices
            TangoNode n0 = (TangoNode)getChildAt(i);
            try {
              db.add_device(n0.toString(),newName,server+"/"+instance);
            } catch (DevFailed e) {
              JiveUtils.showTangoError(e);
            }
          }

          refresh();
          selectClass(newName,server+"/"+instance);

          break;

        // ----------------------------------------------------------------------------
        case ACTION_DELETE:

          int ok = JOptionPane.showConfirmDialog(invoker, "Delete class " + className + " ?", "Confirm delete", JOptionPane.YES_NO_OPTION);
          if (ok == JOptionPane.YES_OPTION) {

            for(int i=0;i<getChildCount();i++) {
              // Devices
              TangoNode n0 = (TangoNode)getChildAt(i);
              try {
                db.delete_device(n0.toString());
              } catch (DevFailed e) {
                JiveUtils.showTangoError(e);
              }
            }

            refresh();
            selectFullServer(server + "/" + instance);

          }
          break;

        // ----------------------------------------------------------------------------
        case ACTION_ADDDEVICE:

          ServerDlg sdlg = new ServerDlg(this);
          sdlg.setValidFields(false, false);
          sdlg.setDefaults(server + "/" + instance, className);
          ATKGraphicsUtils.centerFrame(invoker.innerPanel, sdlg);
          sdlg.setVisible(true);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_DEVICESWIZ:
          DevWizard dswdlg = new DevWizard(invoker);
          dswdlg.showDevicesWizard(server + "/" + instance,className);
          refresh();
          break;

      }

    }

    // IServerAction (Call by ServerDlg)
    public void doJob(String server, String classname, String[] devices) {

      // Add devices
      try {
        for (int i = 0; i < devices.length; i++)
          db.add_device(devices[i], classname, server);
      } catch (DevFailed e) {
        JiveUtils.showTangoError(e);
      }

      refresh();
      selectClass(classname,server);

    }

  }

  // ---------------------------------------------------------------

  class AttributeNode extends TangoNode {

    private String devName;

    AttributeNode(String devName) {
      this.devName = devName;
    }

    void populateNode() throws DevFailed {

      String[] list = new String[0];
      String[] devList = new String[0];
      String[] dbList = new String[0];
      int idl = 0; // 0 means that no property will be considered as attribute config.
      // In other terms , that means that if the device doesn't run , all
      // attribute properties will appear in the attribute property node.
      DeviceProxy ds = new DeviceProxy(devName);

      try {
        devList = ds.get_attribute_list();
        idl = ds.get_idl_version();
      } catch( DevFailed e) {
      }
      dbList = db.get_device_attribute_list(devName);

      JiveUtils.sortList(list);
      for(int i=0;i<devList.length;i++)
        add(new TaskDeviceAttributePropertyNode(self,db,devName,devList[i],idl,false));
      for(int i=0;i<dbList.length;i++)
        if(!JiveUtils.contains(devList,dbList[i]))
          add(new TaskDeviceAttributePropertyNode(self,db,devName,dbList[i],idl,true));

    }

    public int[] getAction() {
      return new int[] {
          TreePanel.ACTION_COPY,
          TreePanel.ACTION_PASTE,
          TreePanel.ACTION_CREATE_ATTPROP
      };
    }

    public void execAction(int actionNumber) {
      switch(actionNumber) {

        case TreePanel.ACTION_CREATE_ATTPROP:
          createEmptyAttributeProperty(devName);
          break;

        case TreePanel.ACTION_COPY:
          // Copy all attribute property to the clipboard
          int nbAtt = getChildCount();
          JiveUtils.the_clipboard.clear();
          for(int i=0;i<nbAtt;i++) {
            TaskDeviceAttributePropertyNode node = (TaskDeviceAttributePropertyNode)getChildAt(i);
            String[][] props = node.getProperties();
            for(int j=0;j<props.length;j++)
              JiveUtils.the_clipboard.add(props[j][0],node.getAttributeName(),props[j][1]);
          }
          break;

        case TreePanel.ACTION_PASTE:
          for(int i=0;i<JiveUtils.the_clipboard.getAttPropertyLength();i++) {
            putAttributeProperty( devName,
                JiveUtils.the_clipboard.getAttName(i),
                JiveUtils.the_clipboard.getAttPropertyName(i),
                JiveUtils.the_clipboard.getAttPropertyValue(i));
          }
          break;

      }
    }

    public String toString() {
      return "Attribute properties";
    }

    public ImageIcon getIcon() {
      return TangoNodeRenderer.atticon;
    }

  }

  // ---------------------------------------------------------------

  class DeviceServerNode extends TangoNode {

    private String server;
    private String instance;
    private String className;
    private String    devName;

    DeviceServerNode(String server, String instance, String className, String devName) {
      this.server = server;
      this.instance = instance;
      this.className = className;
      this.devName = devName;
    }

    void populateNode() throws DevFailed {
      add(new TaskDevicePropertyNode(self,db,devName));
      add(new TaskPollingNode(db,devName));
      add(new TaskEventNode(db,devName));
      add(new TaskAttributeNode(db,devName));
      add(new TaskPipeNode(db,devName));
      add(new AttributeNode(devName));
      add(new TaskLoggingNode(db,devName));
    }

    public String toString() {
      return devName;
    }

    ImageIcon getIcon() {
      return TangoNodeRenderer.devicon;
    }

    String getValue() {
      return getDeviceInfo(devName);
    }

    String getTitle() {
      return "Device Info";
    }

    int[] getAction() {
      if(JiveUtils.readOnly)
        return new int[]{ACTION_MONITORDEV,
                         ACTION_TESTDEV,
                         ACTION_GOTODEVNODE
        };
      else
        return new int[]{ACTION_COPY,
                         ACTION_PASTE,
                         ACTION_RENAME,
                         ACTION_DELETE,
                         ACTION_MONITORDEV,
                         ACTION_TESTDEV,
                         ACTION_DEFALIAS,
                         ACTION_GOTODEVNODE,
                         ACTION_RESTART,
                         ACTION_DEVICEWIZ,
                         ACTION_LOG_VIEWER
        };
    }

    void execAction(int actionNumber) {

      switch(actionNumber) {

        // ----------------------------------------------------------------------------
        case ACTION_COPY:
          JiveUtils.copyDeviceProperties(db,devName);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_PASTE:
          pasteDeviceProperty(db,devName);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_RENAME:
          String newName = JOptionPane.showInputDialog(null,"Rename device",devName);
          if(newName==null) return;
          if( renameDevice(newName) ) {
            refresh();
            selectDevice(className,server + "/" + instance,newName);
          }
          break;

        // ----------------------------------------------------------------------------
        case ACTION_DELETE:
          int ok = JOptionPane.showConfirmDialog(invoker, "Delete device " + devName + " ?", "Confirm delete", JOptionPane.YES_NO_OPTION);
          if (ok == JOptionPane.YES_OPTION) {
            try {
              db.delete_device(devName);
            } catch (DevFailed e) {
              JiveUtils.showTangoError(e);
            }
            refresh();
          }
          break;

        // ----------------------------------------------------------------------------
        case ACTION_MONITORDEV:
          new atkpanel.MainPanel(devName, false, true, !JiveUtils.readOnly);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_TESTDEV:
          testDevice(devName);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_DEFALIAS:
          String alias = JOptionPane.showInputDialog(null,"Define device alias","");
          if(alias==null) return;
          try {
            db.put_device_alias(devName, alias);
          } catch (DevFailed e) {
            JiveUtils.showTangoError(e);
          }
          break;

        // ----------------------------------------------------------------------------
        case ACTION_GOTODEVNODE:
          invoker.goToDeviceNode(devName);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_RESTART:
          try {
            DeviceProxy ds = new DeviceProxy("dserver/" + server + "/" + instance);
            DeviceData in = new DeviceData();
            in.insert(devName);
            ds.command_inout("DevRestart", in);
          } catch (DevFailed e) {
            JiveUtils.showTangoError(e);
          }
          break;

        // ----------------------------------------------------------------------------
        case ACTION_DEVICEWIZ:
          DevWizard dwdlg = new DevWizard(invoker);
          dwdlg.showDeviceWizard(server + "/" + instance , className , devName);
          break;

        // ----------------------------------------------------------------------------
        case ACTION_LOG_VIEWER:
          launchLogViewer(devName);
          break;

      }

    }

    boolean renameDevice(String nDevName) {

      boolean isAlive = false;
      boolean success = false;

      try {

        // Check if the device exixts
        DbDevImportInfo ii = db.import_device(nDevName);
        JiveUtils.showJiveError("The device " + nDevName + " already exits.\nServer: " + ii.server);

      } catch (DevFailed e1) {

        // try to create the new device
        try {

          db.add_device(nDevName,className,server + "/" + instance);

          // The new device exists
          success = true;

          DeviceProxy ds = null;
          try {
            ds = new DeviceProxy(devName);
            ds.ping();
            isAlive=true;
          } catch (DevFailed e2) {}

          int ok = JOptionPane.showConfirmDialog(invoker, "Do you want to copy propeties of " + devName + " to " + nDevName + " ?", "Confirm propety move", JOptionPane.YES_NO_OPTION);
          if (ok == JOptionPane.YES_OPTION) {

            // Clone device properties
            String[] propList = db.get_device_property_list(devName,"*");
            if (propList.length > 0) {
              DbDatum[] data = db.get_device_property(devName, propList);
              db.put_device_property(nDevName, data);
            }

            // Clone attributes propeties
            try {

              String[] attList = db.get_device_attribute_list(devName);

              if (attList.length > 0) {
                DbAttribute[] adata = db.get_device_attribute_property(devName, attList);
                db.put_device_attribute_property(nDevName, adata);
              }

            } catch (DevFailed e3) {
              JiveUtils.showJiveError("Failed to copy attribute properties of " + devName + "\n" + e3.errors[0].desc);
            }

          }

          // Remove the old device
          if(isAlive)
            JiveUtils.showJiveWarning("The old device " + devName + " is still alive and should be removed by hand.");
          else
            db.delete_device(devName);

        } catch (DevFailed e4) {
          JiveUtils.showTangoError(e4);
        }

      }

      return success;

    }

  }

}
