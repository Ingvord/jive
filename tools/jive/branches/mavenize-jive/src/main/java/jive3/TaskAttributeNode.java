package jive3;

import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.TangoApi.*;
import fr.esrf.Tango.DevFailed;

import javax.swing.*;
import java.util.Vector;

import jive.JiveUtils;

public class TaskAttributeNode extends TangoNode {

  private Database db;
  private String   devName;
  private Vector   attInfo;

  class AttInfo {
    String name;
    String min_alarm;
    String max_alarm;
    String min_warning;
    String max_warning;
    String delta_t;
    String delta_val;
    String unit;
    String display_unit;
    String standard_unit;
    String min;
    String max;
    String format;
    String label;
    String descr;
  }

  TaskAttributeNode(Database db, String devName) {
    this.db = db;
    this.devName = devName;
    attInfo = null;
  }

  void populateNode() throws DevFailed {
  }

  ImageIcon getIcon() {
    return TangoNodeRenderer.atticon;
  }

  public String toString() {
    return "Attribute config";
  }

  String getTitle() {
    return "Attribute configuration";
  }

  String getName() {
    return devName;
  }

  public boolean isLeaf() {
    return true;
  }

  int getAttributeNumber() {
    if(attInfo==null) browseAttributeInfo();
    return attInfo.size();
  }

