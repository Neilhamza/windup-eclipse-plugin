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
package org.jboss.tools.windup.ui.internal.rules;

import java.io.File;

import jakarta.inject.Inject;

import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.windup.ui.internal.Messages;

public class ImportExistingRulesetWizard extends Wizard implements IImportWizard{

	@Inject protected EPartService partService;
	
	private ImportXMLRulesetWizardPage xmlPage;
	
	public ImportExistingRulesetWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.ImportRuleset_title);
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}
	
	@Override
	public void addPages() {
		xmlPage = new ImportXMLRulesetWizardPage();
		addPage(xmlPage);
	}

	@Override
	public boolean performFinish() {
		String location = xmlPage.getRulesetFileLocation();
		RuleRepositoryView view = (RuleRepositoryView)PlatformUI.getWorkbench().
				getActiveWorkbenchWindow().getActivePage().findView(RuleRepositoryView.VIEW_ID);
		view.addRulesets(new String[] {location});
		return true;
	}

	
	private static class ImportXMLRulesetWizardPage extends WizardPage {
		
		private Text directoryText;
		
		private String rulesetFileLocation;
		
		public ImportXMLRulesetWizardPage() {
			super("xmlPage"); //$NON-NLS-1$
			setTitle(Messages._UI_WIZARD_IMPORT_XML_RULESET_FILE_HEADING);
			setDescription(Messages._UI_WIZARD_IMPORT_XML_FILE_EXPL);
		}
		
		public String getRulesetFileLocation() {
			return rulesetFileLocation; 
		}
		
		@Override
		public void createControl(Composite parent) {
			GridLayoutFactory.fillDefaults().applyTo(parent);
			
			Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(container);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			
			Composite top = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(top);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(top);
			
			Label label = new Label(top, SWT.NONE);
			label.setText(Messages._UI_WIZARD_IMPORT_XML_RULESET_FILE);
			GridDataFactory.fillDefaults().hint(65, SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			
			directoryText = new Text(top, SWT.BORDER);
			directoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			directoryText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					validate();
				}
			});
			
			Button directoryButton = new Button(top, SWT.PUSH);
			directoryButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			directoryButton.setText(Messages.ImportRuleset_browse);
			directoryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell());
					dialog.setFilterExtensions(new String[] {NewXMLFilePage.FILTER_EXTENSION}); 
					rulesetFileLocation = dialog.open();
					if (rulesetFileLocation != null) {
						directoryText.setText(rulesetFileLocation);
					}
					validate();
				}
			});
			
			setControl(container);
			validate();
		}
		
		private void validate() {
			String path = directoryText.getText().trim();
			if (!path.isEmpty()) {
				if (new File(path).exists()) {
					setPageComplete(true);
				}
				else {
					setPageComplete(false);
				}
			}
			else {
				setPageComplete(false);
			}
		}
	}
}
