// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.wizards.routinesjar;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.VersionUtils;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.CorePlugin;
import org.talend.core.PluginChecker;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.general.Project;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.model.properties.RoutinesJarItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.routines.CodesJarInfo;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.designer.maven.tools.CodesJarM2CacheManager;
import org.talend.repository.ProjectManager;
import org.talend.repository.RepositoryPlugin;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;

public class NewRoutinesJarWizard extends Wizard {

    private NewRoutinesJarWizardPage mainPage;

    private RoutinesJarItem routinesJarItem;

    private Property property;

    private IPath path;

    public NewRoutinesJarWizard(IPath path) {
        super();
        this.path = path;

        this.property = PropertiesFactory.eINSTANCE.createProperty();
        this.property.setAuthor(((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                .getUser());
        this.property.setVersion(VersionUtils.DEFAULT_VERSION);
        this.property.setStatusCode(""); //$NON-NLS-1$
        routinesJarItem = PropertiesFactory.eINSTANCE.createRoutinesJarItem();

        routinesJarItem.setProperty(property);
        routinesJarItem.setRoutinesJarType(PropertiesFactory.eINSTANCE.createRoutinesJarType());
    }

    @Override
    public void addPages() {
        mainPage = new NewRoutinesJarWizardPage(property, path);
        addPage(mainPage);
        setWindowTitle(Messages.getString("NewRoutinesJarWizard.title")); //$NON-NLS-1$
    }

    @Override
    public boolean performFinish() {

        IProxyRepositoryFactory repositoryFactory = ProxyRepositoryFactory.getInstance();
        try {
            property.setId(repositoryFactory.getNextId());
            property.setLabel(property.getDisplayName());
            repositoryFactory.create(routinesJarItem, mainPage.getDestinationPath());
            Project project = ProjectManager.getInstance().getCurrentProject();
            IFolder folder = ResourceUtils
                    .getFolder(ResourceUtils.getProject(project),
                            ERepositoryObjectType.getFolderName(ERepositoryObjectType.ROUTINESJAR), true)
                    .getFolder(property.getLabel());
            if (!folder.exists()) {
                ResourceUtils.createFolder(folder);
            }
            if (PluginChecker.isSWTBotLoaded()) {
                CodesJarM2CacheManager.updateCodesJarProject(CodesJarInfo.create(property), false);
            } else {
                Job job = new Job("Update custom jar project") { //$NON-NLS-1$

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            CodesJarM2CacheManager.updateCodesJarProject(CodesJarInfo.create(property), false);
                            return Status.OK_STATUS;
                        } catch (Exception e) {
                            return new Status(IStatus.ERROR, RepositoryPlugin.PLUGIN_ID, 1, e.getMessage(), e);
                        }
                    }

                };
                job.setUser(false);
                job.setPriority(Job.INTERACTIVE);
                job.schedule();
            }
        } catch (Exception e) {
            MessageDialog.openError(getShell(), Messages.getString("NewProcessWizard.failureTitle"), ""); //$NON-NLS-1$ //$NON-NLS-2$
            ExceptionHandler.process(e);
        }

        return routinesJarItem != null;
    }

    public RoutinesJarItem getRoutinesJar() {
        return routinesJarItem;
    }

}
