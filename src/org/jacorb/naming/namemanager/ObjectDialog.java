/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2004  Gerald Brose.
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.jacorb.naming.namemanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ObjectDialog
    extends JDialog
    implements ActionListener, KeyListener
{
    JTextField nameField;
    JTextField iorField;
    boolean isOk;

    public ObjectDialog(Frame frame, int updInt)
    {
        super(frame, "Bind Object", true);
        isOk = false;
        JPanel mainPanel = new JPanel( new GridLayout(2,1));
        getContentPane().add(mainPanel);
        JPanel hiPanel = new JPanel();
        hiPanel.setLayout( new BoxLayout( hiPanel, BoxLayout.Y_AXIS ));
        JPanel loPanel = new JPanel();

        mainPanel.add(hiPanel);
        mainPanel.add(loPanel);

        JLabel nameLabel = new JLabel("Name:");
        JLabel objectLabel = new JLabel("IOR:");
        nameField = new JTextField(40);
        iorField = new JTextField(40);

        hiPanel.add(nameLabel); 
        hiPanel.add(nameField); 
        hiPanel.add(objectLabel);
        hiPanel.add(iorField);

        JButton ok = new JButton("Ok");
        JButton cancel = new JButton("Cancel");

        loPanel.add(ok); 
        loPanel.add(cancel);

        ok.addActionListener(this);
        cancel.addActionListener(this);

    }

    public String getName()
    {
        return nameField.getText();
    }

    public String getIOR()
    {
        return iorField.getText();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Ok")) 
        {
            try
            {
                isOk = true; 
                dispose();
            } 
            catch (Exception ex)
            {
                JOptionPane.showMessageDialog( this, ex.getMessage(),
                                               "Input error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else dispose();
    }
    public void keyPressed(KeyEvent e) 
    {
        if (e.getKeyCode()==KeyEvent.VK_ENTER) 
            actionPerformed(new ActionEvent(this, 0, "Ok"));
        else if (e.getKeyCode()==KeyEvent.VK_ESCAPE) 
            actionPerformed(new ActionEvent(this, 0, "Cancel"));
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
}



