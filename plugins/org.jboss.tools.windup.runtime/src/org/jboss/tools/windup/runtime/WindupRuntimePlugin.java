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
package org.jboss.tools.windup.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.xtext.util.StringInputStream;
import org.jboss.tools.common.util.PlatformUtil;
import org.jboss.tools.common.xml.IMemento;
import org.jboss.tools.common.xml.XMLMemento;
import org.jboss.tools.windup.runtime.options.Help;
import org.jboss.tools.windup.runtime.options.OptionDescription;
import org.osgi.framework.BundleContext;

import com.google.common.collect.Lists;

/**
 * The activator class for the plugin containing
 *
 */
public class WindupRuntimePlugin extends Plugin {
	/**
	 * <p>
	 * The plugin ID.
	 * </p>
	 */
	public static final String PLUGIN_ID = "org.jboss.tools.windup.runtime"; //$NON-NLS-1$

	/**
	 * <p>
	 * Location of the Windup home directory (this contains a full installation of
	 * windup)
	 * </p>
	 */
	private static final String WINDUP_DIRECTORY = "windup"; //$NON-NLS-1$
	private static final String MTA_DIRECTORY = "mta"; //$NON-NLS-1$

	private static final String HELP_CACHE = "/cache/help/help.xml"; //$NON-NLS-1$

	/**
	 * <p>
	 * The singleton instance of the plugin.
	 * </p>
	 */
	private static WindupRuntimePlugin plugin;

	public static String computeWindupHome() {
//		IEclipsePreferences defaultPreferences = DefaultScope.INSTANCE.getNode(WindupRuntimePlugin.PLUGIN_ID);
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(WindupRuntimePlugin.PLUGIN_ID);
		String path = preferences.get(IPreferenceConstants.WINDUP_HOME, "");
		if (path.isEmpty()) {
//			path = defaultPreferences.get(IPreferenceConstants.WINDUP_HOME, "");
			path = "/usr/local/bin/kantra";
		}
//		if (path.isEmpty()) {
//			path = WindupRuntimePlugin.getDefaultWindupHome().toPath().toString();
//		}
//		return new File(path).toPath();
		return path;
	}

	public static String computeWindupExecutable() {
		String location = WindupRuntimePlugin.computeWindupHome(); //$NON-NLS-1$ //$NON-NLS-2$
//		if (PlatformUtil.isWindows()) {
//			location = location + ".bat"; //$NON-NLS-1$
//		}
		return location.toString();
	}

	/**
	 * Returns the root directory of the embedded Windup installation.
	 */
	private static File getDefaultWindupHome() {
		try {
			File bundleFile = FileLocator.getBundleFile(WindupRuntimePlugin.getDefault().getBundle());
			File windupDirectory = new File(bundleFile, WINDUP_DIRECTORY);
			for (File file : windupDirectory.listFiles()) {
				// find the directory with a rules subdirectory... this is the actual unzipped
				// windup installation
				// (eg, PLUGIN_DIR/windup/windup-distribution-2.2.0.Final/)
				if (file.isDirectory() && new File(file, "rules").exists()) {
					return file;
				}
			}
			return null;
		} catch (IOException e) {
			WindupRuntimePlugin.logError("Error getting Windup Furnace add on repository location.", e); //$NON-NLS-1$
			return null;
		}
	}
	
	private static File getMtaOptions() {
		try {
			File bundleFile = FileLocator.getBundleFile(WindupRuntimePlugin.getDefault().getBundle());
			return new File(bundleFile, MTA_DIRECTORY + File.separatorChar + "help.xml");
		} catch (IOException e) {
			WindupRuntimePlugin.logError("Error getting MTA help location.", e); //$NON-NLS-1$
			return null;
		}
	}


