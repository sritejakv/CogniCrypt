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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.clafer.instance.InstanceClafer;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import de.cognicrypt.codegenerator.Activator;
import de.cognicrypt.codegenerator.featuremodel.clafer.ClaferModelUtils;
import de.cognicrypt.codegenerator.question.Answer;
import de.cognicrypt.codegenerator.question.Question;
import de.cognicrypt.codegenerator.utilities.XMLClaferParser;
import de.cognicrypt.core.Constants;
import de.cognicrypt.utils.FileHelper;

/**
 * This class is a storage for the configuration chosen by the user.
 *
 * @author Stefan Krueger
 */
public class Configuration {

	final private InstanceClafer instance;
	final private Map<Question, Answer> options;
	final private String pathOnDisk;

	public Configuration(final InstanceClafer instance, final Map<Question, Answer> constraints, final String pathOnDisk) {
		this.instance = instance;
		this.pathOnDisk = pathOnDisk;
		this.options = constraints;
	}

	/**
	 * Writes chosen configuration to hard disk.
	 *
	 * @return Written file.
	 * @throws IOException
	 *         see {@link FileWriter#FileWriter(String)) FileWriter} and {@link XMLWriter#write(String) XMLWriter.write()}
	 */
	public File persistConf() throws IOException {
		final XMLClaferParser parser = new XMLClaferParser();
		Document configInXMLFormat = parser.displayInstanceValues(this.instance, this.options);
		if (configInXMLFormat != null) {
			final OutputFormat format = OutputFormat.createPrettyPrint();
			final XMLWriter writer = new XMLWriter(new FileWriter(this.pathOnDisk), format);
			writer.write(configInXMLFormat);
			writer.close();
			configInXMLFormat = null;

			return new File(this.pathOnDisk);
		} else {
			Activator.getDefault().logError(Constants.NO_XML_INSTANCE_FILE_TO_WRITE);
		}
		return null;
	}

	/**
	 * Retrieves list of custom providers from configuration.
	 *
	 * @return List of custom providers
	 */
	public List<String> getProviders() {
		final List<String> providers = new ArrayList<String>();
		for (final InstanceClafer instanceChild : this.instance.getChildren()) {
			if (instanceChild.hasRef() && instanceChild.getRef() instanceof InstanceClafer) {
				for (final InstanceClafer innerChild : ((InstanceClafer) instanceChild.getRef()).getChildren()) {
					if (ClaferModelUtils.removeScopePrefix(innerChild.getType().getName()).equals("Provider")) {
						try {
							final String provider = ClaferModelUtils.removeScopePrefix(((InstanceClafer) innerChild.getRef()).getType().getName());
							if (!provider.equals(Constants.DEFAULT_PROVIDER)) {
								providers.add(provider);
							}
						} catch (final ClassCastException ex) {
							Activator.getDefault().logError(ex, "Not all custom providers set successfully.");
						}
					}
				}
			}
		}
		return providers;
	}

	/**
	 * Deletes config file from hard disk.
	 */
	public void deleteConfFromDisk() {
		FileHelper.deleteFile(this.pathOnDisk);
	}
}
