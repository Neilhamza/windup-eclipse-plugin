/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.windup.ui.internal.rules.delegate;

import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.jboss.tools.windup.ui.internal.RuleMessages;
import org.jboss.tools.windup.ui.internal.editor.ElementAttributesContainer;
import org.jboss.tools.windup.ui.internal.editor.RulesetElementUiDelegateFactory.ClassAttributeRow;
import org.jboss.tools.windup.ui.internal.editor.RulesetElementUiDelegateFactory.RulesetConstants;
import org.jboss.windup.ast.java.data.TypeReferenceLocation;
import org.w3c.dom.Node;

import com.google.common.base.Objects;

@SuppressWarnings({"restriction"})
public class JavaClassDelegate extends ElementUiDelegate {
	
	private static final int SASH_LEFT_DEFAULT = 550;
	private static final int SASH_RIGHT_DEFAULT = 400;
	
	enum JAVA_CLASS_REFERENCE_LOCATION {
		
		ANNOTATION(TypeReferenceLocation.ANNOTATION.toString(), "A Java class references the annotation."),
		CATCH_EXCEPTION_STATEMENT(TypeReferenceLocation.CATCH_EXCEPTION_STATEMENT.toString(), "A Java class method catches the specified type."),
		CONSTRUCTOR_CALL(TypeReferenceLocation.CONSTRUCTOR_CALL.toString(), "A Java class constructs the specified type."),
		ENUM_CONSTANT(TypeReferenceLocation.ENUM_CONSTANT.toString(), "A Java class declares the enumeration."),
		FIELD_DECLARATION(TypeReferenceLocation.FIELD_DECLARATION.toString(), "A Java class declares a field of the specified type."),
		IMPLEMENTS_TYPE(TypeReferenceLocation.IMPLEMENTS_TYPE.toString(), "A Java class implements the specified type; works transitively."),
		IMPORT(TypeReferenceLocation.IMPORT.toString(), "A Java class imports the type."), 
		INHERITANCE(TypeReferenceLocation.INHERITANCE.toString(), "A Java class inherits the specified type; works transitively."),
		INSTANCE_OF(TypeReferenceLocation.INSTANCE_OF.toString(), "A Java class of the specified type is used in an instanceof statement."),
		METHOD(TypeReferenceLocation.METHOD.toString(), "A Java class declares the referenced method."),
		METHOD_CALL(TypeReferenceLocation.METHOD_CALL.toString(), "A Java class calls the specified method; works transitively for interfaces."),
		METHOD_PARAMETER(TypeReferenceLocation.METHOD_PARAMETER.toString(), "A Java class declares the referenced method parameter."),
		RETURN_TYPE(TypeReferenceLocation.RETURN_TYPE.toString(), "A Java class returns the specified type."),
		TAGLIB_IMPORT(TypeReferenceLocation.TAGLIB_IMPORT.toString(), "This is only relevant for JSP sources and represents the import of a taglib into the JSP source file."),
		THROW_STATEMENT(TypeReferenceLocation.THROW_STATEMENT.toString(), "A method in the Java class throws the an instance of the specified type."),
		THROWS_METHOD_DECLARATION(TypeReferenceLocation.THROWS_METHOD_DECLARATION.toString(), "A Java class declares that it may throw the specified type."),
		TYPE(TypeReferenceLocation.TYPE.toString(), "A Java class declares the type."),
		VARIABLE_DECLARATION(TypeReferenceLocation.VARIABLE_DECLARATION.toString(), "A Java class declares a variable of the specified type."),
		VARIABLE_INITIALIZER(TypeReferenceLocation.VARIABLE_INITIALIZER.toString(), "A variable initalization expression value.");
		
		private String label;
		private String description;
		
		JAVA_CLASS_REFERENCE_LOCATION(String label, String description) {
			this.label = label;
			this.description = description;
		}
		
		public String getLabel() {
			return label;
		}
		
		public String getDescription() {
			return description;
		}
	}
	
	private Composite containerControl;
	private SashForm sash;
	
	private DetailsTab detailsTab;
	
	@Override
	public void update() {
		detailsTab.update();
	}
	
	@Override
	public Control getControl() {
		if (containerControl == null) {
			containerControl = createContainerControl(parent);
			createControls(containerControl);
		}
		return containerControl;
	}
	
	private Composite createContainerControl(Composite parent) {
		Composite container = toolkit.createComposite(parent);
		//container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
//		FormLayout layout = new FormLayout();
//		layout.spacing = 5;
//		container.setLayout(layout);
		GridLayoutFactory.fillDefaults().applyTo(container);
		return container;
	}
	
