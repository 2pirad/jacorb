package org.jacorb.imr.util;

import javax.swing.*;
import java.awt.Component;
/**
 * This class sets the combo box in the server table
 * and preselects the correct host name for a server.
 *
 * @author Nicolas Noffke
 * 
 * $Id: ImRTableCellEditor.java,v 1.4 2002-03-19 09:25:11 nicolas Exp $
 */

public class ImRTableCellEditor extends DefaultCellEditor {
    
    public ImRTableCellEditor(JComboBox box){
	super(box);
    }

    /**
     * Return HostSelector JComboBox.
     */
    public Component getTableCellEditorComponent(JTable table,
						 Object value,
						 boolean isSelected,
						 int row,
						 int column){

	super.getTableCellEditorComponent(table, value, isSelected, row, column);
	
	JComboBox _box = (JComboBox) editorComponent;
	
	// preselect host name for server
	for(int _i = 0; _i < _box.getItemCount(); _i++){
	    if(((String) value).equals((String) _box.getItemAt(_i))){
		_box.setSelectedIndex(_i);
		break;
	    }
	}

	return _box;
    }
    
} // ImRTableCellEditor








