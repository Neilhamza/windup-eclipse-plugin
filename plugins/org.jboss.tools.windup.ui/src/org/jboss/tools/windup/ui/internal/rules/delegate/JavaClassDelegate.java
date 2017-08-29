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

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationListener;
import org.eclipse.jface.viewers.ColumnViewerEditorDeactivationEvent;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.jboss.tools.windup.ui.internal.Messages;
import org.jboss.tools.windup.ui.internal.editor.DeleteNodeAction;
import org.jboss.tools.windup.ui.internal.editor.ElementAttributesContainer;
import org.jboss.tools.windup.ui.internal.editor.RulesetElementUiDelegateFactory.ClassAttributeRow;
import org.jboss.tools.windup.ui.internal.editor.RulesetElementUiDelegateFactory.IElementUiDelegate;
import org.jboss.tools.windup.ui.internal.editor.RulesetElementUiDelegateFactory.RulesetConstants;
import org.jboss.tools.windup.ui.internal.rules.annotation.AnnotationContentProvider;
import org.jboss.tools.windup.ui.internal.rules.annotation.AnnotationElement;
import org.jboss.tools.windup.ui.internal.rules.annotation.AnnotationElement.AttributeElement;
import org.jboss.tools.windup.ui.internal.rules.annotation.AnnotationModel;
import org.jboss.tools.windup.ui.internal.rules.delegate.AnnotationUtil.EvaluationContext;
import org.jboss.tools.windup.ui.internal.rules.delegate.AnnotationUtil.IAnnotationEmitter;
import org.jboss.tools.windup.ui.internal.services.RulesetDOMService;
import org.jboss.windup.ast.java.data.TypeReferenceLocation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

@SuppressWarnings({"restriction"})
public class JavaClassDelegate extends ElementUiDelegate {
	
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
	
	public void generateAnnotationElements(Annotation annotation, EvaluationContext evaluationContext) {
		IAnnotationEmitter emitter = new IAnnotationEmitter() {
			@Override
			public void emitSingleValue(String value, EvaluationContext evaluationContext) {
				detailsTab.createAnnotationLiteral(value, (Element)evaluationContext.getElement());
			}
			@Override
			public void emitMemberValuePair(String name, String value, EvaluationContext evaluationContext) {
				detailsTab.createAnnotationLiteral(name, value, (Element)evaluationContext.getElement());
			}
			
			@Override
			public void emitBeginMemberValuePairArrayInitializer(String name, EvaluationContext evaluationContext) {
				Node annotationList = detailsTab.createAnnotationList(name, (Element)evaluationContext.getElement());
				evaluationContext.setElement(annotationList);
			}
			
			@Override
			public void emitEndMemberValuePairArrayInitializer(EvaluationContext evaluationContext) {
				// popup to the current context's element's parent.
				Element element = (Element)evaluationContext.getElement();
				element = (Element)element.getParentNode();
				// not sure if this is right, we might need to get the parent context (witch might already have this parent element as its elemnt)
				evaluationContext.setElement(element);
			}
			
			@Override
			public void emitBeginArrayInitializer(EvaluationContext evaluationContext) {
				// Assuming we're handling arrays with no name (ie., not a MemberValuePair) as nameless annotation-list
				Node annotationList = detailsTab.createAnnotationList(null, (Element)evaluationContext.getElement());
				evaluationContext.setElement(annotationList);
			}
			
			@Override
			public void emitEndArrayInitializer(EvaluationContext evaluationContext) {
				// popup to the current context's element's parent.
				Element element = (Element)evaluationContext.getElement();
				element = (Element)element.getParentNode();
				// not sure if this is right, we might need to get the parent context (witch might already have this parent element as its elemnt)
				evaluationContext.setElement(element);
			}
			
			@Override
			public void emitAnnotation(Annotation annotation, EvaluationContext evaluationContext) {
				String annotationName = annotation.getTypeName().getFullyQualifiedName();
				ITypeBinding typeBinding= annotation.resolveTypeBinding();
				if (typeBinding != null) {
					annotationName = typeBinding.getQualifiedName();
				}
				if (evaluationContext.isTopLevelContext()) {
					boolean initialized = isJavaclassInitialized(element);
					if (!initialized) {
						detailsTab.initialize(annotationName, element);
						evaluationContext.setElement(element);
					}
					else if (!evaluationContext.isInitialized()){
						Node anntotationTypeNode = detailsTab.createAnnotationType(annotationName, element);
						evaluationContext.setElement(anntotationTypeNode);
					}
				}
				else {
					Element parent = (Element)evaluationContext.getElement();
					Node anntotationTypeNode = detailsTab.createAnnotationType(annotationName, parent);
					evaluationContext.setElement(anntotationTypeNode);
				}
			}
		};
		annotation.accept(new SnippetAnnotationVisitor(emitter, evaluationContext));
	}
	
