/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.windup.ui.internal.launch;

import jakarta.inject.Inject;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.jboss.tools.windup.core.services.WindupOptionsService;
import org.jboss.tools.windup.core.services.WindupService;
import org.jboss.tools.windup.model.domain.ModelService;

/**
 * The group of Windup launch configuration tabs.
 */
public class WindupLaunchGroup extends AbstractLaunchConfigurationTabGroup {
	
	@Inject private ModelService modelService;
	@Inject private WindupOptionsService optionsService;
	@Inject private WindupService windupService;

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			new WindupInputTab(modelService),
			new OptionsTab(modelService, optionsService),
		};
		setTabs(tabs);
	}
}
