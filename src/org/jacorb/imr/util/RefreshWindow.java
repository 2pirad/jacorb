package org.jacorb.imr.util;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
/**
 * This class shows a window which lets the user control
 * the behaviour of the refresh thread. It allows to change
 * the refresh interval and stop/restart the thread.
 *
 * @author Nicolas Noffke
 *
 * $Log: RefreshWindow.java,v $
 * Revision 1.1  2001-03-17 18:08:25  brose
 * Initial revision
 *
 * Revision 1.3  1999/11/25 16:05:49  brose
 * cosmetics
 *
 * Revision 1.2  1999/11/25 10:02:14  noffke
 * Wrote small comment.
 *
 *
 */

public class RefreshWindow  extends JFrame implements ActionListener{
    private JTextField m_interval_tf;
    private JButton m_ok_btn;
    private JButton m_cancel_btn;
    private Checkbox m_disable_box;
    private ImRModel m_model;
    
    public RefreshWindow(ImRModel model) {
	super("Refresh Interval Settings");

	m_model = model;

	JPanel _interval_panel = new JPanel();
	GridBagLayout _interval_gbl = new GridBagLayout();
	GridBagConstraints _constraints = new GridBagConstraints();

	JLabel _interval_lbl = new JLabel("Enter an Interval (in ms):");
	buildConstraints(_constraints, 0, 0, 1, 1, 1, 1);
	_constraints.fill = GridBagConstraints.NONE;
	_interval_gbl.setConstraints(_interval_lbl, _constraints);
	_interval_panel.add(_interval_lbl);
	
	m_interval_tf = new JTextField("" + m_model.m_current_refresh_interval, 10);
	buildConstraints(_constraints, 0, 1, 2, 1, 1, 1);
	_constraints.fill = GridBagConstraints.HORIZONTAL;
	_interval_gbl.setConstraints(m_interval_tf, _constraints);
	_interval_panel.add(m_interval_tf);

	m_disable_box = new Checkbox("Disable automatic refresh");
	m_disable_box.setState(m_model.m_refresh_disabled);
	buildConstraints(_constraints, 0, 2, 2, 1, 1, 1);
	_constraints.fill = GridBagConstraints.HORIZONTAL;
	_interval_gbl.setConstraints(m_disable_box, _constraints);
	_interval_panel.add(m_disable_box);

	m_ok_btn = new JButton("OK");
	m_ok_btn.addActionListener(this);
	buildConstraints(_constraints, 0, 3, 1, 1, 1, 1);
	_constraints.fill = GridBagConstraints.NONE;
	_interval_gbl.setConstraints(m_ok_btn, _constraints);
	_interval_panel.add(m_ok_btn);
	
	m_cancel_btn = new JButton("Cancel");
	m_cancel_btn.addActionListener(this);
	buildConstraints(_constraints, 1, 3, 1, 1, 1, 1);
	_constraints.fill = GridBagConstraints.NONE;
	_interval_gbl.setConstraints(m_cancel_btn, _constraints);
	_interval_panel.add(m_cancel_btn);

	_interval_panel.setLayout(_interval_gbl);

	getContentPane().add(_interval_panel);
	pack();
	setVisible(true);
    }

    private void buildConstraints(GridBagConstraints gbc, int gx, int gy, 
				  int gw, int gh, int wx, int wy){
	gbc.gridx = gx;
	gbc.gridy = gy;
	gbc.gridwidth = gw;
	gbc.gridheight = gh;
	gbc.weightx = wx;
	gbc.weighty = wy;
    }

    // implementation of java.awt.event.ActionListener interface
    /**
     *
     * @param event a button has been clicked.
     */
    public void actionPerformed(ActionEvent event) {
	JButton _source = (JButton) event.getSource();
	
	if (_source == m_cancel_btn)
	    dispose();
	else if (_source == m_ok_btn){
	    dispose();
	    if (m_disable_box.getState())
		//disabled is selected
		m_model.disableRefresh();
	    else
		m_model.setRefreshInterval(Integer.parseInt(m_interval_tf.getText()));
	}
    }  
} // RefreshWindow