	private boolean isJavaclassInitialized(Element element) {
		if (element.getAttribute(RulesetConstants.JAVA_CLASS_REFERENCES).isEmpty() && 
				element.getElementsByTagName(RulesetConstants.JAVA_CLASS_LOCATION).getLength() == 0) {
			return false;
		}
		return true;
	}
	
	private Form topContainer;
	private DetailsTab detailsTab;
	
	@Override
	public void update() {
		detailsTab.update();
		//topContainer.reflow(true);
	}
	
	@Override
	public Control getControl() {
		if (topContainer == null) {
			//topContainer = toolkit.createScrolledForm(parent);
			topContainer = toolkit.createForm(parent);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(topContainer.getBody());
			createTabs();
		}
		return topContainer;
	}
	
	@Override
	protected <T> TabWrapper addTab(Class<T> clazz) {
		IEclipseContext child = createTabContext(topContainer.getBody());
		T obj = create(clazz, child);
		return new TabWrapper(obj, child, null);
	}
	
	protected void createTabs() {
		this.detailsTab = (DetailsTab)addTab(DetailsTab.class).getObject();
	}

	@Override
	public Object[] getChildren() {
		return super.getChildren();
	}
	
	public static class DetailsTab extends ElementAttributesContainer {
		
		@Inject private RulesetDOMService domService;
		
		private AnnotationModel annotationModel;
		private TreeViewer annotationTree;
		
		private JavaClassLocationContainer locationContainer;
		private JavaClassAnnotationLiteralContainer annotationLiteralContainer;
		private JavaClassAnnotationListContainer annotationListContainer;
		private JavaClassAnnotationTypeContainer annotationTypeContainer;
		
		private ClassAttributeRow javaClassReferenceRow;
		
		public void initialize(String annotationName, Element parent) {
			javaClassReferenceRow.setText(annotationName);
			locationContainer.createLocationWithAnnotationType(parent);
		}
		
		private Node createAnnotationType(String pattern, Element parent) {
			return annotationTypeContainer.createAnnotationTypeWithPattern(pattern, parent);
		}
		
		private void createAnnotationLiteral(String value, Element parent) {
			annotationLiteralContainer.createAnnotationLiteralWithValue(null, value, parent);
		}
		
		private void createAnnotationLiteral(String name, String value, Element parent) {
			annotationLiteralContainer.createAnnotationLiteralWithValue(name, value, parent);
		}
		
		private Node createAnnotationList(String name, Element parent) {
			return annotationListContainer.createAnnotationList(name, parent);
		}
		
