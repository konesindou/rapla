/*--------------------------------------------------------------------------*
 | Copyright (C) 2006  Christopher Kohlhaas                                 |
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

package org.rapla.client.swing.gui.edit.test;

import org.rapla.RaplaResources;
import org.rapla.client.ClientService;
import org.rapla.client.swing.gui.tests.GUITestCase;
import org.rapla.client.swing.images.RaplaImages;
import org.rapla.client.swing.internal.edit.AttributeEdit;
import org.rapla.client.swing.internal.edit.RaplaListEdit.RaplaListEditFactory;
import org.rapla.client.swing.toolkit.DialogUI.DialogUiFactory;
import org.rapla.client.swing.toolkit.FrameControllerList;
import org.rapla.components.i18n.BundleManager;
import org.rapla.components.i18n.server.ServerBundleManager;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.logger.Logger;

import junit.framework.Test;
import junit.framework.TestSuite;

public final class AttributeEditTest extends GUITestCase
{
    public AttributeEditTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AttributeEditTest.class);
    }



    public void testMain() throws Exception {
        ClientService clientService = getClientService();
        final Logger logger = getLogger();
        RaplaImages raplaImages = new RaplaImages(logger);
        final RaplaListEditFactory raplaListEditFactory = new RaplaListEditFactory (raplaImages);
        BundleManager bundleManager = new ServerBundleManager();
        RaplaResources i18n = new RaplaResources(bundleManager);
        FrameControllerList frameList = new FrameControllerList(logger);
        DialogUiFactory dialogUiFactory = new DialogUiFactory(i18n, raplaImages, bundleManager, frameList);
        AttributeEdit editor = new AttributeEdit(getFacade(), i18n, getRaplaLocale(), getLogger(), null, raplaListEditFactory, dialogUiFactory);
        editor.setDynamicType(clientService.getFacade().getDynamicTypes(null)[0]);
        testComponent(editor.getComponent(),500,500);
        getLogger().info("Attribute edit started");
    }

    public void testNew() throws Exception {
        ClientService clientService = getClientService();
        final Logger logger = getLogger();
        RaplaImages raplaImages = new RaplaImages(logger);
        final RaplaListEditFactory raplaListEditFactory = new RaplaListEditFactory (raplaImages);
        BundleManager bundleManager = new ServerBundleManager();
        RaplaResources i18n = new RaplaResources(bundleManager);
        FrameControllerList frameList = new FrameControllerList(logger);
        DialogUiFactory dialogUiFactory = new DialogUiFactory(i18n, raplaImages, bundleManager, frameList);
        AttributeEdit editor = new AttributeEdit(getFacade(), i18n, getRaplaLocale(), getLogger(), null, raplaListEditFactory, dialogUiFactory);
        DynamicType type =  clientService.getFacade().edit(clientService.getFacade().getDynamicTypes(null)[0]);
        Attribute attribute = type.getAttributes()[0];
        editor.setDynamicType(type);
        testComponent(editor.getComponent(),500,500);
        editor.selectAttribute( attribute);
    }


    public static void main(String[] args) {
        new AttributeEditTest(AttributeEditTest.class.getName()
                               ).interactiveTest("testMain");
    }
}