	private void createControls(Composite parent) {
		
		this.sash = new SashForm(parent, SWT.SMOOTH|SWT.VERTICAL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sash);
		sash.setOrientation(SWT.HORIZONTAL);
		sash.setFont(parent.getFont());
		sash.setVisible(true);
		
		Composite leftContainer = toolkit.createComposite(sash);
		GridLayoutFactory.fillDefaults().applyTo(leftContainer);
		//leftContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		FormData leftData = new FormData();
		leftData.top = new FormAttachment(null);
		leftData.left = new FormAttachment(0);
		leftData.bottom = new FormAttachment(100);
		leftContainer.setLayoutData(leftData);
		
		IEclipseContext context = super.createTabContext(leftContainer);
		detailsTab = super.create(DetailsTab.class, context);
		
		Composite rightContainer = toolkit.createComposite(sash);
		GridLayoutFactory.fillDefaults().applyTo(rightContainer);
		//rightContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		FormData rightData = new FormData();
		rightData.top = new FormAttachment(null);
		rightData.bottom = new FormAttachment(100);
		rightData.left = new FormAttachment(leftContainer);
		rightData.right = new FormAttachment(100);
		rightContainer.setLayoutData(rightData);
		
		leftData.right = new FormAttachment(50);
		
		createJavaEditor(rightContainer);
		
		sash.setWeights(new int[]{SASH_LEFT_DEFAULT, SASH_RIGHT_DEFAULT});
	}
	
	private void createJavaEditor(Composite parent) {
		parent.setLayout(new FormLayout());
		Composite client = ElementDetailsSection.createSection(parent, 3, toolkit, null);
		Section section = (Section)client.getParent();
		section.setText("Java Source Code"); //$NON-NLS-1$
		FormData data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		data.bottom = new FormAttachment(100);
		section.setLayoutData(data);
		
		new JavaEmbeddedEditor(client);
	}

	protected void createTabs() {
		addTab(DetailsTab.class);
	}

	@Override
	public Object[] getChildren() {
		return super.getChildren();
	}
	
	public static class DetailsTab extends ElementAttributesContainer {
		
		private JavaClassLocationContainer locationContainer;
		private JavaClassAnnotationLiteralContainer annotationLiteralContainer;
		private JavaClassAnnotationListContainer annotationListContainer;
		private JavaClassAnnotationTypeContainer annotationTypeContainer;
		
		@PostConstruct
		@SuppressWarnings("unchecked")
		public void createControls(Composite parent/*, CTabItem item*/) {
			//item.setText(Messages.ruleElementDetails);
			parent.setLayout(new FormLayout());
			Composite client = super.createSection(parent, 3);
			Section section = (Section)client.getParent();
			section.setDescription(RuleMessages.javaclass_description);
			FormData data = new FormData();
			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);
			section.setLayoutData(data);
			
			CMElementDeclaration ed = modelQuery.getCMElementDeclaration(element);
			if (ed != null) {
				List<CMAttributeDeclaration> availableAttributeList = modelQuery.getAvailableContent(element, ed, ModelQuery.INCLUDE_ATTRIBUTES);
			    for (CMAttributeDeclaration declaration : availableAttributeList) {
				    	if (Objects.equal(declaration.getAttrName(), RulesetConstants.JAVA_CLASS_REFERENCES)) {
				    		IFile file = context.get(IFile.class);
				    		IProject project = null;
				    		if (file != null) {
				    			project = file.getProject();
				    		}
				    		ClassAttributeRow row = new ClassAttributeRow(element, declaration, project) {
				    			@Override
				    			protected Node getNode() {
				    				return findNode(element, ed, declaration);
				    			}
				    		};
						rows.add(row);
						row.createContents(client, toolkit, 2);
				    	}
				    	else {
				    		rows.add(ElementAttributesContainer.createTextAttributeRow(element, toolkit, declaration, client, 3));
				    	}
			    }
			    createSections(parent, section);
			}
		}
		
		private void createSections(Composite parent, Section top) {
			locationContainer = new JavaClassLocationContainer(element, model, modelQuery, elementDeclaration, toolkit, uiDelegateFactory, context, contentHelper);
			Section section = locationContainer.createControls(parent);
			FormData data = new FormData();
			data.top = new FormAttachment(top);
			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);
			section.setLayoutData(data);
			
			data = new FormData();
			data.top = new FormAttachment(section);
			annotationLiteralContainer = new JavaClassAnnotationLiteralContainer(element, model, modelQuery, elementDeclaration, toolkit, uiDelegateFactory, context, contentHelper);
			section = annotationLiteralContainer.createControls(parent);
			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);
			section.setLayoutData(data);
			
			data = new FormData();
			data.top = new FormAttachment(section);
			annotationListContainer = new JavaClassAnnotationListContainer(element, model, modelQuery, elementDeclaration, toolkit, uiDelegateFactory, context, contentHelper);
			section = annotationListContainer.createControls(parent);
			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);
			section.setLayoutData(data);
			
			data = new FormData();
			data.top = new FormAttachment(section);
			annotationTypeContainer = new JavaClassAnnotationTypeContainer(element, model, modelQuery, elementDeclaration, toolkit, uiDelegateFactory, context, contentHelper);
			section = annotationTypeContainer.createControls(parent);
			data.left = new FormAttachment(0);
			data.right = new FormAttachment(100);
			section.setLayoutData(data);
		}
		
		@Override
		protected void bind() {
			super.bind();
			locationContainer.bind();
			annotationLiteralContainer.bind();
			annotationListContainer.bind();
			annotationTypeContainer.bind();
		}
	}
}