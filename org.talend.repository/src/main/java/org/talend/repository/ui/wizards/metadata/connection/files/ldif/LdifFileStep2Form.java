// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.repository.ui.wizards.metadata.connection.files.ldif;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.talend.commons.ui.swt.dialogs.ErrorDialogWidthDetailArea;
import org.talend.commons.ui.swt.formtools.Form;
import org.talend.commons.ui.swt.formtools.LabelledCheckboxCombo;
import org.talend.commons.ui.swt.formtools.UtilsButton;
import org.talend.core.model.metadata.builder.connection.FileConnection;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.targetschema.editor.TargetSchemaEditor2;
import org.talend.core.ui.targetschema.editor.TargetSchemaTableEditorView2;
import org.talend.core.ui.targetschema.editor.TargetSchemaToolbarEditorView2;
import org.talend.core.utils.XmlArray;
import org.talend.repository.i18n.Messages;
import org.talend.repository.preview.ProcessDescription;
import org.talend.repository.ui.swt.preview.ShadowProcessPreview;
import org.talend.repository.ui.swt.utils.AbstractLdifFileStepForm;
import org.talend.repository.ui.utils.ShadowProcessHelper;

/**
 * @author cantoine
 * 
 */
public class LdifFileStep2Form extends AbstractLdifFileStepForm {

    private static Logger log = Logger.getLogger(LdifFileStep2Form.class);

    private static final String EMPTY_VALUE = Messages.getString("FileStep2.empty");

    private static final String[] STRING_NUMBERS_DATA = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13",
            "14", "15", "16", "17", "18", "19", "20" };

    /**
     * Fields use to preview.
     */
    private LabelledCheckboxCombo rowsToSkipLimitCheckboxCombo;

    private Group previewGroup;

    private Button previewButton;

    private Label previewInformationLabel;

    private ShadowProcessPreview ldifFilePreview;

    /**
     * Another.
     */

    private UtilsButton cancelButton;

    private boolean readOnly;

    /**
     * Constructor to use by RCP Wizard.
     * 
     * @param Composite
     * @param Wizard
     * @param Style
     */
    public LdifFileStep2Form(Composite parent, ConnectionItem connectionItem) {
        super(parent, connectionItem);
        setupForm();
    }

    /**
     * 
     * Initialize value, forceFocus first field.
     */
    protected void initialize() {

        int i = getConnection().getLimitEntry();
        if (i > 0) {
            rowsToSkipLimitCheckboxCombo.setText("" + getConnection().getLimitEntry());
        }
        checkFieldsValue();
    }

    /**
     * DOC ocarbone Comment method "adaptFormToReadOnly".
     */
    protected void adaptFormToReadOnly() {
        readOnly = isReadOnly();
        rowsToSkipLimitCheckboxCombo.setReadOnly(isReadOnly());
    }

    /**
     * add field to Group Limit.
     * 
     * @param mainComposite
     * @param form
     * @param width
     * @param height
     */