  String getAttName(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).name;
  }

  // -- Alarms ----------------------------------------------------

  String getMinAlarm(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).min_alarm;
  }

  String getMaxAlarm(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).max_alarm;
  }

  String getMinWarning(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).min_warning;
  }

  String getMaxWarning(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).max_warning;
  }

  String getDeltaT(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).delta_t;
  }

  String getDeltaVal(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).delta_val;
  }

  void setMinAlarm(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      if(ai.alarms!=null)
        ai.alarms.min_alarm = value;
      else
        ai.min_alarm = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void setMaxAlarm(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      if(ai.alarms!=null)
        ai.alarms.max_alarm = value;
      else
        ai.max_alarm = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void setMinWarning(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      if(ai.alarms!=null)
        ai.alarms.min_warning = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void setMaxWarning(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      if(ai.alarms!=null)
        ai.alarms.max_warning = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void setDeltaT(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      if(ai.alarms!=null)
        ai.alarms.delta_t = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void setDeltaVal(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      if(ai.alarms!=null)
        ai.alarms.delta_val = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetAlarms(int idx) {

    try {

      // Clear database
      String[] pNames = {"min_alarm","max_alarm","min_warning","max_warning","delta_t","delta_val"};
      db.delete_device_attribute_property(devName,getAttName(idx),pNames);

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetLAlarms(int idx) {

    // Restore library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      if(ai.alarms!=null) {
        ai.alarms.min_alarm = "Not specified";
        ai.alarms.max_alarm = "Not specified";
        ai.alarms.min_warning = "Not specified";
        ai.alarms.max_warning = "Not specified";
        ai.alarms.delta_t = "Not specified";
        ai.alarms.delta_val = "Not specified";
      }
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetULAlarms(int idx) {

    // Restore user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      if(ai.alarms!=null) {
        ai.alarms.min_alarm = "";
        ai.alarms.max_alarm = "";
        ai.alarms.min_warning = "";
        ai.alarms.max_warning = "";
        ai.alarms.delta_t = "";
        ai.alarms.delta_val = "";
      }
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetCULAlarms(int idx) {

    // Restore class/user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      if(ai.alarms!=null) {
        ai.alarms.min_alarm = "NaN";
        ai.alarms.max_alarm = "NaN";
        ai.alarms.min_warning = "NaN";
        ai.alarms.max_warning = "NaN";
        ai.alarms.delta_t = "NaN";
        ai.alarms.delta_val = "NaN";
      }
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  // -- Units ----------------------------------------------------

  String getUnit(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).unit;
  }

  String getDisplayUnit(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).display_unit;
  }

  String getStandardUnit(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).standard_unit;
  }

  void setUnit(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.unit = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void setDisplayUnit(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.display_unit = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void setStandardUnit(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.standard_unit = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetUnit(int idx) {

    try {

      // Clear database
      String[] pNames = {"unit","display_unit","standard_unit"};
      db.delete_device_attribute_property(devName,getAttName(idx),pNames);

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetLUnit(int idx) {

    // Restore library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.unit = "Not specified";
      ai.display_unit = "Not specified";
      ai.standard_unit = "Not specified";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetULUnit(int idx) {

    // Restore user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.unit = "";
      ai.display_unit = "";
      ai.standard_unit = "";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetCULUnit(int idx) {

    // Restore class/user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.unit = "NaN";
      ai.display_unit = "NaN";
      ai.standard_unit = "NaN";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  // -- Range ----------------------------------------------------

  String getMin(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).min;
  }

  String getMax(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).max;
  }

  void setMin(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.min_value = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void setMax(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.max_value = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetRange(int idx) {

    try {

      // Clear database
      String[] pNames = {"min_value","max_value"};
      db.delete_device_attribute_property(devName,getAttName(idx),pNames);

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetLRange(int idx) {

    // Restore library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.min_value = "Not specified";
      ai.max_value = "Not specified";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetULRange(int idx) {

    // Restore user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.min_value = "";
      ai.max_value = "";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetCULRange(int idx) {

    // Restore class/user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.min_value = "NaN";
      ai.max_value = "NaN";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  // -- Display ----------------------------------------------------

  String getLabel(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).label;
  }

  String getFormat(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).format;
  }

  void setLabel(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.label = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void setFormat(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.format = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetDisplay(int idx) {

    try {

      // Clear database
      String[] pNames = {"label","format"};
      db.delete_device_attribute_property(devName,getAttName(idx),pNames);

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetLDisplay(int idx) {

    // Restore library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.label = "Not specified";
      ai.format = "Not specified";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetULDisplay(int idx) {

    // Restore user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.label = "";
      ai.format = "";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetCULDisplay(int idx) {

    // Restore class/user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.label = "NaN";
      ai.format = "NaN";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  // -- Description -------------------------------------------------------------

  String getDescription(int idx) {
    if(attInfo==null) browseAttributeInfo();
    return ((TaskAttributeNode.AttInfo)attInfo.get(idx)).descr;
  }

  void setDescription(int idx,String value) {

    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.description = value;
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetDescription(int idx) {

    try {

      // Clear database
      String[] pNames = {"description"};
      db.delete_device_attribute_property(devName,getAttName(idx),pNames);

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetLDescription(int idx) {

    // Restore library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.description = "Not specified";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetULDescription(int idx) {

    // Restore user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.description = "";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  void resetCULDescription(int idx) {

    // Restore class/user/library defaults (Tango8)
    try {
      DeviceProxy ds = new DeviceProxy(devName);
      AttributeInfoEx ai = ds.get_attribute_info_ex(getAttName(idx));
      ai.description = "";
      ds.set_attribute_info(new AttributeInfoEx[]{ai});
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  // -- Alias ----------------------------------------------------------------

  public String get_alias_attribute(String fullAttName) throws DevFailed {

    // Get the alias of an attribute
    DeviceData argin = new DeviceData();
    String request = "select alias from attribute_alias where name='" + fullAttName + "'";
    argin.insert(request);
    DeviceData argout = db.command_inout("DbMySqlSelect",argin);

    DevVarLongStringArray arg = argout.extractLongStringArray();
    if(arg.svalue.length==1) {
      if(arg.lvalue[0]!=0) return arg.svalue[0];
    }

    return "None";

  }

  void setAlias(int idx,String value) {

    try {
      String fullAttName = devName + "/" + getAttName(idx);
      if( value.equalsIgnoreCase("none") || value.length()==0 ) {
        String currentAlias = get_alias_attribute(fullAttName);
        db.delete_attribute_alias(currentAlias);
      } else {
        db.put_attribute_alias(fullAttName,value);
      }
    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  String getAlias(int idx) {

    try {
      String fullAttName = devName + "/" + getAttName(idx);
      return get_alias_attribute(fullAttName);
    } catch (DevFailed e) {
    }

    return "None";

  }


  // -- Browsing -------------------------------------------------------------

  void browseAttributeInfo() {

    try {

      DeviceProxy ds = new DeviceProxy(devName);
      attInfo = new Vector();
      String[] attList = ds.get_attribute_list();
      JiveUtils.sortList(attList);
      for (int i = 0; i < attList.length; i++) {
        AttributeInfoEx ai = ds.get_attribute_info_ex(attList[i]);
        TaskAttributeNode.AttInfo ei = new TaskAttributeNode.AttInfo();
        ei.name = attList[i];
        if (ai.alarms != null) {
          ei.min_alarm = reformat(ai.alarms.min_alarm);
          ei.max_alarm = reformat(ai.alarms.max_alarm);
          ei.min_warning = reformat(ai.alarms.min_warning);
          ei.max_warning = reformat(ai.alarms.max_warning);
          ei.delta_t = reformat(ai.alarms.delta_t);
          ei.delta_val = reformat(ai.alarms.delta_val);
        } else {
          ei.min_alarm = reformat(ai.min_alarm);
          ei.max_alarm = reformat(ai.max_alarm);
          ei.min_warning = "None";
          ei.max_warning = "None";
          ei.delta_t = "None";
          ei.delta_val = "None";
        }
        ei.unit = reformat(ai.unit);
        ei.display_unit = reformat(ai.display_unit);
        ei.standard_unit = reformat(ai.standard_unit);
        ei.min = reformat(ai.min_value);
        ei.max = reformat(ai.max_value);
        ei.format = reformat(ai.format);
        ei.label = reformat(ai.label);
        ei.descr = reformat(ai.description);
        attInfo.add(ei);
      }

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  public boolean isTango8() {

    try {

      DbDevImportInfo info = db.import_device(devName);
      DeviceProxy ds = new DeviceProxy("dserver/" + info.server);
      CommandInfo[] cmds = ds.command_list_query();
      // Search "ZmqEventSubscriptionChange"
      boolean found = false;
      int i=0;
      while(!found && i<cmds.length) {
        found = cmds[i].cmd_name.equalsIgnoreCase("ZmqEventSubscriptionChange");
        if(!found) i++;
      }
      return found;

    } catch (DevFailed e) {

      return false;

    }

  }

  public void restartDevice() {

    try {

      DbDevImportInfo info = db.import_device(devName);
      DeviceProxy ds = new DeviceProxy("dserver/" + info.server);
      DeviceData in = new DeviceData();
      in.insert(devName);
      ds.command_inout("DevRestart", in);

    } catch (DevFailed e) {
      JiveUtils.showTangoError(e);
    }

  }

  private String reformat(String s) {
    if(s.equalsIgnoreCase("Not specified")    ||
       s.equalsIgnoreCase("No standard unit") ||
       s.equalsIgnoreCase("No unit")          ||
       s.equalsIgnoreCase("No description")   ||
       s.equalsIgnoreCase("No display unit"))
      return "None";
    else
      return s;
  }

}