		@PostConstruct
		@SuppressWarnings("unchecked")
		public void createControls(Composite parent) {
			Composite client = super.createSection(parent, 3, toolkit, element, ExpandableComposite.TITLE_BAR |Section.NO_TITLE_FOCUS_BOX, Messages.ruleElementDetails, null);
			Section section = (Section)client.getParent();
			section.setExpanded(true);

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
				    		javaClassReferenceRow = new ClassAttributeRow(element, declaration, project) {
				    			@Override
				    			protected Node getNode() {
				    				return findNode(element, ed, declaration);
				    			}
				    		};
						rows.add(javaClassReferenceRow);
						javaClassReferenceRow.createContents(client, toolkit, 2);
				    	}
				    	else {
				    		rows.add(ElementAttributesContainer.createTextAttributeRow(element, toolkit, declaration, client, 3));
				    	}
			    }
			    //createSections(parent, section);
			    createAnnotationModelTree(parent, section);
			}
		}
		
		private void createAnnotationModelTree(Composite parent, Composite top) {
			Composite container = toolkit.createComposite(parent);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			container.setLayout(layout);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			
			Group group = new Group(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(group);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
			
			annotationTree = new TreeViewer(group);
			
			this.annotationModel = new AnnotationModel(element, modelQuery, model);
			AnnotationContentProvider provider = new AnnotationContentProvider();
			DelegatingStyledCellLabelProvider styleProvider = new DelegatingStyledCellLabelProvider(provider);
			annotationTree.setContentProvider(provider);
			annotationTree.setLabelProvider(styleProvider);
			annotationTree.setInput(annotationModel);
			
			FontDescriptor descriptor = FontDescriptor.createFrom(JFaceResources.getDialogFont());
			descriptor = descriptor.increaseHeight(0);
			final Font customFont = descriptor.createFont(Display.getDefault());
			descriptor = descriptor.setStyle(SWT.ITALIC);
			
			Control control = annotationTree.getControl();
			control.setFont(customFont);
			
			GridDataFactory.fillDefaults().grab(true, true).applyTo(control);
			
			createViewerContextMenu();
			
			annotationTree.expandAll();
			
			annotationTree.setColumnProperties(new String[]{"col1"});
			
			annotationTree.setCellEditors(new CellEditor[]{new TextCellEditor(annotationTree.getTree())});
			annotationTree.setCellModifier(new XMLCMCellModifier());
		}
		
		private void createViewerContextMenu() {
			MenuManager popupMenuManager = new MenuManager();
			IMenuListener listener = new IMenuListener() {
				@Override
				public void menuAboutToShow(IMenuManager mng) {
					fillContextMenu(mng);
				}
			};
			popupMenuManager.addMenuListener(listener);
			popupMenuManager.setRemoveAllWhenShown(true);
			Control control = annotationTree.getControl();
			Menu menu = popupMenuManager.createContextMenu(control);
			control.setMenu(menu);
		}
		
		@SuppressWarnings("unchecked")
		private void fillContextMenu(IMenuManager manager) {
			ISelection selection = annotationTree.getSelection();
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (!ssel.isEmpty()) {
				if (ssel.toList().size() == 1) {
					AnnotationElement annotationElement = (AnnotationElement)ssel.getFirstElement();
					Element element = annotationElement.getElement();
					IElementUiDelegate delegate = uiDelegateRegistry.getUiDelegate(element);
					if (delegate != null) {
						delegate.fillContextMenu(manager, annotationTree);
					}
				}
				Action deleteAction = new Action() {
					@Override
					public ImageDescriptor getImageDescriptor() {
						return PDEPluginImages.DESC_DELETE;
					}

					@Override
					public ImageDescriptor getDisabledImageDescriptor() {
						return PDEPluginImages.DESC_REMOVE_ATT_DISABLED;
					}

					@Override
					public void run() {
						List<AnnotationElement> annotationElements = ssel.toList();
						List<Element> elements = annotationElements.stream().map(e -> e.getElement()).distinct().collect(Collectors.toList());
						removeNodes(elements);
					}
				};
				deleteAction.setText(Messages.RulesetEditor_RemoveElement);
				manager.add(deleteAction);
			}
			this.annotationTree.getControl().update();
		}
		
		public void removeNodes(List<Element> elements) {
			if (elements.isEmpty()) {
				return;
			}
			IStructuredModel model = super.model;
			try {
				model.aboutToChangeModel();

				filterChildElements(elements);
			
				Node nextSelection = findElementForSelection(elements);
				
				new DeleteNodeAction(model, elements).run();
				
				if (nextSelection != null) {
					annotationTree.setSelection(new StructuredSelection(nextSelection));
				}
			}
			finally {
				model.changedModel();
			}
		}
		
		private Node findElementForSelection(List<Element> toBeDeleted) {
			if (toBeDeleted.size() > 1) {
				return null;
			}
			Element firstElement = toBeDeleted.get(0);
			Element parent = (Element)firstElement.getParentNode();
			
			Node nextSelection = domService.findNextSibling(toBeDeleted.get(toBeDeleted.size()-1), 1);
			if (nextSelection == null || toBeDeleted.contains(nextSelection)) {
				// no next node, use previous node
				nextSelection = domService.findPreviousSibling(firstElement);
			}

			if (nextSelection == null || toBeDeleted.contains(nextSelection)) {
				// next or previous null, use parent
				nextSelection = parent;
			}
			return nextSelection;
		}
		
		private void filterChildElements(List<Element> elements) {
			for (Iterator<Element> iter = Lists.newArrayList(elements).iterator(); iter.hasNext();) {
				Element element = iter.next();
				// climb parent hierarchy, and remove element if one of parents is in list.
				while (true) {
					Node parent = element.getParentNode();
					if (parent == null) {
						break;
					}
					if (elements.contains(parent)) {
						iter.remove();
						break;
					}
					parent = element.getParentNode();
					if (!(parent instanceof Element)) {
						break;
					}
					element = (Element)parent; 
				}
			}
		}
		
		private void createSections(Composite parent, Composite top) {
			
			Composite container = toolkit.createComposite(parent);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			container.setLayout(layout);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);

			locationContainer = new JavaClassLocationContainer(element, model, modelQuery, elementDeclaration, toolkit, uiDelegateFactory, context, contentHelper);
			locationContainer.createControls(container);
			
			container = toolkit.createComposite(parent);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginBottom = 0;
			layout.marginWidth = 0;
			container.setLayout(layout);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			
			
			annotationLiteralContainer = new JavaClassAnnotationLiteralContainer(element, model, modelQuery, elementDeclaration, toolkit, uiDelegateFactory, context, contentHelper);
			annotationLiteralContainer.createControls(container);
			
			container = toolkit.createComposite(parent);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginBottom = 0;
			layout.marginWidth = 0;
			container.setLayout(layout);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			
			annotationListContainer = new JavaClassAnnotationListContainer(element, model, modelQuery, elementDeclaration, toolkit, uiDelegateFactory, context, contentHelper);
			annotationListContainer.createControls(container);
			
			container = toolkit.createComposite(parent);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginBottom = 0;
			layout.marginWidth = 0;
			container.setLayout(layout);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			
			annotationTypeContainer = new JavaClassAnnotationTypeContainer(element, model, modelQuery, elementDeclaration, toolkit, uiDelegateFactory, context, contentHelper);
			annotationTypeContainer.createControls(container);
		}
		
		@Override
		protected void bind() {
			super.bind();
			annotationTree.refresh(annotationModel, true);
//			locationContainer.bind();
//			annotationLiteralContainer.bind();
//			annotationListContainer.bind();
//			annotationTypeContainer.bind();
		}
		
		private class CellListener extends ColumnViewerEditorActivationListener implements ICellEditorListener {

			private AttributeElement attributeElement;
			private CellEditor editor;
			private String originalValue;

			CellListener(AttributeElement attributeElement, CellEditor editor) {
				this.attributeElement = attributeElement;
				this.editor = editor;
				originalValue = attributeElement.getValue();
				attributeElement.getStructuredModel().aboutToChangeModel();
			}

			public void applyEditorValue() {
				editor.removeListener(this);
			}

			public void cancelEditor() {
				final Object value = editor.getValue();
				if (value != null && !value.equals(originalValue)) {
					attributeElement.setValue(String.valueOf(originalValue));
				}
				editor.removeListener(this);
			}

			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
				if (newValidState) {
					attributeElement.setValue(editor.getValue().toString());
				}
			}

			public void beforeEditorActivated(ColumnViewerEditorActivationEvent event) {
			}

			public void afterEditorActivated(ColumnViewerEditorActivationEvent event) {
			}

			public void beforeEditorDeactivated(ColumnViewerEditorDeactivationEvent event) {
			}

			public void afterEditorDeactivated(ColumnViewerEditorDeactivationEvent event) {
				attributeElement.getStructuredModel().changedModel();
				annotationTree.getColumnViewerEditor().removeEditorActivationListener(this);
			}
		}

		public class XMLCMCellModifier implements ICellModifier {
			
			public boolean canModify(Object element, String property) {
				boolean result = false;
				if (element instanceof AttributeElement) {
					/* Set up the cell editor based on the element */
					CellEditor[] editors = annotationTree.getCellEditors();
					if (editors.length > 0) {
						if (editors[0] != null)
							editors[0].dispose();
						editors[0] = createDefaultPropertyDescriptor((AttributeElement)element);
						if (editors[0] instanceof TextCellEditor) {
							final CellListener listener = new CellListener((AttributeElement)element, editors[0]);
							annotationTree.getColumnViewerEditor().addEditorActivationListener(listener);
							editors[0].addListener(listener);
							result = true;
						}
					}
				}
				return result;
			}

			public Object getValue(Object object, String property) {
				String result = null;
				if (object instanceof Node) {
					result = contentHelper.getNodeValue((Node) object);
				}
				return (result != null) ? result : ""; //$NON-NLS-1$
			}

			public void modify(Object element, String property, Object value) {
				Item item = (Item) element;
				if (item != null) {
					AttributeElement attributeElement = (AttributeElement)item.getData();
					String newValue = value.toString();
					if (newValue != null) {
						attributeElement.setValue(newValue);
					}
				}
			}

			protected CellEditor createDefaultPropertyDescriptor(AttributeElement element) {
				String attributeName = element.getCmNode().getNodeName();
				TextPropertyDescriptor descriptor = new TextPropertyDescriptor(attributeName, attributeName);
				return descriptor.createPropertyEditor(annotationTree.getTree());
			}
		}
	}
}