//    private void addGroupAttributes(final Composite mainComposite, final int width, final int height) {
//        // Group Schema Viewer
//        Group group = Form.createGroup(mainComposite, 1, Messages.getString("XmlFileStep1.groupSchemaTarget"), height);
//
//        ScrolledComposite scrolledCompositeFileViewer = new ScrolledComposite(group, SWT.H_SCROLL | SWT.V_SCROLL
//                | SWT.NONE);
//        scrolledCompositeFileViewer.setExpandHorizontal(true);
//        scrolledCompositeFileViewer.setExpandVertical(true);
//        GridData gridData1 = new GridData(GridData.FILL_BOTH);
//        // gridData1.widthHint = width;
//        // gridData1.heightHint = height;
//        scrolledCompositeFileViewer.setLayoutData(gridData1);
//        scrolledCompositeFileViewer.setLayout(new FillLayout());
//
//        TargetSchemaEditor2 targetSchemaEditor = new TargetSchemaEditor2(Messages.getString("FileStep3.metadataDescription"));
//        TargetSchemaTableEditorView2 tableEditorView = new TargetSchemaTableEditorView2(scrolledCompositeFileViewer, SWT.NONE, targetSchemaEditor);
//        scrolledCompositeFileViewer.setContent(tableEditorView.getTableViewerCreator().getTable());
//        scrolledCompositeFileViewer.setSize(width, height);
//
//        // Composite toolbar = new Composite(group, SWT.BORDER);
//        TargetSchemaToolbarEditorView2 targetSchemaToolbarEditorView2 = new TargetSchemaToolbarEditorView2(group,
//                SWT.NONE, tableEditorView);
//    
//    }
    
    /**
     * add field to Group Limit.
     * 
     * @param mainComposite
     * @param form
     * @param width
     * @param height
     */
    private void addGroupLimit(final Composite mainComposite, final int width, final int height) {
        // Composite Limited rows
        Group group = Form.createGroup(mainComposite, 1, Messages.getString("FileStep2.groupLimitOfRows"), height);
        Composite compositeLimit = Form.startNewDimensionnedGridLayout(group, 3, width, height);

        // Information Limit
        Label info = new Label(compositeLimit, SWT.NONE);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 3;
        info.setLayoutData(gridData);
        info.setText(Messages.getString("FileStep2.groupLimitOfRowsTip"));

        // Limit
        rowsToSkipLimitCheckboxCombo = new LabelledCheckboxCombo(compositeLimit, Messages.getString("FileStep2.limit"), Messages
                .getString("FileStep2.limitTip"), STRING_NUMBERS_DATA, 1, true, SWT.NONE);
    }

    /**
     * add Field to Group File Viewer.
     * 
     * @param parent
     * @param form
     * @param width
     * @param height
     */
    private void addGroupFileViewer(final Composite parent, final int width, int height) {
        // composite Delimited File Preview
        previewGroup = Form.createGroup(parent, 1, Messages.getString("FileStep2.groupPreview"), height);
        Composite compositeDelimitedFilePreviewButton = Form.startNewDimensionnedGridLayout(previewGroup, 4, width,
                HEIGHT_BUTTON_PIXEL);
        height = height - HEIGHT_BUTTON_PIXEL - 15;

        // Delimited File Preview Info
        previewButton = new Button(compositeDelimitedFilePreviewButton, SWT.NONE);
        previewButton.setText(Messages.getString("FileStep2.refreshPreview"));
        previewButton.setSize(WIDTH_BUTTON_PIXEL, HEIGHT_BUTTON_PIXEL);

        // simple space
        new Label(compositeDelimitedFilePreviewButton, SWT.NONE);
        // Information Label
        previewInformationLabel = new Label(compositeDelimitedFilePreviewButton, SWT.NONE);
        previewInformationLabel
                .setText("                                                                                                                        ");
        previewInformationLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLUE));

        Composite compositeDelimitedFilePreview = Form.startNewDimensionnedGridLayout(previewGroup, 1, width, height);

        // Delimited File Preview
        ldifFilePreview = new ShadowProcessPreview(compositeDelimitedFilePreview, null, width, height - 10);
        ldifFilePreview.newTablePreview();
    }

    protected void addFields() {

        // compositeFileDelimitor Main Fields
        Composite mainComposite = Form.startNewGridLayout(this, 2);
//        addGroupAttributes(mainComposite, 300, 85);
        addGroupLimit(mainComposite, 300, 85);
        addGroupFileViewer(this, 700, 210);

        if (!isInWizard()) {
            // Bottom Button
            Composite compositeBottomButton = Form.startNewGridLayout(this, 2, false, SWT.CENTER, SWT.CENTER);
            // Button Cancel
            cancelButton = new UtilsButton(compositeBottomButton, Messages.getString("CommonWizard.cancel"), WIDTH_BUTTON_PIXEL,
                    HEIGHT_BUTTON_PIXEL);
        }
        addUtilsButtonListeners();
    }

    /**
     * create ProcessDescription and set it.
     * 
     * WARNING ::field FieldSeparator, RowSeparator, EscapeChar and TextEnclosure are surround by double quote.
     * 
     * @param getConnection()
     * 
     * @return processDescription
     */
    private ProcessDescription getProcessDescription() {

        //PTODO cantoine : view why it's not the same declaration that others Wizards.
        ProcessDescription processDescription = ShadowProcessHelper.getProcessDescription(getConnection());

        // Adapt Header width firstRowIsCaption to preview the first line on caption or not
        Integer i = 0;
        processDescription.setHeaderRow(i);

        // adapt the limit to the preview
        processDescription.setLimitRows(MAXIMUM_ROWS_TO_PREVIEW);
        if (rowsToSkipLimitCheckboxCombo.isInteger()) {
            i = new Integer(rowsToSkipLimitCheckboxCombo.getText());
            if (i < MAXIMUM_ROWS_TO_PREVIEW) {
                processDescription.setLimitRows(i);
            }
        }

        return processDescription;
    }

    /**
     * clear the table preview
     */
    void clearPreview() {
        ldifFilePreview.clearTablePreview();
    }

    /**
     * refreshPreview use ShadowProcess to refresh the preview.
     */
    void refreshPreview() {
        clearPreview();

        // if no file, the process don't be executed
        if (getConnection().getFilePath() == null || getConnection().getFilePath().equals("")) {
            previewInformationLabel.setText("   " + Messages.getString("FileStep2.filePathIncomplete"));
            return;
        }

        // if incomplete settings, , the process don't be executed
        if (!checkFieldsValue()) {
            previewInformationLabel.setText("   " + Messages.getString("FileStep2.settingsIncomplete"));
            return;
        }

        previewInformationLabel.setText("   " + Messages.getString("FileStep2.previewProgress"));

        // get the XmlArray width an adapt ProcessDescription
        try {

            ProcessDescription processDescription = getProcessDescription();

            XmlArray xmlArray = ShadowProcessHelper.getXmlArray(processDescription, "FILE_LDIF");
            if (xmlArray == null) {
                previewInformationLabel.setText("   " + Messages.getString("FileStep2.previewFailure"));
            } else {
                previewInformationLabel.setText("   " + Messages.getString("FileStep2.previewIsDone"));

                // refresh TablePreview on this step
                ldifFilePreview.refreshTablePreview(xmlArray, false);
                previewInformationLabel.setText("");
            }

        
        } catch (CoreException e) {
            previewInformationLabel.setText("   " + Messages.getString("FileStep2.previewFailure"));
            new ErrorDialogWidthDetailArea(getShell(), PID, Messages.getString("FileStep2.previewFailure"), e.getMessage());
            log.error(Messages.getString("FileStep2.previewFailure") + " " + e.getMessage());
        }
    }

    /**
     * Main Fields addControls.
     */
    protected void addFieldsListeners() {
        addFieldsListenersGroupsRowToSkipAndLimit();
    }

    /**
     * DOC ocarbone Comment method "setCharFlag".
     * 
     * @param escapeCharFlag2
     * @param text
     */
    protected void setCharFlag(Label label, String string) {
        // PTODO OCA : when is utils in the Flag ?
        label.setText(string);
    }

    /**
     * add Controls to group Row To Skip and Limit.
     */
    private void addFieldsListenersGroupsRowToSkipAndLimit() {
        // Event modify
        ArrayList<LabelledCheckboxCombo> labelledCheckboxCombo2Control = new ArrayList<LabelledCheckboxCombo>();
        labelledCheckboxCombo2Control.add(rowsToSkipLimitCheckboxCombo);

        Iterator<LabelledCheckboxCombo> iCheckboxCombo;
        LabelledCheckboxCombo labelledCheckboxCombo;

        // Event : keyPressed
        for (iCheckboxCombo = labelledCheckboxCombo2Control.iterator(); iCheckboxCombo.hasNext();) {
            labelledCheckboxCombo = iCheckboxCombo.next();

            // Event Key (numeric value only)
            labelledCheckboxCombo.addKeyListener(new KeyAdapter() {

                public void keyPressed(KeyEvent e) {
                    if (Character.getNumericValue(e.character) >= 10) {
                        e.doit = false;
                    }
                }
            });
        }

        rowsToSkipLimitCheckboxCombo.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                if (!rowsToSkipLimitCheckboxCombo.isEmpty()) {
                    if (!rowsToSkipLimitCheckboxCombo.isInteger() || rowsToSkipLimitCheckboxCombo.getText().equals("0")) {
                        rowsToSkipLimitCheckboxCombo.deselectAll();
                        getConnection().setUseLimit(rowsToSkipLimitCheckboxCombo.isChecked());
                        getConnection().setLimitEntry(0);
                    } else {
                        getConnection().setLimitEntry(new Integer(rowsToSkipLimitCheckboxCombo.getText()));
                    }
                } else {
                    getConnection().setUseLimit(rowsToSkipLimitCheckboxCombo.isChecked());
                    getConnection().setLimitEntry(0);
                }
                checkFieldsValue();
            }
        });
    }

    /**
     * Ensures that fields are set. Update checkEnable / use to checkConnection().
     * 
     * @return
     */
    protected boolean checkFieldsValue() {
        previewInformationLabel.setText("   " + Messages.getString("FileStep2.settingsIncomplete"));
        updateStatus(IStatus.OK, null);
        previewButton.setEnabled(false);

        // Labelled Checkbox Combo (Row to Skip and Limit)
        ArrayList<LabelledCheckboxCombo> labelledCheckboxCombo2Control = new ArrayList<LabelledCheckboxCombo>();
        labelledCheckboxCombo2Control.add(rowsToSkipLimitCheckboxCombo);

        Iterator<LabelledCheckboxCombo> iCheckboxCombo;
        LabelledCheckboxCombo labelledCheckboxCombo;

        for (iCheckboxCombo = labelledCheckboxCombo2Control.iterator(); iCheckboxCombo.hasNext();) {
            labelledCheckboxCombo = iCheckboxCombo.next();
            // if the checkbox is checked, check Numeric value
            if (labelledCheckboxCombo.getCheckbox().getSelection()) {
                if (labelledCheckboxCombo.getText() == "") {
                    updateStatus(IStatus.ERROR, labelledCheckboxCombo.getLabelText()
                            + Messages.getString("FileStep2.mustBePrecised"));
                    return false;
                }
            }
        }
        previewInformationLabel.setText("");
        previewButton.setEnabled(true);
        updateStatus(IStatus.OK, null);
        return true;
    }

    /**
     * addButtonControls.
     * 
     * @param cancelButton
     */
    protected void addUtilsButtonListeners() {

        // Event PreviewButton
        previewButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (!previewButton.getText().equals(Messages.getString("FileStep2.wait"))) {
                    previewButton.setText(Messages.getString("FileStep2.wait"));
                    refreshPreview();
                } else {
                    previewButton.setText(Messages.getString("FileStep2.refreshPreview"));
                }
            }
        });

        if (cancelButton != null) {
            // Event CancelButton
            cancelButton.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(final SelectionEvent e) {
                    getShell().close();
                }
            });
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.widgets.Control#setVisible(boolean)
     * 
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (super.isVisible()) {
            // Refresh the preview width the adapted rowSeparator
            // If metadata exist, refreshMetadata
            if ((!"".equals(getConnection().getFilePath())) && (getConnection().getFilePath() != null)) {
//                refreshPreview();
            }
            if (isReadOnly() != readOnly) {
                adaptFormToReadOnly();
            }
        }
    }
}