	/**
	 * Returns the cached help file from the embedded Windup installation.
	 */
	public static Help findWindupHelpCache() {
		Help result = new Help();
		File cacheFile = WindupRuntimePlugin.getMtaOptions();
		try {
//			URL url = cacheFile.toURI().toURL();			
			InputStream input = new StringInputStream(WindupRuntimePlugin.getOptions()); // url.openStream();
			XMLMemento root = XMLMemento.createReadRoot(input);
			for (IMemento element : root.getChildren("option")) { //$NON-NLS-1$
				String name = element.getString("name"); //$NON-NLS-1$
				XMLMemento descriptionChild = (XMLMemento) element.getChild("description"); //$NON-NLS-1$
				String description = descriptionChild.getTextData();
				XMLMemento typeChild = (XMLMemento) element.getChild("type"); //$NON-NLS-1$
				String type = typeChild.getTextData();
				XMLMemento uiTypeChild = (XMLMemento) element.getChild("ui-type"); //$NON-NLS-1$
				String uiType = uiTypeChild.getTextData();
				List<String> availableOptions = Lists.newArrayList();
				XMLMemento availableOptionsElement = (XMLMemento) element.getChild("available-options"); //$NON-NLS-1$
				if (availableOptionsElement != null) {
					for (IMemento optionElement : availableOptionsElement.getChildren("option")) {
						XMLMemento optionMemento = (XMLMemento) optionElement;
						String availableOption = optionMemento.getTextData();
						availableOptions.add(availableOption);
					}
				}
				Collections.sort(availableOptions);
				boolean required = false;
				XMLMemento requiredElement = (XMLMemento) element.getChild("require");
				if (requiredElement != null) {
					required = Boolean.valueOf(requiredElement.getTextData());
				}
				OptionDescription option = new OptionDescription(name, description, type, uiType, availableOptions,
						required);
				result.getOptions().add(option);
			}
		} catch (Exception e) {
			WindupRuntimePlugin.log(e);
		}
		return result;
	}

