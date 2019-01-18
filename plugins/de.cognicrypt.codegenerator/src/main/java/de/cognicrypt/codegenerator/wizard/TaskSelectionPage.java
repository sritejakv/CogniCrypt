/********************************************************************************
 * Copyright (c) 2015-2018 TU Darmstadt
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package de.cognicrypt.codegenerator.wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import de.cognicrypt.codegenerator.Activator;
import de.cognicrypt.codegenerator.tasks.Task;
import de.cognicrypt.codegenerator.tasks.TaskJSONReader;
import de.cognicrypt.core.Constants;

public class TaskSelectionPage extends WizardPage {

	private static final String KEY_IMAGE = "key.png";
	private static final String WIFI_IMAGE = "wifi.png";
	private static final String LOCK_IMAGE = "lock.png";
	private static final String SIGN_IMAGE = "signing.png";
	private static final String MPC_IMAGE = "mpc.png";


	private Composite container;
	private Task selectedTask = null;

	public TaskSelectionPage() {
		super(Constants.SELECT_TASK);
		setTitle(Constants.TASK_LIST);
		setDescription(Constants.DESCRIPTION_TASK_SELECTION_PAGE);
		setPageComplete(false);
	}

	@Override
	public void createControl(final Composite parent) {
		final ScrolledComposite sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		this.container = new Composite(sc, SWT.NONE);
		this.container.setBounds(10, 10, 450, 200);

		//To display the Help view after clicking the help icon
		PlatformUI.getWorkbench().getHelpSystem().setHelp(sc, "de.cognicrypt.codegenerator.TaskSelectionHelp");

		final GridLayout gl = new GridLayout(2, false);
		gl.verticalSpacing = -6;
		this.container.setLayout(gl);

		new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);

		final Image encImage = loadImage(LOCK_IMAGE);
		final Button encryptionButton = createImageButton(this.container, encImage);

		final Label useCaseDescriptionLabel = new Label(this.container, SWT.WRAP);
		final GridData gd_selectProjectLabel = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 4);
		gd_selectProjectLabel.heightHint = 200;
		gd_selectProjectLabel.widthHint = 600;
		useCaseDescriptionLabel.setLayoutData(gd_selectProjectLabel);

		final Image hashImage = loadImage(KEY_IMAGE);
		final Button hashButton = createImageButton(this.container, hashImage);

		final Image secChanImage = loadImage(WIFI_IMAGE);
		final Button secChanButton = createImageButton(this.container, secChanImage);
		
		final Image signImage = loadImage(SIGN_IMAGE);
		final Button signButton = createImageButton(this.container, signImage);
		
		final Image mpcImage = loadImage(MPC_IMAGE);
		final Button mpcButton = createImageButton(this.container, mpcImage);


		final Button[] buttons = new Button[] { encryptionButton, hashButton, secChanButton, signButton, mpcButton};
		final Image[] unclickedImages = new Image[] { encImage, hashImage, secChanImage, signImage, mpcImage};
		//final Button[] buttons = new Button[] { encryptionButton, hashButton, secChanButton, crcButton };
		//final Image[] unclickedImages = new Image[] { encImage, hashImage, secChanImage, crcImage };
		
		// Get Tasks
		final List<Task> tasks = TaskJSONReader.getTasks();
		final Task[] taskdescs = new Task[] {
				// TODO we should organize that file correctly and don't do such dirty hacks
				tasks.get(0), tasks.get(1), tasks.get(2), tasks.get(4), tasks.get(5) };
				//tasks.get(0), tasks.get(1), tasks.get(2), tasks.get(5), tasks.get(3) };


		for (final Button button : buttons) {
			button.addListener(SWT.Selection, new SelectionButtonListener(buttons, unclickedImages, taskdescs, useCaseDescriptionLabel));
		}

		encryptionButton.notifyListeners(SWT.Selection, new Event());

		setControl(this.container);
		new Label(this.container, SWT.NONE);
		new Label(this.container, SWT.NONE);

		sc.setContent(this.container);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(this.container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setControl(sc);
	}

	public IProject getSelectedProject() {

		// this information must be queried from the Locator page.
		return null;//this.selectedProject;
	}

	public Task getSelectedTask() {
		// TODO return task depending on the currently selected use case (via button)
		return this.selectedTask;
		//return (Task) ((IStructuredSelection) this.taskComboSelection.getSelection()).getFirstElement();
	}

	@Override
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		if (visible) {
			this.container.setFocus();
		}
	}

	private Button createImageButton(final Composite container, final Image startImage) {
		final Button b = new Button(container, SWT.WRAP);
		final Rectangle bounds = startImage.getBounds();
		b.setSize(bounds.width, bounds.height);
		b.setImage(startImage);

		return b;
	}

	private Image loadImage(final String image) {
		try {
			final Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
			if (bundle == null) {
				return null;
			}

			final URL entry = bundle.getEntry("src/main/resources/images/" + image);
			final URL resolvedURL = FileLocator.toFileURL(entry);
			URI resolvedURI = null;
			if (resolvedURL != null) {
				resolvedURI = new URI(resolvedURL.getProtocol(), resolvedURL.getPath(), null);
			} else {
				resolvedURI = FileLocator.resolve(entry).toURI();
			}

			final File file = new File(resolvedURI);
			final InputStream is = new FileInputStream(file);

			return new Image(PlatformUI.getWorkbench().getDisplay(), is);
		} catch (final Exception ex) {
			Activator.getDefault().logError(ex);
		}

		return null;
	}

	class SelectionButtonListener implements Listener {

		private final Button[] buttons;
		private final Image[] unclicked;
		private final Task[] tasks;

		private final Label targetLabel;

		public SelectionButtonListener(final Button[] buttons, final Image[] unclicked, final Task[] tasks, final Label targetLabel) {
			if (buttons.length != unclicked.length || buttons.length != tasks.length) {
				throw new IllegalArgumentException("All arrays are required to have the same length." + "If not it indicates an incomplete setup for buttons and their images");
			}

			this.buttons = buttons;
			this.unclicked = unclicked;
			this.tasks = tasks;
			this.targetLabel = targetLabel;
		}

		@Override
		public void handleEvent(final Event event) {
			final Button eventButton = (Button) event.widget;
			for (int i = 0; i < this.buttons.length; i++) {
				final Button b = this.buttons[i];
				if (eventButton.equals(b)) {
					b.setSelection(true);
					this.targetLabel.setText(this.tasks[i].getTaskDescription());
					TaskSelectionPage.this.selectedTask = this.tasks[i];
					setPageComplete(true);
				} else {
					b.setSelection(false);
					b.setImage(this.unclicked[i]);
				}
			}
		}
	}
}