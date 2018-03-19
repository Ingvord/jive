package jive3;

import jive.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import java.util.prefs.Preferences;

import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.Database;
import fr.esrf.Tango.DevFailed;
import fr.esrf.tangoatk.widget.util.ATKConstant;
import fr.esrf.tangoatk.widget.util.ATKGraphicsUtils;

public class MainPanel extends JFrame implements ChangeListener,NavigationListener,IServerAction {

  private Database db;

  JTabbedPane       treePane;
  JSplitPane        splitPane;
  JPanel            lockPanel;
  int               panelMask;

  // Trees
  class TreePanelRecord {

    String    name;
    String    tabName;
    TreePanel treePanel;
    boolean   display;

    TreePanelRecord(TreePanel t,Database db,String tabName,String name,String treeName) {
      t.setDatabase(db);
      this.treePanel = t;
      this.tabName = tabName;
      this.name = name;
      this.treePanel.tree.setName(treeName);
      this.display = true;
    }

  }

  ArrayList<TreePanelRecord> treePanels;
  int nbPanels;

  // Right panels
  DefaultPanel         defaultPanel;
  PropertyPanel        propertyPanel;
  DevicePollingPanel   devicePollingPanel;
  DeviceEventPanel     deviceEventPanel;
  DeviceAttributePanel deviceAttributePanel;
  DevicePipePanel      devicePipePanel;
  DeviceLoggingPanel   deviceLoggingPanel;
  SingleAttributePanel singleAttributePanel;

  // History panel
  PropertyHistoryDlg   historyDlg;

  // Multiple selecection panel
  SelectionDlg   selectionDlg;

  // Filter dialog
  FilterDlg filterDlg=null;

  // Navigation stuff
  private NavManager    navManager;
  private NavigationBar navBar;
  private boolean       recordPos;

  //Search stuff
  SearchEngine          searchEngine;

  // Inner panel
  JPanel innerPanel;

  private String lastResOpenedDir = ".";

  private boolean running_from_shell;

  // User settings
  Preferences prefs;
  private String[] knownTangoHost;
  private String THID = "TangoHost";

  // Relase number
  public static final String DEFAULT_VERSION = "-.-";
  public static final String VERSION = getVersion();

  // General constructor
  public MainPanel() {
    this(false,false);
  }

  /**
   * Construct a Jive application.
   * @param runningFromShell True if running from shell. If true , Jive calls System.exit().
   * @param readOnly Read only flag.
   */
  public MainPanel(boolean runningFromShell,boolean readOnly) {
    this(runningFromShell, readOnly, 126);
  }

  public MainPanel(boolean runningFromShell,boolean readOnly,int panelMask) {

    this.panelMask = panelMask;

    // Get user settings
    prefs = Preferences.userRoot().node(this.getClass().getName());
    knownTangoHost = JiveUtils.makeStringArray(prefs.get(THID,""));
    if(knownTangoHost.length==1)
      if(knownTangoHost[0].equals(""))
        knownTangoHost = new String[0];

    running_from_shell = runningFromShell;
    JiveUtils.readOnly = readOnly;
    initComponents();
    centerWindow();
    setVisible(true);
    JiveUtils.parent = this;
    navManager = new NavManager(this);
    searchEngine = new SearchEngine(this);
    recordPos = true;

  }

