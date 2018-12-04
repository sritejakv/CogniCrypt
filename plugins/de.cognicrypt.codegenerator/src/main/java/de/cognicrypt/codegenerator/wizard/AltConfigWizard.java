package de.cognicrypt.codegenerator.wizard;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.clafer.instance.InstanceClafer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.cognicrypt.codegenerator.Activator;
import de.cognicrypt.codegenerator.DeveloperProject;
import de.cognicrypt.codegenerator.featuremodel.clafer.InstanceGenerator;
import de.cognicrypt.codegenerator.generator.CodeGenCrySLRule;
import de.cognicrypt.codegenerator.generator.CodeGenerator;
import de.cognicrypt.codegenerator.generator.CrySLBasedCodeGenerator;
import de.cognicrypt.codegenerator.generator.XSLBasedGenerator;
import de.cognicrypt.codegenerator.question.Answer;
import de.cognicrypt.codegenerator.question.Question;
import de.cognicrypt.codegenerator.tasks.Task;
import de.cognicrypt.codegenerator.utilities.CodeGenUtils;
import de.cognicrypt.codegenerator.wizard.beginner.BeginnerModeQuestionnaire;
import de.cognicrypt.codegenerator.wizard.beginner.BeginnerTaskQuestionPage;
import de.cognicrypt.core.Constants;
import de.cognicrypt.core.Constants.CodeGenerators;
import de.cognicrypt.utils.Utils;

public class AltConfigWizard extends Wizard {

	private TaskSelectionPage taskListPage;
	private HashMap<Question, Answer> constraints;
	private BeginnerModeQuestionnaire beginnerQuestions;
	private final CodeGenerators generator;

