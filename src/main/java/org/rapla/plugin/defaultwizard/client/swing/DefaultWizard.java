
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
package org.rapla.plugin.defaultwizard.client.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.swing.MenuElement;

import org.rapla.RaplaResources;
import org.rapla.client.dialog.DialogUiFactoryInterface;
import org.rapla.client.event.StartActivityEvent;
import org.rapla.client.extensionpoints.ReservationWizardExtension;
import org.rapla.client.swing.RaplaGUIComponent;
import org.rapla.client.swing.SwingActivityController;
import org.rapla.client.swing.images.RaplaImages;
import org.rapla.client.swing.internal.SwingPopupContext;
import org.rapla.client.swing.toolkit.RaplaMenu;
import org.rapla.client.swing.toolkit.RaplaMenuItem;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Reservation;
import org.rapla.storage.PermissionController;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.facade.CalendarModel;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.framework.logger.Logger;
import org.rapla.inject.Extension;

import com.google.web.bindery.event.shared.EventBus;

/** This ReservationWizard displays no wizard and directly opens a ReservationEdit Window
*/
@Extension(provides = ReservationWizardExtension.class, id= "defaultWizard")
public class DefaultWizard extends RaplaGUIComponent implements ReservationWizardExtension, ActionListener
{
	Map<Component,DynamicType> typeMap = new HashMap<Component, DynamicType>();
	private final PermissionController permissionController;
    private final CalendarModel model;
    private final RaplaImages raplaImages;
    private final DialogUiFactoryInterface dialogUiFactory;
    private final EventBus eventBus;
    @Inject
	public DefaultWizard(ClientFacade facade, RaplaResources i18n, RaplaLocale raplaLocale, Logger logger, CalendarModel model, RaplaImages raplaImages, DialogUiFactoryInterface dialogUiFactory, EventBus eventBus){
        super(facade, i18n, raplaLocale, logger);
        this.permissionController = facade.getPermissionController();
        this.model = model;
        this.eventBus = eventBus;
        this.raplaImages = raplaImages;
        this.dialogUiFactory = dialogUiFactory;
    }
    
    public String getId() {
		return "000_defaultWizard";
	}

 	public MenuElement getMenuElement() {
 		typeMap.clear();
		List<DynamicType> eventTypes = new ArrayList<DynamicType>();
        final User user = getUser();
        try {
			DynamicType[] types = getQuery().getDynamicTypes(DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION);
			for ( DynamicType type: types)
			{
                if (permissionController.canCreate( type, user))
			    {
			        eventTypes.add( type );
			    }
			}
		} catch (RaplaException e) {
			return null;
		}
		boolean canCreateReservation = eventTypes.size() > 0;
		MenuElement element;
		String newEventText = getString("new_reservation");
		final ClientFacade clientFacade = getClientFacade();
		final RaplaLocale raplaLocale = getRaplaLocale();
        if ( eventTypes.size() == 1)
		{
		    RaplaMenuItem item = new RaplaMenuItem( getId());
            item.setEnabled( permissionController.canAllocate(model, user, raplaLocale) && canCreateReservation);
            DynamicType type = eventTypes.get(0);
            String name = type.getName( getLocale());
            if ( newEventText.endsWith( name))
            {
                item.setText(newEventText );
            }
            else
            {
                item.setText(newEventText + " " + name);
            }
            item.setIcon( raplaImages.getIconFromKey("icon.new"));
			item.addActionListener( this);
			
            typeMap.put( item, type);
			element = item;
		}
		else
		{
			RaplaMenu item = new RaplaMenu( getId());
			item.setEnabled( permissionController.canAllocate(model, user, raplaLocale) && canCreateReservation);
			item.setText(newEventText);
			item.setIcon( raplaImages.getIconFromKey("icon.new"));
			for ( DynamicType type:eventTypes)
			{
				RaplaMenuItem newItem = new RaplaMenuItem(type.getKey());
				String name = type.getName( getLocale());
                newItem.setText( name);
				item.add( newItem);
				newItem.addActionListener( this);
				typeMap.put( newItem, type);
			}
			element = item;
		}
		return element;
	}
    

	public void actionPerformed(ActionEvent e) {
		try
		{
	    	Object source = e.getSource();
			DynamicType type = typeMap.get(source);
	    	if ( type == null)
	    	{
	    		getLogger().warn("Type not found for " + source + " in map " + typeMap);
	    		return;
	    	}
	    	eventBus.fireEvent(new StartActivityEvent(SwingActivityController.CREATE_RESERVATION_FOR_DYNAMIC_TYPE, type.getId()));
		}
		catch (RaplaException ex)
		{
		    dialogUiFactory.showException( ex, new SwingPopupContext(getMainComponent(), null));
		}
    }

    public static List<Reservation> addAllocatables(CalendarModel model, Collection<Reservation> newReservations, User user) throws RaplaException
    {
        Collection<Allocatable> markedAllocatables = model.getMarkedAllocatables();
        if (markedAllocatables == null || markedAllocatables.size() == 0)
        {
            Collection<Allocatable> allocatables = Arrays.asList(model.getSelectedAllocatables());
            if (allocatables.size() == 1)
            {
                addAlloctables(newReservations, allocatables);
            }
        }
        else
        {
            Collection<Allocatable> allocatables = markedAllocatables;
            addAlloctables(newReservations, allocatables);
        }
        List<Reservation> list = new ArrayList<Reservation>();
        for (Reservation reservation : newReservations)
        {
            Reservation cast = reservation;
            String lastChangedBy = cast.getLastChangedBy();
            if (lastChangedBy != null && !lastChangedBy.equals(user.getId()))
            {
                throw new RaplaException("Reservation " + cast + " has wrong user " + lastChangedBy);
            }
            list.add(cast);
        }
        return list;
    }
	
    static private void addAlloctables(Collection<Reservation> events, Collection<Allocatable> allocatables)
    {
        for (Reservation event : events)
        {
            for (Allocatable alloc : allocatables)
            {
                event.addAllocatable(alloc);
            }
        }
    }

//	/**
//	 * @param model
//	 * @param startDate
//	 * @return
//	 */
//	protected Date getEndDate( CalendarModel model,Date startDate) {
//		Collection<TimeInterval> markedIntervals = model.getMarkedIntervals();
//		Date endDate = null;
//    	if ( markedIntervals.size() > 0)
//    	{
//    		TimeInterval first = markedIntervals.iterator().next();
//    		endDate = first.getEnd();
//    	}
//    	if ( endDate != null)
//    	{
//    		return endDate;
//    	}
//		return new Date(startDate.getTime() + DateTools.MILLISECONDS_PER_HOUR);
//	}
//
//	protected Date getStartDate(CalendarModel model) {
//		Collection<TimeInterval> markedIntervals = model.getMarkedIntervals();
//		Date startDate = null;
//    	if ( markedIntervals.size() > 0)
//    	{
//    		TimeInterval first = markedIntervals.iterator().next();
//    		startDate = first.getStart();
//    	}
//    	if ( startDate != null)
//    	{
//    		return startDate;
//    	}
//    	
//		
//		Date selectedDate = model.getSelectedDate();
//		if ( selectedDate == null)
//		{
//			selectedDate = getQuery().today();
//		}
//		Date time = new Date (DateTools.MILLISECONDS_PER_MINUTE * getCalendarOptions().getWorktimeStartMinutes());
//		startDate = getRaplaLocale().toDate(selectedDate,time);
//		return startDate;
//	}

	
    
	



}



