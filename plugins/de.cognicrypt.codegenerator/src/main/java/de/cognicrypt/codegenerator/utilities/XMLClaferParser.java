/********************************************************************************
 * Copyright (c) 2015-2018 TU Darmstadt
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package de.cognicrypt.codegenerator.utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.clafer.ast.AstClafer;
import org.clafer.instance.InstanceClafer;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import de.cognicrypt.codegenerator.Activator;
import de.cognicrypt.codegenerator.featuremodel.clafer.ClaferModelUtils;
import de.cognicrypt.codegenerator.featuremodel.clafer.PropertiesMapperUtil;
import de.cognicrypt.codegenerator.question.Answer;
import de.cognicrypt.codegenerator.question.CodeDependency;
import de.cognicrypt.codegenerator.question.Question;
import de.cognicrypt.core.Constants;

/**
 * This class handles XML files.
 *
 * @author Mohammad Zahraee
 * @author Ram Kamath
 * @author Stefan Krueger
 *
 */
public class XMLClaferParser {

	private Document document = null;
	private String enumParent = null;

	/**
	 * Builds xml document, returns its string representation.
	 *
	 * @param inst
	 *        Clafer instance/algorithm configuration selected to be generated
	 * @param constraints
	 *        constraints of task that need to be encoded in the xml file
	 * @return content of xml instance file as String object
	 */
	public Document displayInstanceValues(final InstanceClafer inst, final Map<Question, Answer> constraints) {
		this.document = DocumentHelper.createDocument();

		// create task tag on the root level of the document
		final Element taskElem = this.document.addElement(Constants.Task);

		if (inst != null && inst.hasChildren()) {
			final String taskName = inst.getType().getName();
			taskElem.addAttribute(Constants.Description, ClaferModelUtils.removeScopePrefix(taskName));

			// add imports
			taskElem.addElement(Constants.Package).addText(Constants.PackageName.replace(Constants.innerFileSeparator, "."));	// Constants.xmlPackage
			final Element xmlimports = taskElem.addElement(Constants.Imports);
			for (final String file : Constants.xmlimportsarr) {
				xmlimports.addElement(Constants.Import).addText(file);
			}

			boolean isSubclaferOfAlgorithm = false;
			for (final InstanceClafer in : inst.getChildren()) {
				final AstClafer targetType = in.getType().getRef().getTargetType();
				// climb up the inheritances to find if the current clafer is a subclafer of _Algorithm_
				for (AstClafer superClafer = targetType.getSuperClafer(); superClafer != null && superClafer.hasSuperClafer(); superClafer = superClafer.getSuperClafer()) {
					if (superClafer.toString().contains(Constants.CLAFER_ALGORITHM)) {
						isSubclaferOfAlgorithm = true;
						break;
					}
				}

				if (isSubclaferOfAlgorithm) {
					if (!targetType.isPrimitive()) {
						final Element algoElem = taskElem.addElement(Constants.ALGORITHM).addAttribute(Constants.Type, ClaferModelUtils.removeScopePrefix(targetType.getName()));
						displayInstanceXML(in, algoElem);
					} else {
						displayInstanceXML(in, taskElem);
					}
				}
			}

			// add the element tag containing the direct subclafers of the Task instance
			final Element algoElem = taskElem.addElement("element").addAttribute(Constants.Type, taskElem.attributes().get(0).getValue());

			for (final InstanceClafer in : inst.getChildren()) {
				if (!in.getType().getName().contains("description")) {
					final Object ref = in.getRef();
					String text = "";
					if (ref instanceof InstanceClafer) {
						text = ClaferModelUtils.removeScopePrefix(((InstanceClafer) in.getRef()).getType().getName());
					} else if (ref instanceof Integer) {
						text = ref.toString();
					} else if (ref instanceof String) {
						text = (String) ref;
					} else {
						text = "This should not happen.";
					}
					algoElem.addElement(ClaferModelUtils.removeScopePrefix(in.getType().getName())).setText(text);
				}

			}
		}

		// add code dependencies
		final Element codeElem = taskElem.addElement(Constants.Code);
		for (final Entry<Question, Answer> ent : constraints.entrySet()) {
			final ArrayList<CodeDependency> cdp = ent.getValue().getCodeDependencies();
			if (cdp != null) {
				for (final CodeDependency dep : cdp) {
					codeElem.addElement(dep.getOption()).addText(dep.getValue() + "");
				}
			}
		}
		return this.document;
	}

	private void displayInstanceXML(final InstanceClafer inst, final Element parent) {
		try {
			if (inst.hasChildren()) {
				for (final InstanceClafer in : inst.getChildren()) {
					if (isAlgorithm(in.getType())) {
						final Element algoElem = parent.addElement(Constants.ALGORITHM);
						algoElem.addAttribute(Constants.Type, ClaferModelUtils.removeScopePrefix(in.getType().getRef().getTargetType().getName()));
						displayInstanceXML(in, algoElem);
					} else {
						displayInstanceXML(in, parent);
					}
				}
				return;
			}

			final String refClass;
			if (inst.hasRef() && !inst.getType().isPrimitive() && (refClass = inst.getRef().getClass().toString()) != null && !refClass.contains(Constants.INTEGER) && !refClass
				.contains(Constants.STRING) && !refClass.contains(Constants.BOOLEAN)) {
				this.enumParent = ClaferModelUtils.removeScopePrefix(inst.getType().getName());
				displayInstanceXML((InstanceClafer) inst.getRef(), parent);
			} else if (PropertiesMapperUtil.getenumMap().keySet().contains(inst.getType().getSuperClafer())) {
				ClaferModelUtils.removeScopePrefix(inst.getType().getSuperClafer().getName());
				parent.addElement(this.enumParent).addText(ClaferModelUtils.removeScopePrefix(inst.getType().toString()).replace("\"", ""));
			} else {
				final String instName = ClaferModelUtils.removeScopePrefix(inst.getType().getName());
				if (inst.hasRef()) {
					parent.addElement(instName).addText(inst.getRef().toString().replace("\"", ""));
				} else {
					final String instparentName = ClaferModelUtils.removeScopePrefix(inst.getType().getParent().getName());
					parent.addElement(instparentName).addText(instName);
				}
			}
		} catch (final Exception e) {
			Activator.getDefault().logError(e);
		}
	}

	private boolean isAlgorithm(final AstClafer astClafer) {
		if (astClafer.hasRef()) {
			if (astClafer.getRef().getTargetType() != null && astClafer.getRef().getTargetType().getSuperClafer() != null) {
				return astClafer.getRef().getTargetType().getSuperClafer().getName().contains(Constants.CLAFER_ALGORITHM);
			}
		}
		return false;
	}

	/**
	 * Writes XML document to file. Before calling this method {@link de.cognicrypt.codegenerator.utilities.XMLClaferParser#displayInstanceValues(InstanceClafer, HashMap)
	 * displayInstanceValues()} should have been called to create document.
	 *
	 * @param path
	 *        path the XML file is written to
	 * @throws IOException
	 *         See {@link org.dom4j.io.XMLWriter#write(Document) write()} and {@link org.dom4j.io.XMLWriter#close() close()}
	 */
	public void writeXMLToFile(final String path) throws IOException {
		if (this.document != null) {
			final OutputFormat format = OutputFormat.createPrettyPrint();
			final XMLWriter writer = new XMLWriter(new FileWriter(path), format);
			writer.write(this.document);
			writer.close();
			this.document = null;
		} else {
			Activator.getDefault().logError(Constants.NO_XML_INSTANCE_FILE_TO_WRITE);
		}
	}

}
