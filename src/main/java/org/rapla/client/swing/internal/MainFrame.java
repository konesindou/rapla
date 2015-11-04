/*--------------------------------------------------------------------------*
 | Copyright (C) 2014 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.client.swing.internal;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.Locale;

import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import org.rapla.RaplaResources;
import org.rapla.client.ClientService;
import org.rapla.client.swing.RaplaGUIComponent;
import org.rapla.client.swing.images.RaplaImages;
import org.rapla.client.swing.toolkit.DialogUI;
import org.rapla.client.swing.toolkit.DialogUI.DialogUiFactory;
import org.rapla.client.swing.toolkit.FrameControllerList;
import org.rapla.client.swing.toolkit.RaplaFrame;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.framework.internal.ContainerImpl;
import org.rapla.framework.logger.Logger;



public class MainFrame extends RaplaGUIComponent
    implements
        ModificationListener
{
    RaplaMenuBar menuBar;
    private final RaplaFrame frame ;
    Listener listener = new Listener();
    CalendarEditor cal;
    JLabel statusBar = new JLabel("");
    private final RaplaImages raplaImages;
    private final FrameControllerList frameControllerList;
    private final DialogUiFactory dialogUiFactory;
    @Inject
    public MainFrame(RaplaMenuBarContainer menuBarContainer,ClientFacade facade, RaplaResources i18n, RaplaLocale raplaLocale, Logger logger, RaplaMenuBar raplaMenuBar, CalendarEditor editor, RaplaImages raplaImages, FrameControllerList frameControllerList, DialogUiFactory dialogUiFactory) throws RaplaException {
        super(facade, i18n, raplaLocale, logger);
        this.menuBar = raplaMenuBar;
        this.raplaImages = raplaImages;
        this.frameControllerList = frameControllerList;
        this.dialogUiFactory = dialogUiFactory;
        frame =  (RaplaFrame)getMainComponent();
        String title = getQuery().getSystemPreferences().getEntryAsString(ContainerImpl.TITLE, getString("rapla.title"));
        // CKO TODO Title should be set in config along with the facade used
        frame.setTitle(title );
        	
        cal = editor;
        getUpdateModule().addModificationListener(this);

        JMenuBar menuBar = menuBarContainer.getMenubar();
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(statusBar);
        menuBar.add(Box.createHorizontalStrut(5));
        frame.setJMenuBar( menuBar );

        getContentPane().setLayout( new BorderLayout() );
      //  getContentPane().add ( statusBar, BorderLayout.SOUTH);

        getContentPane().add(  cal.getComponent() , BorderLayout.CENTER );
    }

    public void show()  {
        getLogger().debug("Creating Main-Frame");
        createFrame();
        //dataChanged(null);
        setStatus();
        cal.start();
        frame.setIconImage(raplaImages.getIconFromKey("icon.rapla_small").getImage());
        frame.setVisible(true);
        frameControllerList.setMainWindow(frame);
    }

    private JPanel getContentPane() {
        return (JPanel) frame.getContentPane();
    }

    private void createFrame() {
        Dimension dimension = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(new Dimension(
                                    Math.min(dimension.width,1200)
                                    ,Math.min(dimension.height-20,900)
                                    )
                      );

        frame.addVetoableChangeListener(listener);
        //statusBar.setBorder( BorderFactory.createEtchedBorder());
    }


    class Listener implements VetoableChangeListener {
        public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
            if (shouldExit())
                close();
            else
                throw new PropertyVetoException("Don't close",evt);
        }

    }

    public Window getFrame() {
        return frame;
    }

    public JComponent getComponent() {
        return (JComponent) frame.getContentPane();
    }

    public void dataChanged(ModificationEvent e) throws RaplaException {
        cal.dataChanged( e );
        new StatusFader(statusBar).updateStatus();
    }
    
    private void setStatus() {
        statusBar.setMaximumSize( new Dimension(400,20));
        final StatusFader runnable = new StatusFader(statusBar);
        final Thread fadeThread = new Thread(runnable);
        fadeThread.setDaemon( true);
        fadeThread.start();
    
    }
    
    class StatusFader implements Runnable{
        JLabel label;

        StatusFader(JLabel label)
        {
          this.label=label;
        }

        public void run() {
            try {
                
                
                {
                    User  user = getUser();
                    final boolean admin = user.isAdmin();
                    String name = user.getName();
                    if ( name == null ||  name.length() == 0 )
                    {
                        name = user.getUsername();
                    }
                    String message = getI18n().format("rapla.welcome",name);
                    if ( admin)
                    {
                        message = message + " " + getString("admin.login");
                    }
                    
                    statusBar.setText(message);
                    fadeIn( statusBar );
                }
                Thread.sleep(2000);
                {
                    fadeOut( statusBar);
                    if (getUserModule().isSessionActive())
                    {
                    	updateStatus();
                    }
                    fadeIn( statusBar );
                }
            } catch (InterruptedException ex) {
                //Logger.getLogger(Fader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RaplaException e) {
            }
        }

        public void updateStatus() throws RaplaException {
            User  user = getUser();
            final boolean admin = user.isAdmin();
            String message =   getString("user") + " "+ user.toString(); 
            Allocatable template = getModification().getTemplate();
            if ( template != null)
            {
            	Locale locale = getLocale();
                message = getString("edit-templates") + " [" +  template.getName(locale) + "] " + message; 
            }

            statusBar.setText( message);
            final Font boldFont = statusBar.getFont().deriveFont(Font.BOLD);
            statusBar.setFont( boldFont);
            if ( admin)
            {
                statusBar.setForeground( new Color(220,30,30));
            }
            else
            {
                statusBar.setForeground( new Color(30,30,30) );
            }
        }

        private void fadeIn(JLabel label) throws InterruptedException {
            int alpha=0;
            Color c = label.getForeground();
            while(alpha<=230){
                alpha+=25;
                final Color color = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                label.setForeground(color);
                label.repaint();
                Thread.sleep(200);
            }
        }
        
        private void fadeOut(JLabel label) throws InterruptedException {
            int alpha=250;
            Color c = label.getForeground();
            while(alpha>0){
                alpha-=25;
                final Color color = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                label.setForeground(color);
                label.repaint();
                Thread.sleep(200);
            }
        }
    }

    protected boolean shouldExit() {
		try {
	        DialogUI dlg = dialogUiFactory.create(
				                           frame.getRootPane()
				                           ,true
				                           ,getString("exit.title")
				                           ,getString("exit.question")
				                           ,new String[] {
				                               getString("exit.ok")
				                               ,getString("exit.abort")
				                           }
				                           );
			dlg.setIcon(raplaImages.getIconFromKey("icon.question"));
	        //dlg.getButton(0).setIcon(getIcon("icon.confirm"));
	        dlg.getButton(0).setIcon(raplaImages.getIconFromKey("icon.abort"));
	        dlg.setDefault(1);
	        dlg.start();
        return (dlg.getSelectedIndex() == 0);
		} catch (RaplaException e) {
			getLogger().error( e.getMessage(), e);
			return true;
		}

    }

    public void close() {
        getUpdateModule().removeModificationListener(this);
        frame.close();
    }

}




