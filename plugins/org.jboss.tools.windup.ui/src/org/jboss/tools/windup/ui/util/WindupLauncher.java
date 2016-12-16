/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.

 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.windup.ui.util;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.windup.runtime.WindupRmiClient;
import org.jboss.tools.windup.runtime.WindupRmiClient.ProgressCallback;
import org.jboss.tools.windup.ui.WindupUIPlugin;
import org.jboss.tools.windup.ui.internal.Messages;
import org.jboss.tools.windup.ui.internal.services.ConsoleService;
import org.jboss.tools.windup.ui.internal.services.MarkerService;
import org.jboss.tools.windup.ui.util.FutureUtils.AbstractDelegatingMonitorJob;
import org.jboss.tools.windup.windup.ConfigurationElement;

@Singleton
@Creatable
public class WindupLauncher {
	
	// TODO: Move to preference item.
	public static final long WINDUP_START_DURATION_TIMEOUT = 15000;

	@Inject private MarkerService markerService;
	@Inject private ConsoleService consoleService;
	@Inject private WindupRmiClient windupClient;
	@Inject @Named (IServiceConstants.ACTIVE_SHELL) Shell shell;

	public void launchWindup(ConfigurationElement configuration, Consumer<ConfigurationElement> windupStartedCallback) {
		markerService.deleteAllWindupMarkers();
		if (!windupClient.isWindupServerRunning()) {
			String windupHome = windupClient.getWindupHome().toString();
			boolean executable = new File(windupHome).setExecutable(true);
			if (executable) {
				startWindup(configuration, windupStartedCallback);
			}
			else {
				WindupUIPlugin.logErrorMessage(Messages.WindupNotExecutableInfo);
				String message = Messages.WindupNotExecutableInfo + " - " + windupHome;
				MessageDialog.openError(shell, Messages.WindupNotExecutableTitle, message);
			}
		}
		else {
			windupStartedCallback.accept(configuration);
		}
	}
	
	private void startWindup(ConfigurationElement configuration, Consumer<ConfigurationElement> windupStartedCallback) {
		WindupRmiClient.ProgressCallback callback = new WindupRmiClient.ProgressCallback() {
			private boolean serverStarted = false;
			@Override
			public void serverStarted() {
				serverStarted = true;
			}
			@Override
			public boolean isServerStarted() {
				return serverStarted;
			}
			@Override
			public void log(String line) {
				consoleService.write(line);
			}
			@Override
			public void processFailed(String message) {
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(shell, Messages.WindupServerError, message);
				});
			}
		};

		Job job = createStartWindupJob(callback);
		Display.getDefault().syncExec(() -> {
			IStatus status = FutureUtils.runWithProgress(job, WINDUP_START_DURATION_TIMEOUT, 7, shell, Messages.WindupStartingDetail);
			if (status.isOK()) {
				windupStartedCallback.accept(configuration);
			}
			else {
				WindupLauncher.openError(shell, status.getMessage());
			}
		});
	}
	
	public Job createStartWindupJob(final ProgressCallback callback) {
		Job job = new AbstractDelegatingMonitorJob(Messages.WindupStartingTitle) {
			@Override
			protected IStatus doRun(IProgressMonitor monitor) {
				Future<IStatus> future = new Future<IStatus>() {
					private AtomicBoolean cancelled = new AtomicBoolean(false);
					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						cancelled.set(true);
						return true;
					}
					@Override
					public boolean isCancelled() {
						return cancelled.get();
					}
					@Override
					public boolean isDone() {
						return callback.isServerStarted();
					}
					@Override
					public IStatus get() throws InterruptedException, ExecutionException {
						return null;
					}
					@Override
					public IStatus get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						return null;
					}
				};
				windupClient.startWindup(monitor, callback);
				try {
					FutureUtils.waitForFuture(WINDUP_START_DURATION_TIMEOUT, future, monitor);
				} catch (ExecutionException | TimeoutException | InterruptedException e) {
					WindupUIPlugin.log(e);
					WindupLauncher.openError(shell, e.getMessage());
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		return job;
	}
	
	private static void openError(Shell shell, String message) {
		Display.getDefault().asyncExec(() -> {
			String msg = Messages.WindupStartingError + " " + message;
			MessageDialog.openError(shell,  Messages.WindupServerError, msg);
		});
	}
}