	public AltConfigWizard(CodeGenerators codeGen) {
		super();
		// Set the Look and Feel of the application to the operating
		// system's look and feel.
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			Activator.getDefault().logError(e);
		}
		setWindowTitle("Cryptography Task Configurator");
		final ImageDescriptor image = AbstractUIPlugin.imageDescriptorFromPlugin("de.cognicrypt.codegenerator", "icons/cognicrypt-medium.png");
		setDefaultPageImageDescriptor(image);
		this.constraints = new HashMap<>();
		generator = codeGen;
	}

	@Override
	public void addPages() {
		this.taskListPage = new TaskSelectionPage();
		setForcePreviousAndNextButtons(true);
		addPage(this.taskListPage);
	}

	@Override
	public boolean canFinish() {
		final IWizardPage page = getContainer().getCurrentPage();
		return page instanceof LocatorPage && page.isPageComplete();

	}

	private boolean checkifInUpdateRound() {
		boolean updateRound = false;
		final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (final StackTraceElement el : stack) {
			if (el.getMethodName().contains("updateButtons")) {
				updateRound = true;
				break;
			}
		}
		return updateRound;
	}

	/**
	 * This method returns the next page. If current page is task list or any but the last question page, the first/next question page is returned. If the current page is the last
	 * question page, the instance list page is returned.
	 *
	 * @param currentPage
	 *        current page
	 * @return either next question page or instance list page
	 */
	@Override
	public IWizardPage getNextPage(final IWizardPage currentPage) {

		if (checkifInUpdateRound()) {
			return currentPage;
		}
		final Task selectedTask = this.taskListPage.getSelectedTask();
		if (currentPage instanceof TaskSelectionPage) {
			this.beginnerQuestions = new BeginnerModeQuestionnaire(selectedTask, selectedTask.getQuestionsJSONFile());
			// It is possible that now questions are within a BeginnerModeQuestionnaire

			if (this.beginnerQuestions.hasPages()) {
				final BeginnerTaskQuestionPage questionPage = new BeginnerTaskQuestionPage(this.beginnerQuestions.nextPage(), this.beginnerQuestions.getTask(), null);
				addPage(questionPage);
				return questionPage;
			} else {
				return addLocatorPage();
			}
		}

		//Only case that is left: BeginnerTaskQuestionPage
		final BeginnerTaskQuestionPage curQuestionPage = (BeginnerTaskQuestionPage) currentPage;
		final HashMap<Question, Answer> curQuestionAnswerMap = curQuestionPage.getMap();

		for (final Entry<Question, Answer> entry : curQuestionAnswerMap.entrySet()) {
			this.constraints.put(entry.getKey(), entry.getValue());
		}

		final int nextPageid = curQuestionPage.getPageNextID();
		if (this.beginnerQuestions.hasMorePages() && nextPageid > -1) {
			final BeginnerTaskQuestionPage questionPage = new BeginnerTaskQuestionPage(this.beginnerQuestions.getPageByID(nextPageid), this.beginnerQuestions.getTask(), null);
			addPage(questionPage);
			return questionPage;
		} else {
			final InstanceGenerator instanceGenerator = new InstanceGenerator(CodeGenUtils.getResourceFromWithin(selectedTask.getModelFile())
				.getAbsolutePath(), "c0_" + selectedTask.getName(), selectedTask.getDescription());

			instanceGenerator.generateInstances(this.constraints);

			if (instanceGenerator.getNoOfInstances() > 0) {
				return addLocatorPage();
			} else {
				final String message = Constants.NO_POSSIBLE_COMBINATIONS_BEGINNER;
				MessageDialog.openError(new Shell(), "Error", message);
			}
		}
		return currentPage;
	}

	private IWizardPage addLocatorPage() {
		final LocatorPage locatorPage = new LocatorPage("Locator");
		addPage(locatorPage);
		return locatorPage;
	}

	/**
	 * This method returns previous page. If currentPage is the first question, the task list page is returned. If it is any other question page or the instance list page, the
	 * previous question page is returned.
	 *
	 * @param currentPage
	 *        current page, either instance list page or question page
	 * @return either previous question or task selection page
	 */
	@Override
	public IWizardPage getPreviousPage(final IWizardPage currentPage) {
		if (!checkifInUpdateRound()) {
			final IWizardPage[] pages = getPages();
			for (int i = 0; i < pages.length; i++) {
				if (currentPage.equals(pages[i])) {
					if (currentPage instanceof BeginnerTaskQuestionPage) {
						((BeginnerTaskQuestionPage) currentPage).setPageInactive();
					}
					final BeginnerTaskQuestionPage prevPage = (BeginnerTaskQuestionPage) pages[i - 1];
					for (final Entry<Question, Answer> quesAns : prevPage.getSelection().entrySet()) {
						this.constraints.remove(quesAns.getKey());
					}
					return prevPage;
				}
			}
		}

		return super.getPreviousPage(currentPage);
	}

	/**
	 * This method is called once the user selects an instance. It writes the instance to an xml file and calls the code generation.
	 *
	 * @return <code>true</code>/<code>false</code> if writing instance file and code generation are (un)successful
	 */
	@Override
	public boolean performFinish() {
		boolean ret = false;
		final Task selectedTask = this.taskListPage.getSelectedTask();
		CodeGenerator codeGenerator;
		String additionalResources = selectedTask.getAdditionalResources();

		switch (generator) {
			case CrySL:
				List<List<CodeGenCrySLRule>> rules = new ArrayList<List<CodeGenCrySLRule>>();
				try {
					List<List<String>> stringRules = new ArrayList<List<String>>();
					stringRules.add(Arrays.asList(new String[] { "SecureRandom", "PBEKeySpec", "SecretKeyFactory", "SecretKey", "SecretKeySpec" }));
					stringRules.add(Arrays.asList(new String[] { "Cipher" }));

					for (List<String> rule : stringRules) {
						ArrayList<CodeGenCrySLRule> newRules = new ArrayList<CodeGenCrySLRule>();
						rules.add(newRules);
						for (String r : rule) {
							try {
								newRules.add(new CodeGenCrySLRule(Utils.getCryptSLRule(r)));
							} catch (FileNotFoundException ex) {
								Activator.getDefault().logError(ex, "CrySL rule " + r + " not found.");
							}
						}
					}

					codeGenerator = new CrySLBasedCodeGenerator(this.taskListPage.getSelectedProject());
					
					Map<CodeGenCrySLRule, ?> constraints = new HashMap<CodeGenCrySLRule, Object>();
					Configuration chosenConfig = new CrySLConfiguration(rules, constraints, codeGenerator.getDeveloperProject()
						.getProjectPath() + Constants.innerFileSeparator + Constants.pathToClaferInstanceFile);

					ret = codeGenerator.generateCodeTemplates(chosenConfig, additionalResources);
				} catch (Exception e) {
					Activator.getDefault().logError(e);
					return false;
				}
				break;
			case XSL:
				
				this.constraints = (this.constraints != null) ? this.constraints : new HashMap<>();
				final InstanceGenerator instanceGenerator = new InstanceGenerator(CodeGenUtils.getResourceFromWithin(selectedTask.getModelFile())
					.getAbsolutePath(), "c0_" + selectedTask.getName(), selectedTask.getDescription());

				instanceGenerator.generateInstances(this.constraints);
				final Map<String, InstanceClafer> instances = instanceGenerator.getInstances();
				final InstanceClafer instance = instances.values().iterator().next();
				final LocatorPage currentPage = (LocatorPage) getContainer().getCurrentPage();

				// Initialize Code Generation
				codeGenerator = new XSLBasedGenerator(Utils.getIProjectFromISelection(currentPage.getSelectedResource()), selectedTask.getXslFile());
				final DeveloperProject developerProject = codeGenerator.getDeveloperProject();

				// Generate code template
				XSLConfiguration chosenConfig = new XSLConfiguration(instance, this.constraints, developerProject.getProjectPath() + Constants.innerFileSeparator + Constants.pathToClaferInstanceFile);
				ret &= codeGenerator.generateCodeTemplates(chosenConfig, selectedTask.getAdditionalResources());
				break;
			default:
				ret = false;
				break;
		}
		return ret;
	}

	public HashMap<Question, Answer> getConstraints() {
		return this.constraints;
	}

}