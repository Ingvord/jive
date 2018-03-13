package jive3;

import fr.esrf.TangoApi.Database;
import fr.esrf.TangoApi.DbAttribute;
import fr.esrf.Tango.DevFailed;

import javax.swing.*;

import jive.JiveUtils;

import java.io.IOException;
import java.util.Vector;

// ---------------------------------------------------------------

class TaskDeviceAttributePropertyNode extends PropertyNode {

  private Database db;
  private String   devName;
  private String   attributeName;
  private int      idl;
  private boolean  dbAttribute;

  TaskDeviceAttributePropertyNode(TreePanel parent,Database db,String devName,String attributeName,int idl,boolean isDB) {
    this.db = db;
    this.devName = devName;
    this.attributeName = attributeName;
    this.parentPanel = parent;
    this.idl = idl;
    this.dbAttribute = isDB;
  }

  void populateNode() throws DevFailed {
  }

  ImageIcon getIcon() {
    if(dbAttribute)
      return TangoNodeRenderer.uleaficon;
    else
      return TangoNodeRenderer.leaficon;
  }

  public String toString() {
    return attributeName;
  }

  String getTitle() {
    return "Device attribute properties";
  }

  String getDevName() {
    return devName;
  }

  String getName() {
    return devName + "/" + attributeName;
  }
  
  String getAttributeName() {
    return attributeName;
  }

  public boolean isLeaf() {
    return true;
  }

  public void viewHistory() {

    parentPanel.invoker.historyDlg.viewDeviceAttPropertyHistory(devName,attributeName,"*");
    parentPanel.invoker.showHistory();
    
  }

  void saveProperties() {

    try {
      DbFileWriter.SaveDeviceAttributeProperties(devName, attributeName);
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    } catch (IOException e2) {
      JiveUtils.showJiveError(e2.getMessage());
    }

  }

  String[][] getProperties() {

    String[][] ret = new String[0][0];

    try {

      DbAttribute lst = db.get_device_attribute_property(devName, attributeName);

      String plist[] = lst.get_property_list();
      Vector pvec = new Vector();
      for(int i = 0; i < plist.length ; i++) {
        if(JiveUtils.IsAttCfgItem(plist[i],idl)<0) {
          pvec.add(plist[i]);
        }
      }

      ret = new String[pvec.size()][2];

      for (int i = 0; i < pvec.size(); i++) {
        ret[i][0] = (String) pvec.get(i);
        ret[i][1] = lst.get_string_value((String) pvec.get(i));
      }

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

    return ret;

  }

  void setProperty(String propName, String value) {

    try {
      DbAttribute att = new DbAttribute(attributeName);
      att.add(propName, JiveUtils.makeStringArray(value));
      db.put_device_attribute_property(devName,att);
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void deleteProperty(String propName) {

    try {
      db.delete_device_attribute_property(devName,attributeName,propName);
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  public void execAction(int number) {
    switch(number) {

      case TreePanel.ACTION_COPY:
        JiveUtils.the_clipboard.clear();
        String[][] props = getProperties();
        for(int i=0;i<props.length;i++)
          JiveUtils.the_clipboard.add(props[i][0],attributeName,props[i][1]);
        break;

      case TreePanel.ACTION_PASTE:

        JiveUtils.the_clipboard.parse();

        // Paste attribute property
        for(int i=0;i<JiveUtils.the_clipboard.getAttPropertyLength();i++)
          setProperty(JiveUtils.the_clipboard.getAttPropertyName(i),
                      JiveUtils.the_clipboard.getAttPropertyValue(i));

        // Paste object property
        for(int i=0;i<JiveUtils.the_clipboard.getObjectPropertyLength();i++)
          setProperty(JiveUtils.the_clipboard.getObjectPropertyName(i),
                      JiveUtils.the_clipboard.getObjectPropertyValue(i));

        parentPanel.refreshValues();
        break;

      case TreePanel.ACTION_VIEW_HISTORY:
        viewHistory();
        break;

      case TreePanel.ACTION_SAVE_PROP:
        saveProperties();
        break;

    }
  }

}