  // Init componenet
  private void initComponents() {

    innerPanel = new JPanel();
    innerPanel.setLayout(new BorderLayout());

    MultiLineToolTipUI.initialize();

    // *************************************************************
    // Initialise the Tango database
    // *************************************************************
    String tangoHost = null;

    try {
      tangoHost = ApiUtil.getTangoHost();
    } catch ( DevFailed e ) {
      System.out.println("TANGO_HOST no defined, exiting...");
      exitForm();
    }

    try {
      db = ApiUtil.get_db_obj();
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
      db = null;
    }

    updateTitle(tangoHost);

    // *************************************************************
    // Create widget
    // *************************************************************
    defaultPanel = new DefaultPanel();
    propertyPanel = new PropertyPanel();
    propertyPanel.setParent(this);
    devicePollingPanel = new DevicePollingPanel();
    deviceEventPanel = new DeviceEventPanel();
    deviceAttributePanel = new DeviceAttributePanel();
    devicePipePanel = new DevicePipePanel();
    deviceLoggingPanel = new DeviceLoggingPanel();
    singleAttributePanel = new SingleAttributePanel();

    splitPane = new JSplitPane();
    splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

    treePanels = new ArrayList<TreePanelRecord>();

    if(isCollectionPanelVisible())
      treePanels.add( new TreePanelRecord(new TreePanelHostCollection(this),db,"Collection","Collection:","COLLECTION") );
    if(isServerPanelVisible())
      treePanels.add( new TreePanelRecord(new TreePanelServer(this),db,"Server","Server:","SERVER") );
    if(isDevicePanelVisible())
      treePanels.add( new TreePanelRecord(new TreePanelDevice(this),db,"Device","Device:","DEVICE") );
    if(isClassPanelVisible())
      treePanels.add( new TreePanelRecord(new TreePanelClass(this),db,"Class","Class:","CLASS") );
    if(isDevAliasPanelVisible())
      treePanels.add( new TreePanelRecord(new TreePanelAlias(this),db,"Alias","Alias:","DEV-ALIAS") );
    if(isAttAliasPanelVisible())
      treePanels.add( new TreePanelRecord(new TreePanelAttributeAlias(this),db,"Att. Alias","AttAlias:","ATT-ALIAS") );
    if(isFreePropertyPanelVisible())
      treePanels.add( new TreePanelRecord(new TreePanelFreeProperty(this),db,"Property","FreeProperty:","PROPERTY") );

    treePane = new JTabbedPane();
    treePane.setFont(ATKConstant.labelFont);
    treePane.addChangeListener(this);
    splitPane.setLeftComponent(treePane);
    nbPanels = treePanels.size();
    for(int i=0;i<nbPanels;i++)
      treePane.add(treePanels.get(i).tabName,treePanels.get(i).treePanel);

    int minWidth = 5;
    for(int i=0;i<nbPanels;i++) {
      Dimension d = ATKGraphicsUtils.measureString(treePanels.get(i).tabName,ATKConstant.labelFont);
      minWidth += 24 + d.width;
    }
    if(minWidth<250) minWidth = 250;
    treePane.setMinimumSize(new Dimension(minWidth, 0));

    historyDlg = new PropertyHistoryDlg();
    historyDlg.setDatabase(db, tangoHost);
    selectionDlg = new SelectionDlg();
    selectionDlg.setDatabase(db);
    innerPanel.add(splitPane, BorderLayout.CENTER);
    splitPane.setRightComponent(defaultPanel);

    navBar = new NavigationBar();
    navBar.enableBack(false);
    navBar.enableForward(false);
    navBar.enableNextOcc(false);
    navBar.enablePreviousOcc(false);
    navBar.addNavigationListener(this);

    if( JiveUtils.readOnly ) {

      JPanel upPanel = new JPanel();
      upPanel.setLayout(new BorderLayout());

      lockPanel = new JPanel();
      lockPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
      lockPanel.setBackground(new Color(233,233,233));
      JLabel lockIcon = new JLabel();
      lockIcon.setIcon(new ImageIcon(getClass().getResource("/jive/lock.gif")));
      lockPanel.add(lockIcon);
      JLabel lockLabel = new JLabel("Read only mode (No write access to database allowed)");
      lockLabel.setFont(ATKConstant.labelFont);
      lockPanel.add(lockLabel);
      upPanel.add(lockPanel,BorderLayout.NORTH);
      upPanel.add(navBar,BorderLayout.SOUTH);
      innerPanel.add(upPanel, BorderLayout.NORTH);

    } else {

      innerPanel.add(navBar, BorderLayout.NORTH);

    }

    //**************************************************************
    // Menu bar
    //**************************************************************
    JMenuBar mainMenu = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    JMenuItem loadFile = new JMenuItem("Load property file");
    loadFile.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        loadPropFile();
      }
    });
    loadFile.setEnabled(!JiveUtils.readOnly);
    JMenuItem checkFile = new JMenuItem("Check property file");
    checkFile.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        checkPropFile();
      }
    });
    JSeparator sep1 = new JSeparator();
    JMenuItem exit = new JMenuItem("Exit");
    exit.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        exitForm();
      }
    });

    fileMenu.add(loadFile);
    fileMenu.add(checkFile);
    fileMenu.add(sep1);
    fileMenu.add(exit);
    mainMenu.add(fileMenu);

    JMenu editMenu = new JMenu("Edit");
    JMenuItem refresh = new JMenuItem("Refresh Tree");
    refresh.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        refreshTree();
      }
    });
    refresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
    editMenu.add(refresh);

    editMenu.add(new JSeparator());

    if( running_from_shell ) {

      JMenuItem chTangoHost = new JMenuItem("Change Tango Host");
      chTangoHost.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        changeTangoHost();
      }
    });
      editMenu.add(chTangoHost);

      editMenu.add(new JSeparator());

    }

    JMenuItem createServer = new JMenuItem("Create server");
    createServer.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        createServer();
      }
    });
    createServer.setEnabled(!JiveUtils.readOnly);
    editMenu.add(createServer);

    JMenuItem createFreeProperty = new JMenuItem("Create free property");
    createFreeProperty.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        createFreeProperty();
      }
    });
    createFreeProperty.setEnabled(!JiveUtils.readOnly);
    editMenu.add(createFreeProperty);

    editMenu.add(new JSeparator());

    JMenuItem showClipboard = new JMenuItem("Show clipboard");
    showClipboard.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        showClipboard();
      }
    });
    editMenu.add(showClipboard);

    JMenuItem clearClipboard = new JMenuItem("Clear clipboard");
    clearClipboard.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        JiveUtils.the_clipboard.clear();
      }
    });
    editMenu.add(clearClipboard);

    editMenu.add(new JSeparator());

    final JCheckBoxMenuItem showSystemProperty = new JCheckBoxMenuItem("Show system property");
    showSystemProperty.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        JiveUtils.showSystemProperty = showSystemProperty.isSelected();
        refreshTree();
      }
    });
    editMenu.add(showSystemProperty);

    JMenu serverMenu = new JMenu("Tools");
    JMenuItem createServerWz = new JMenuItem("Server Wizard");
    createServerWz.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        createServerWz();
      }
    });
    createServerWz.setEnabled(!JiveUtils.readOnly);
    serverMenu.add(createServerWz);
    JMenuItem dbInfoMenu = new JMenuItem("Database Info");
    dbInfoMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showDatabaseInfo();
      }
    });
    serverMenu.add(dbInfoMenu);
    JMenuItem dbHistMenu = new JMenuItem("Database history");
    dbHistMenu.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        showHistory();
      }
    });
    serverMenu.add(dbHistMenu);
    JMenuItem selectionMenu = new JMenuItem("Multiple selection");
    selectionMenu.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        showMultipleSelection();
      }
    });
    serverMenu.add(selectionMenu);

    JMenu filterMenu = new JMenu("Filter");
    JMenuItem filterServer = new JMenuItem("Server");
    filterServer.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        filterServer();
      }
    });
    filterMenu.add(filterServer);
    JMenuItem filterDevice = new JMenuItem("Device");
    filterDevice.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        filterDevice();
      }
    });
    filterMenu.add(filterDevice);
    JMenuItem filterClass = new JMenuItem("Class");
    filterClass.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        filterClass();
      }
    });
    filterMenu.add(filterClass);
    JMenuItem filterAlias = new JMenuItem("Alias");
    filterAlias.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        filterAlias();
      }
    });
    filterMenu.add(filterAlias);
    JMenuItem filterAttributeAlias = new JMenuItem("Att. Alias");
    filterAttributeAlias.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        filterAttributeAlias();
      }
    });
    filterMenu.add(filterAttributeAlias);
    JMenuItem filterProperty = new JMenuItem("Property");
    filterProperty.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
        filterProperty();
      }
    });
    filterMenu.add(filterProperty);

    mainMenu.add(fileMenu);
    mainMenu.add(editMenu);
    mainMenu.add(serverMenu);
    mainMenu.add(filterMenu);
    setJMenuBar(mainMenu);

    //**************************************************************
    // Component listener
    //**************************************************************
    addComponentListener(new ComponentListener() {

      public void componentHidden(ComponentEvent e) {
        exitForm();
      }

      public void componentMoved(ComponentEvent e) {
      }

      public void componentResized(ComponentEvent e) {
      }

      public void componentShown(ComponentEvent e) {
      }

    });

    ImageIcon icon = new ImageIcon(getClass().getResource("/jive/jive.png"));
    setIconImage(icon.getImage());
    setContentPane(innerPanel);

  }

  public boolean isCollectionPanelVisible() {
    return (panelMask & 0x1) != 0;
  }
  public boolean isServerPanelVisible() {
    return (panelMask & 0x2) != 0;
  }
  public boolean isDevicePanelVisible() {
    return (panelMask & 0x4) != 0;
  }
  public boolean isClassPanelVisible() {
    return (panelMask & 0x8) != 0;
  }
  public boolean isDevAliasPanelVisible() {
    return (panelMask & 0x10) != 0;
  }
  public boolean isAttAliasPanelVisible() {
    return (panelMask & 0x20) != 0;
  }
  public boolean isFreePropertyPanelVisible() {
    return (panelMask & 0x40) != 0;
  }

  //**************************************************************
  // Navigation listener
  //**************************************************************
  private void reselect() {

    TreePath path = navManager.getCurrentPath();
    JTree tree = navManager.getCurrentTree();

    recordPos = false;

    for(int i=0;i<nbPanels;i++)
      if(tree==treePanels.get(i).treePanel.tree)
        treePane.setSelectedComponent(treePanels.get(i).treePanel);

    // Work around X11 bug
    treePane.getSelectedComponent().setVisible(true);

    tree.setSelectionPath(path);
    recordPos = true;
    tree.scrollPathToVisible(path);

  }

  public void backAction(NavigationBar src) {
    navManager.goBack();
    navBar.enableBack(navManager.canGoBackward());
    navBar.enableForward(navManager.canGoForward());
    reselect();
  }
  public void forwardAction(NavigationBar src) {
    navManager.goForward();
    navBar.enableBack(navManager.canGoBackward());
    navBar.enableForward(navManager.canGoForward());
    reselect();
  }

  public void nextOccAction(NavigationBar src) {

    TreePath path = searchEngine.findNext();
    TreePanel selected = (TreePanel)treePane.getSelectedComponent();
    if(path!=null) {
      selected.tree.setSelectionPath(path);
      selected.tree.scrollPathToVisible(path);
    }

    navBar.enableNextOcc(!searchEngine.isStackEmpty());

  }

  public void previousOccAction(NavigationBar src) {

  }

  public void searchAction(NavigationBar src,TreePath pathToSelect) {

    // Check if we have a link
    if(pathToSelect!=null) {
      String treeName = pathToSelect.getPathComponent(0).toString();

      for(int i=0;i<nbPanels;i++)
        if( treePanels.get(i).name.equals(treeName) ) {
          treePanels.get(i).treePanel.selectPath(pathToSelect);
          treePane.setSelectedComponent(treePanels.get(i).treePanel);
          treePane.getSelectedComponent().setVisible(true);
        }

      resetSearch();
      return;
    }

    // Search
    String searchText = src.getSearchText();

    for(int i=0;i<nbPanels;i++)
      if( searchText.startsWith(treePanels.get(i).name) ) {
        searchText = searchText.substring(treePanels.get(i).name.length());
        treePane.setSelectedComponent(treePanels.get(i).treePanel);
      }

    treePane.getSelectedComponent().setVisible(true);
    TreePanel selected = (TreePanel)treePane.getSelectedComponent();
    String[] fieldnames = searchText.split("/");

    // Fast one field search
    if( fieldnames.length==1 ) {
      // One field name given
      if( selected.isRootItem(fieldnames[0]) ) {
        TreePath path = selected.selectRootItem(fieldnames[0]);
        selected.tree.scrollPathToVisible(path);
        TangoNode focusedNode = (TangoNode)path.getLastPathComponent();
        searchEngine.setSearchText(searchText);
        resetSearch(focusedNode);
        return;
      }
    }

    // Fast device search
    if( JiveUtils.isDeviceName(searchText) ) {
      if( searchText.startsWith("tango:") )
        searchText = searchText.substring(6);

      if( getDeviceTreePanel().isDomain(fieldnames[0])) {
        TangoNode focusedNode = goToDeviceFullNode(searchText);
        if(focusedNode!=null) {
          searchEngine.setSearchText(searchText);
          resetSearch(focusedNode);
          return;
        }
      }
    }

    // Fast server search
    if( JiveUtils.isFullServerName(searchText) ) {

      if( getServerTreePanel().isServer(fieldnames[0]) ) {
        TangoNode focusedNode = goToServerFullNode(searchText);
        if( focusedNode == null ) {
          // Try to go to server root
          focusedNode = goToServerRootNode(fieldnames[0]);
          if( focusedNode!=null ) {
            searchEngine.setSearchText(searchText);
            resetSearch(focusedNode);
            return;
          }
        } else {
          searchEngine.setSearchText(searchText);
          resetSearch(focusedNode);
          return;
        }
      }
    }

    // Default search
    TreePath path = searchEngine.findText(searchText,selected.root);
    if(path!=null) {
      selected.tree.setSelectionPath(path);
      selected.tree.scrollPathToVisible(path);
    }

    navBar.enableNextOcc(!searchEngine.isStackEmpty());

  }
  public void refreshAction(NavigationBar src) {
    refreshTree();
  }

  // Show the clipboard content
  public void showClipboard() {
    JiveUtils.the_clipboard.show(this);
  }

  // Create a free property object
  private void createFreeProperty() {

    String newProp = JOptionPane.showInputDialog(this, "Enter property object name", "Jive", JOptionPane.QUESTION_MESSAGE);
    if (newProp != null) {
      TreePanelFreeProperty propertyTreePanel = getPropertyTreePanel();
      propertyTreePanel.addProperty(newProp);
      treePane.setSelectedComponent(propertyTreePanel);
      propertyTreePanel.setVisible(true);
    }

  }

  private void resetSearch() {
    resetSearch(null);
  }

  private void resetSearch(TangoNode focusedNode) {

    searchEngine.resetSearch(focusedNode);
    navBar.enableNextOcc(focusedNode!=null);
    navBar.enablePreviousOcc(false);

  }

  // Create a server
  public void doJob(String server,String classname,String[] devices) {

    // Add devices
    try {

      // Check that device is not already existing
      Vector exDevices = new Vector();
      for (int i = 0; i < devices.length; i++) {
        try {
          db.import_device(devices[i]);
        } catch(DevFailed e) {
          continue;
        }
        exDevices.add(devices[i]);
      }

      if(exDevices.size()>0) {
        String message = "Warning, following device(s) already declared:\n";
        for(int i=0;i<exDevices.size();i++)
          message += "   " + exDevices.get(i) + "\n";
        message += "Do you want to continue ?";

        if( JOptionPane.showConfirmDialog(this,message,"Confirmation",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION )
          return;
      }

      for (int i = 0; i < devices.length; i++) {
        db.add_device(devices[i], classname, server);
      }

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

    refreshTree();
    getServerTreePanel().selectFullServer(server);

  }

  private void createServer() {

    ServerDlg sdlg = new ServerDlg(this);
    sdlg.setValidFields(true, true);
    sdlg.setDefaults("", "");
    sdlg.setServerList( getServerTreePanel().getServerList() );
    sdlg.setClassList( getClassTreePanel().getClassList() );
    ATKGraphicsUtils.centerFrame(innerPanel,sdlg);
    sdlg.setVisible(true);

  }

  // Filter server
  private void filterServer() {

    if(filterDlg==null) filterDlg = new FilterDlg(this);
    filterDlg.setLabelName("Server filter");
    filterDlg.setFilter(getServerTreePanel().getFilter());
    if( filterDlg.showDialog() ) {
      // Apply filter
      filterServer(filterDlg.getFilterText());
    }

  }

  public void filterServer(String filter) {
    TreePanelServer serverTreePanel = getServerTreePanel();
    serverTreePanel.applyFilter(filter);
    serverTreePanel.refresh();
    resetSearch();
    serverTreePanel.revalidate();
  }

  // Filter device
  private void filterDevice() {

    if(filterDlg==null) filterDlg = new FilterDlg(this);
    filterDlg.setLabelName("Device filter");
    filterDlg.setFilter(getDeviceTreePanel().getFilter());
    if( filterDlg.showDialog() ) {
      // Apply filter
      filterDevice(filterDlg.getFilterText());
    }

  }

  public void filterDevice(String filter) {
    TreePanelDevice deviceTreePanel = getDeviceTreePanel();
    deviceTreePanel.applyFilter(filter);
    deviceTreePanel.refresh();
    resetSearch();
    deviceTreePanel.revalidate();
  }

  // Filter class
  private void filterClass() {

    if(filterDlg==null) filterDlg = new FilterDlg(this);
    filterDlg.setLabelName("Class filter");
    filterDlg.setFilter(getClassTreePanel().getFilter());
    if( filterDlg.showDialog() ) {
      // Apply filter
      filterClass(filterDlg.getFilterText());
    }

  }

  public void filterClass(String filter) {
    TreePanelClass classTreePanel = getClassTreePanel();
    classTreePanel.applyFilter(filter);
    classTreePanel.refresh();
    resetSearch();
    classTreePanel.revalidate();
  }

  // Filter alias
  private void filterAlias() {

    if(filterDlg==null) filterDlg = new FilterDlg(this);
    filterDlg.setLabelName("Alias filter");
    filterDlg.setFilter(getAliasTreePanel().getFilter());
    if( filterDlg.showDialog() ) {
      // Apply filter
      filterAlias(filterDlg.getFilterText());
    }

  }

  public void filterAlias(String filter) {
    TreePanelAlias aliasTreePanel = getAliasTreePanel();
    aliasTreePanel.applyFilter(filter);
    aliasTreePanel.refresh();
    resetSearch();
    aliasTreePanel.revalidate();
  }

  // Filter attribute alias
  private void filterAttributeAlias() {

    if(filterDlg==null) filterDlg = new FilterDlg(this);
    filterDlg.setLabelName("Att. Alias filter");
    filterDlg.setFilter(getAttributeAliasTreePanel().getFilter());
    if( filterDlg.showDialog() ) {
      // Apply filter
      filterAttributeAlias(filterDlg.getFilterText());
    }

  }

  public void filterAttributeAlias(String filter) {
    TreePanelAttributeAlias attributeAliasTreePanel = getAttributeAliasTreePanel();
    attributeAliasTreePanel.applyFilter(filter);
    attributeAliasTreePanel.refresh();
    resetSearch();
    attributeAliasTreePanel.revalidate();
  }

  // Filter property
  private void filterProperty() {

    if(filterDlg==null) filterDlg = new FilterDlg(this);
    filterDlg.setLabelName("Property filter");
    filterDlg.setFilter(getPropertyTreePanel().getFilter());
    if( filterDlg.showDialog() ) {
      // Apply filter
      filterProperty(filterDlg.getFilterText());
    }

  }

  public void filterProperty(String filter) {
    TreePanelFreeProperty propertyTreePanel = getPropertyTreePanel();
    propertyTreePanel.applyFilter(filterDlg.getFilterText());
    propertyTreePanel.refresh();
    resetSearch();
    propertyTreePanel.revalidate();
  }

  // Create a server using the wizard
  private void createServerWz() {

    DevWizard wdlg = new DevWizard(this);
    wdlg.showWizard(null);
    refreshTree();

  }

  // Update the title bar
  private void updateTitle(String tangoHost) {

    String title = new String("Jive " + VERSION);
    if (JiveUtils.readOnly) {
        title += "(Read Only)";
    }
    if(db==null)
      setTitle(title + " [No connection]");
    else
      setTitle(title + " [" + tangoHost + "]");

  }

  private boolean isKnowTangoHost(String th) {

    boolean found = false;
    int i = 0;
    while(!found&&i<knownTangoHost.length) {
      found = knownTangoHost[i].equalsIgnoreCase(th);
      if(!found) i++;
    }
    return found;

  }

  // Change the TANGO HOST
  private void changeTangoHost() {

    TangoHostDlg dlg = new TangoHostDlg(this,knownTangoHost);
    String th = dlg.getTangoHost();

    if (th != null) {

      String[] ths = th.split(":");

      if( ths.length!=2 ) {
        JiveUtils.showJiveError("Invalid tango host syntax: should be host:port");
        return;
      }

      try {
        Integer.parseInt(ths[1]);
      } catch (NumberFormatException e) {
        JiveUtils.showJiveError("Invalid tango host port number\n" + e.getMessage());
        return;
      }

      try {

        db = ApiUtil.change_db_obj(ths[0],ths[1]);

        // Add this host the to list (if needed) and save to pref

        if( !isKnowTangoHost(th) ) {

          String[] newTH = new String[knownTangoHost.length+1];
          for(int i=0;i<knownTangoHost.length;i++) {
            newTH[i]=knownTangoHost[i];
          }
          newTH[knownTangoHost.length] = th;
          knownTangoHost = newTH;
          JiveUtils.sortList(knownTangoHost);
          prefs.put(THID,JiveUtils.stringArrayToString(knownTangoHost));

        }

      } catch (DevFailed e) {
        JiveUtils.showTangoError(e);
        db = null;
      } catch (Exception e) {
        JiveUtils.showJiveError(e.getMessage());
        db=null;
      }

      // Change database on trees
      ProgressFrame.displayProgress("Refresh in progress");
      for(int i=0;i<nbPanels;i++) {
        treePanels.get(i).treePanel.setDatabase(db);
        ProgressFrame.setProgress("Refreshing...", (int) (100.0 * (double)(i+1) / (double)nbPanels));
      }
      ProgressFrame.hideProgress();

      historyDlg.setDatabase(db, th);
      selectionDlg.setDatabase(db);
      updateTitle(th);
      defaultPanel.setSource(null, 0);
      splitPane.setRightComponent(defaultPanel);
      resetSearch();

    }

  }

  public void resetNavigation() {

    navManager.reset();
    navBar.enableForward(false);
    navBar.enableBack(false);

  }

  private void refreshTreePanel() {

    ProgressFrame.displayProgress("Refresh in progress");
    for(int i=0;i<nbPanels;i++) {
      treePanels.get(i).treePanel.refresh();
      ProgressFrame.setProgress("Refreshing...", (int) (100.0 * (double)(i+1) / (double)nbPanels));
    }
    ProgressFrame.hideProgress();

  }

  // Refresh all trees
  private void refreshTree() {

    refreshTreePanel();
    updateTabbedPane();
    resetSearch();

  }

  TreePanelHostCollection getHsotCollectionTreePanel() {
    return (TreePanelHostCollection)getInstanceOf(TreePanelHostCollection.class);
  }
  TreePanelServer getServerTreePanel() {
    return (TreePanelServer)getInstanceOf(TreePanelServer.class);
  }
  TreePanelDevice getDeviceTreePanel() {
    return (TreePanelDevice)getInstanceOf(TreePanelDevice.class);
  }
  TreePanelClass getClassTreePanel() {
    return (TreePanelClass)getInstanceOf(TreePanelClass.class);
  }
  TreePanelAlias getAliasTreePanel() {
    return (TreePanelAlias)getInstanceOf(TreePanelAlias.class);
  }
  TreePanelAttributeAlias getAttributeAliasTreePanel() {
    return (TreePanelAttributeAlias)getInstanceOf(TreePanelAttributeAlias.class);
  }
  TreePanelFreeProperty getPropertyTreePanel() {
    return (TreePanelFreeProperty)getInstanceOf(TreePanelFreeProperty.class);
  }

  // Get panel of given class
  private TreePanel getInstanceOf(Class _class) {

    boolean found = false;
    int i=0;
    while(!found && i<nbPanels) {
      found = treePanels.get(i).treePanel.getClass() == _class;
      if(!found) i++;
    }

    if(!found)
      return null;
    else
      return treePanels.get(i).treePanel;

  }

  // Load a property file in the database
  private void loadPropFile() {
    String err = "";
    TangoFileReader fr = new TangoFileReader(db);

    JFileChooser chooser = new JFileChooser(lastResOpenedDir);
    int returnVal = chooser.showOpenDialog(this);

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      ResDlg dlg = new ResDlg(this,f.getAbsolutePath());
      if( dlg.showDlg() ) {
        if (f != null) err = fr.parse_res_file(f.getAbsolutePath());
        if (err.length() > 0) JiveUtils.showJiveError(err);
        if (f != null) lastResOpenedDir = f.getAbsolutePath();
      }
    }
  }

  // Load a property file in the database
  private void checkPropFile() {
    
    String err = "";
    TangoFileReader fr = new TangoFileReader(db);

    JFileChooser chooser = new JFileChooser(lastResOpenedDir);
    int returnVal = chooser.showOpenDialog(this);

    if (returnVal == JFileChooser.APPROVE_OPTION) {

      File f = chooser.getSelectedFile();
      if( f!=null ) {

        Vector diff = new Vector();
        err = fr.check_res_file(f.getAbsolutePath(),diff);
        lastResOpenedDir = f.getAbsolutePath();

        if (err.length() > 0) {
          JiveUtils.showJiveError(err);
        } else {
          if( diff.size()>0 ) {
            // Show differences
            DiffDlg dlg = new DiffDlg(diff,f.getAbsolutePath());
            ATKGraphicsUtils.centerFrameOnScreen(dlg);
            dlg.setVisible(true);
          } else {
            JOptionPane.showMessageDialog(this,"Database and file match.");
          }
        }

      }

    }
  }

  // Show database info
  private void showDatabaseInfo() {

    if(db==null) return;

    try {

      String result = db.get_info();
      JOptionPane.showMessageDialog(this,result,"Database Info",JOptionPane.INFORMATION_MESSAGE);

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }
  
  // Show Multiple selection dialog
  private void showMultipleSelection() {

    if(!selectionDlg.isVisible())
      ATKGraphicsUtils.centerFrameOnScreen(selectionDlg);
    selectionDlg.clear();
    selectionDlg.setVisible(true);

  }

  // Display the history window
  public void showHistory() {

    if(!historyDlg.isVisible())
      ATKGraphicsUtils.centerFrameOnScreen(historyDlg);
    historyDlg.setVisible(true);

  }

  // Select a device and show the device tree panel
  public void goToDeviceNode(String devName) {

    if(isDevicePanelVisible()) {
      TreePanelDevice deviceTreePanel = getDeviceTreePanel();
      deviceTreePanel.selectDevice(devName);
      treePane.setSelectedComponent(deviceTreePanel);
      // Work around X11 bug
      treePane.getSelectedComponent().setVisible(true);
    }

  }

  private TangoNode goToDeviceFullNode(String devName) {

    if (isDevicePanelVisible()) {
      TreePanelDevice deviceTreePanel = getDeviceTreePanel();
      TangoNode selected = deviceTreePanel.selectDevice(devName);
      if (selected != null) {
        treePane.setSelectedComponent(deviceTreePanel);
        // Work around X11 bug
        treePane.getSelectedComponent().setVisible(true);
      }
      return selected;
    } else {
      return null;
    }

  }

  // Select a server and show the server tree panel
  public void goToServerNode(String srvName) {
    goToServerFullNode(srvName);
  }

  // Select a server and show the server tree panel
  public TangoNode goToServerFullNode(String srvName) {

    if (isServerPanelVisible()) {
      TreePanelServer serverTreePanel = getServerTreePanel();
      TangoNode selected = serverTreePanel.selectFullServer(srvName);
      if (selected != null) {
        treePane.setSelectedComponent(serverTreePanel);
        // Work around X11 bug
        treePane.getSelectedComponent().setVisible(true);
      }
      return selected;
    } else {
      return null;
    }

  }

  // Select a server and show the server tree panel
  public TangoNode goToServerRootNode(String srvName) {

    if (isServerPanelVisible()) {
      TreePanelServer serverTreePanel = getServerTreePanel();
      TangoNode selected = serverTreePanel.selectServerRoot(srvName);
      if (selected != null) {
        treePane.setSelectedComponent(serverTreePanel);
        // Work around X11 bug
        treePane.getSelectedComponent().setVisible(true);
      }
      return selected;
    } else {
      return null;
    }

  }

  // Tabbed pane listener
  public void stateChanged(ChangeEvent e) {

    updateTabbedPane();

  }

  private void updateTabbedPane() {

    TreePanel p = (TreePanel)(treePane.getSelectedComponent());
    p.refreshValues();

  }

  // TreeListener
  void updatePanel(TangoNode[] source) {

    int i;

    // Check if there is some unsaved change
    try {

      Component panel = splitPane.getRightComponent();

      if( panel instanceof PropertyPanel) {
        PropertyPanel propertyPanel = (PropertyPanel)panel;
        if( propertyPanel.hasChanged() ) {
          if( JOptionPane.showConfirmDialog(this,"Some properties has been updated and not saved.\nWould you like to save change ?","Confirmation",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION )
            propertyPanel.saveChange();
        }
      }

      if( panel instanceof SingleAttributePanel) {
        SingleAttributePanel attPanel = (SingleAttributePanel)panel;
        if( attPanel.hasChanged() ) {
          if( JOptionPane.showConfirmDialog(this,"Some attribute properties has been updated and not saved.\nWould you like to save change ?","Confirmation",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION )
            attPanel.saveChange();
        }
      }

    } catch(Exception e) {}


    // No selection
    if(source==null || source.length==0) {
      defaultPanel.setSource(null,0);
      splitPane.setRightComponent(defaultPanel);
      return;
    }

    TreePanel p = (TreePanel)treePane.getSelectedComponent();
    if(recordPos) navManager.recordPath(p.tree);
    navBar.addLink(p.tree.getSelectionPath());

    navBar.enableBack(navManager.canGoBackward());
    navBar.enableForward(navManager.canGoForward());



    // Check node class
    boolean sameClass = true;
    Class nodeClass = source[0].getClass();
    i=1;
    while(sameClass && i<source.length) {
      sameClass = (source[i].getClass() == nodeClass);
      i++;
    }

    // Get last selection when several node class are selected
    if( !sameClass ) {
      TangoNode[] newSource = new TangoNode[1];
      newSource[0] = source[0];
      source = newSource;
    }

    // Update the panel
    if(source[0] instanceof PropertyNode) {
      PropertyNode[] nodes = new PropertyNode[source.length];
      for(i=0;i<source.length;i++) nodes[i] = (PropertyNode)source[i];
      propertyPanel.setSource(nodes);
      splitPane.setRightComponent(propertyPanel);
    } else if(nodeClass == TaskPollingNode.class) {
      TaskPollingNode[] nodes = new TaskPollingNode[source.length];
      for(i=0;i<source.length;i++) nodes[i] = (TaskPollingNode)source[i];
      devicePollingPanel.setSource(nodes);
      splitPane.setRightComponent(devicePollingPanel);
    } else if(nodeClass == TaskEventNode.class) {
      TaskEventNode[] nodes = new TaskEventNode[source.length];
      for(i=0;i<source.length;i++) nodes[i] = (TaskEventNode)source[i];
      deviceEventPanel.setSource(nodes);
      splitPane.setRightComponent(deviceEventPanel);
    } else if(nodeClass == TaskAttributeNode.class) {
      TaskAttributeNode[] nodes = new TaskAttributeNode[source.length];
      for(i=0;i<source.length;i++) nodes[i] = (TaskAttributeNode)source[i];
      deviceAttributePanel.setSource(nodes);
      splitPane.setRightComponent(deviceAttributePanel);
    } else if(nodeClass == TaskPipeNode.class) {
      TaskPipeNode[] nodes = new TaskPipeNode[source.length];
      for(i=0;i<source.length;i++) nodes[i] = (TaskPipeNode)source[i];
      devicePipePanel.setSource(nodes);
      splitPane.setRightComponent(devicePipePanel);
    } else if(nodeClass == TaskLoggingNode.class) {
      TaskLoggingNode[] nodes = new TaskLoggingNode[source.length];
      for(i=0;i<source.length;i++) nodes[i] = (TaskLoggingNode)source[i];
      deviceLoggingPanel.setSource(nodes);
      splitPane.setRightComponent(deviceLoggingPanel);
    } else if(nodeClass == TaskSingleAttributeNode.class) {
      TaskSingleAttributeNode[] nodes = new TaskSingleAttributeNode[source.length];
      for(i=0;i<source.length;i++) nodes[i] = (TaskSingleAttributeNode)source[i];
      singleAttributePanel.setSource(nodes);
      splitPane.setRightComponent(singleAttributePanel);
    } else {
      defaultPanel.setSource(source[0],source.length);
      splitPane.setRightComponent(defaultPanel);
    }

  }

  // Exit application
  private void exitForm() {
    if (running_from_shell)
      System.exit(0);
    else {
      setVisible(false);
      dispose();
    }
  }

  // Center the window
  private void centerWindow() {
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    Dimension scrsize = toolkit.getScreenSize();
    Dimension appsize = new Dimension(850, 500);
    int x = (scrsize.width - appsize.width) / 2;
    int y = (scrsize.height - appsize.height) / 2;
    setBounds(x, y, appsize.width, appsize.height);
  }

  private static String getVersion(){
    Package p = MainPanel.class.getPackage();

    //if version is set in MANIFEST.mf
    if(p.getImplementationVersion() != null) return p.getImplementationVersion();

    return DEFAULT_VERSION;
  }

  // Main function
  static void printUsage() {
    System.out.println("Usage: jive [-r] [-s server] [-d device] [-fxx filter] [-p panel]");
    System.out.println("   -r        Read only mode (No write access to database allowed)");
    System.out.println("   -s server Open jive and show specified server node (server=ServerName/instance)");
    System.out.println("   -d device Open jive and show specified device node (device=domain/family/member)");
    System.out.println("   -fs filter Default server filter");
    System.out.println("   -fd filter Default device filter");
    System.out.println("   -fc filter Default class filter");
    System.out.println("   -fa filter Default alias filter");
    System.out.println("   -faa filter Default attribute alias filter");
    System.out.println("   -fp filter Default property filter");
    System.out.println("   -p panelmask (1=Collection 2=Server 4=Device 8=Class 16=DevAlias 32=AttAlias 64=FreeProperty"); 
    System.exit(0);
  }

  public static void main(String args[]) {

    if(args.length==0) {

      new MainPanel(true,false);

    } else {

      boolean readOnly = false;
      int i = 0;
      String server = null;
      String device = null;
      String fs = null;
      String fd = null;
      String fc = null;
      String fa = null;
      String faa = null;
      String fp = null;
      int pmask = 126;

      while(i<args.length) {

        if(args[i].equalsIgnoreCase("-r")) {
          readOnly = true;
        } else if(args[i].equalsIgnoreCase("-s")) {
          i++;
          if(i>=args.length)
            printUsage();
          server = args[i];
        } else if(args[i].equalsIgnoreCase("-d")) {
          i++;
          if(i>=args.length)
            printUsage();
          device = args[i];
        } else if(args[i].equalsIgnoreCase("-fs")) {
          i++;
          if(i>=args.length)
            printUsage();
          fs = args[i];
        } else if(args[i].equalsIgnoreCase("-fd")) {
          i++;
          if(i>=args.length)
            printUsage();
          fd = args[i];
        } else if(args[i].equalsIgnoreCase("-fc")) {
          i++;
          if(i>=args.length)
            printUsage();
          fc = args[i];
        } else if(args[i].equalsIgnoreCase("-fa")) {
          i++;
          if(i>=args.length)
            printUsage();
          fa = args[i];
        } else if(args[i].equalsIgnoreCase("-faa")) {
          i++;
          if(i>=args.length)
            printUsage();
          faa = args[i];
        } else if(args[i].equalsIgnoreCase("-fp")) {
          i++;
          if(i>=args.length)
            printUsage();
          fp = args[i];
        } else if(args[i].equalsIgnoreCase("-p")) {
          i++;
          if(i>=args.length)
            printUsage();
          pmask = Integer.parseInt(args[i]);
        } else {
          System.out.println("Invalid option " + args[i]);
          printUsage();
        }
        i++;

      }

      MainPanel p = new MainPanel(true,readOnly,pmask);
      if(server!=null)
        p.goToServerFullNode(server);
      if(device!=null)
        p.goToDeviceNode(device);
      if(fs!=null)
        p.filterServer(fs);
      if(fd!=null)
        p.filterDevice(fd);
      if(fc!=null)
        p.filterClass(fc);
      if(fa!=null)
        p.filterAlias(fa);
      if(faa!=null)
        p.filterAttributeAlias(faa);
      if(fp!=null)
        p.filterProperty(fp);

    }

  }

}