	private static String getOptions() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "<help>\n"
		+ " <option name=\"overwrite\">\n"
		+ "		<description>If set, overwrite the output directory.</description>\n"
		+ "		<type>Boolean</type>\n"
		+ " 	<ui-type>SINGLE</ui-type>\n"
		+ " 	<required>false</required>\n"
		+ " </option>\n"
		+ " <option name=\"analyze-known-libraries\">\n"
		+ "		<description>Analyze known open-source libraries.</description>\n"
		+ "		<type>Boolean</type>\n"
		+ " 	<ui-type>SINGLE</ui-type>\n"
		+ " 	<required>false</required>\n"
		+ " </option>\n"
		+ " <option name=\"rules\">\n"
		+ "		<description>User Rules Directory/File.</description>\n"
		+ "		<type>File</type>\n"
		+ " 	<ui-type>FILE_OR_DIRECTORY</ui-type>\n"
		+ " 	<required>false</required>\n"
		+ " </option>\n"
		+ "	<option name=\"target\">\n"
		+ "		<description>The target technology to consider for analysis.</description>\n"
		+ "		<type>String</type>\n"
		+ "		<ui-type>SELECT_MANY</ui-type>\n"
		+ "		<available-options>\n"
		+ "			<option>azure</option>\n"
		+ "			<option>camel3</option>\n"
		+ "			<option>camel4</option>\n"
		+ "			<option>cloud</option>\n"
		+ "			<option>drools</option>\n"
		+ "			<option>eap</option>\n"
		+ "			<option>eap6</option>\n"
		+ "			<option>eap7</option>\n"
		+ "			<option>eap8</option>\n"
		+ "			<option>eapxp</option>\n"
		+ "			<option>fsw</option>\n"
		+ "			<option>fuse</option>\n"
		+ "			<option>hibernate</option>\n"
		+ "			<option>jakarta</option>\n"
		+ "			<option>java</option>\n"
		+ "			<option>jbpm</option>\n"
		+ "			<option>jws6</option>\n"
		+ "			<option>linux</option>\n"
		+ "			<option>openjdk</option>\n"
		+ "			<option>openjdk11</option>\n"
		+ "			<option>openjdk17</option>\n"
		+ "			<option>openjdk21</option>\n"
		+ "			<option>openliberty</option>\n"
		+ "			<option>quarkus</option>\n"
		+ "			<option>resteasy</option>\n"
		+ "			<option>rhr</option>\n"
		+ "		</available-options>\n"
		+ "		<required>true</required>\n"
		+ "	</option>\n"
		+ "	<option name=\"source\">\n"
		+ "		<description>The source technology to consider for analysis.</description>\n"
		+ "		<type>String</type>\n"
		+ "		<ui-type>SELECT_MANY</ui-type>\n"
		+ "		<available-options>\n"
		+ "			<option>agroal</option>\n"
		+ "			<option>amazon</option>\n"
		+ "			<option>apicurio</option>\n"
		+ "			<option>artemis</option>\n"
		+ "			<option>avro</option>\n"
		+ "			<option>camel</option>\n"
		+ "			<option>config</option>\n"
		+ "			<option>drools</option>\n"
		+ "			<option>eap</option>\n"
		+ "			<option>eap6</option>\n"
		+ "			<option>eap7</option>\n"
		+ "			<option>eap8</option>\n"
		+ "			<option>eapxp</option>\n"
		+ "			<option>elytron</option>\n"
		+ "			<option>flyway</option>\n"
		+ "			<option>glassfish</option>\n"
		+ "			<option>hibernate</option>\n"
		+ "			<option>hibernate-search</option>\n"
		+ "			<option>jakarta-ee</option>\n"
		+ "			<option>java-ee</option>\n"
		+ "			<option>jbpm</option>\n"
		+ "			<option>jboss</option>\n"
		+ "			<option>javaee</option>\n"
		+ "			<option>jdbs</option>\n"
		+ "			<option>jonas</option>\n"
		+ "			<option>jrun</option>\n"
		+ "			<option>jsonb</option>\n"
		+ "			<option>jsonp</option>\n"
		+ "			<option>kafka</option>\n"
		+ "			<option>keycloak</option>\n"
		+ "			<option>kubernetes</option>\n"
		+ "			<option>liquibase</option>\n"
		+ "			<option>log4j</option>\n"
		+ "			<option>logging</option>\n"
		+ "			<option>micrometer</option>\n"
		+ "			<option>narayana</option>\n"
		+ "			<option>openjdk</option>\n"
		+ "			<option>openjdk11</option>\n"
		+ "			<option>openshift</option>\n"
		+ "			<option>opentelemetry</option>\n"
		+ "			<option>oraclejdk</option>\n"
		+ "			<option>orion</option>\n"
		+ "			<option>picocli</option>\n"
		+ "			<option>resin</option>\n"
		+ "			<option>resteasy</option>\n"
		+ "			<option>rmi</option>\n"
		+ "			<option>rpc</option>\n"
		+ "			<option>seam</option>\n"
		+ "			<option>soa</option>\n"
		+ "			<option>spring</option>\n"
		+ "			<option>spring-boot</option>\n"
		+ "			<option>thorntail</option>\n"
		+ "			<option>weblogic</option>\n"
		+ "			<option>websphere</option>\n"
		+ "		</available-options>\n"
		+ "		<required>false</required>\n"
		+ "	</option>\n"
		+ "</help>";
	}

	public static String computeJRELocation() {
		String location = InstanceScope.INSTANCE.getNode(WindupRuntimePlugin.PLUGIN_ID)
				.get(IPreferenceConstants.WINDUP_JRE_HOME, null);
		if (location == null) {
			IVMInstall jre = JavaRuntime.getDefaultVMInstall();
			if (jre != null) {
				location = jre.getInstallLocation().getAbsolutePath();
			}
		}
		return location != null ? location : "";
	}

	/**
	 * @return singleton instance of the plugin
	 */
	public static WindupRuntimePlugin getDefault() {
		return plugin;
	}

	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
		plugin = null;
	}

	public static void log(IStatus status) {
		if (WindupRuntimePlugin.getDefault() != null) {
			WindupRuntimePlugin.getDefault().getLog().log(status);
		}
	}

	public static void logErrorMessage(final String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, null));
	}

	public static void logErrorMessage(final String message, final Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, e));
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else {
			status = new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, e.getMessage(), e);
		}
		log(status);
	}

	/**
	 * <p>
	 * Logs an error message.
	 * </p>
	 * 
	 * @param message
	 *            Error message to log
	 */
	public static void logError(String message, Throwable exception) {
		WindupRuntimePlugin.getDefault().getLog()
				.log(new Status(IStatus.ERROR, WindupRuntimePlugin.PLUGIN_ID, message, exception));
	}

	public static void logInfo(String message) {
		WindupRuntimePlugin.getDefault().getLog().log(new Status(IStatus.INFO, WindupRuntimePlugin.PLUGIN_ID, message));
	}
}