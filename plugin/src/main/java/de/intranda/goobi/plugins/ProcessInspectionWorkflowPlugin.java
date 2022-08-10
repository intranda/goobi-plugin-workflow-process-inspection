package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.DatabaseObject;
import org.goobi.beans.Step;
import org.goobi.process.ExtendedProcessManager;
import org.goobi.process.ExtendedProcessPaginator;
import org.goobi.process.ExtendendProcess;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IWorkflowPlugin;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.CloseStepHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.StepManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class ProcessInspectionWorkflowPlugin implements IWorkflowPlugin, IPlugin {

    @Getter
    private String title = "intranda_workflow_processinspection";

    // name of the current open step
    private String processStepName;

    @Getter
    private List<StepChange> options;

    // metadata to display
    @Getter
    private List<String> metadataList;

    @Getter
    @Setter
    private String sortField;

    private String defaultValue;

    private ExtendedProcessPaginator processPaginator = null;

    @Override
    public PluginType getType() {
        return PluginType.Workflow;
    }

    @Override
    public String getGui() {
        return "/uii/plugin_workflow_processInspection.xhtml";
    }

    /**
     * Constructor
     */
    public ProcessInspectionWorkflowPlugin() {
        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
        config.setExpressionEngine(new XPathExpressionEngine());
        processStepName = config.getString("/stepName", "");
        metadataList = Arrays.asList(config.getStringArray("/metadata"));

        options = new ArrayList<>();

        List<HierarchicalConfiguration> subList = config.configurationsAt("/step");
        for (HierarchicalConfiguration hc : subList) {
            StepChange sc = new StepChange(hc.getString("@stepName"), hc.getString("@changeType"), hc.getString("@label"));
            options.add(sc);
            if (hc.getBoolean("@default", false)) {
                defaultValue = sc.getLabel();
            }
        }

    }

    private void loadProcesses() {
        ExtendedProcessManager m = new ExtendedProcessManager(defaultValue);
        processPaginator = new ExtendedProcessPaginator("prozesse.titel", processStepName, m);
    }

    public ExtendedProcessPaginator getProcessPaginator() {
        if (processPaginator == null) {
            loadProcesses();
        }
        return processPaginator;
    }

    public void executeChanges() {
        for (DatabaseObject dbObj : processPaginator.getList()) { //NOSONAR
            ExtendendProcess ep = (ExtendendProcess) dbObj;
            String selectedAction = ep.getSelectedAction();
            StepChange action = null;
            for (StepChange sc : options) {
                if (sc.getLabel().equals(selectedAction)) {
                    action = sc;
                }
            }
            if (action == null) {
                continue;
            }

            Step changeStep = null;
            Step currentStep = null;
            // check if step is defined
            if (StringUtils.isNotBlank(action.getStepName())) {
                // find step to change
                for (Step other : ep.getProcess().getSchritte()) {
                    if (action.getStepName().equals(other.getTitel())) {
                        changeStep = other;
                    } else if (processStepName.equals(other.getTitel())) {
                        currentStep = other;
                    }
                }
                if (changeStep == null) {
                    // task to change not found, clarify how to proceed
                    continue;
                } else if ("open".equals(action.getChangeType())) {
                    changeStep.setBearbeitungsstatusEnum(StepStatus.OPEN);
                } else if ("disable".equals(action.getChangeType())) {
                    changeStep.setBearbeitungsstatusEnum(StepStatus.DEACTIVATED);
                } else if ("close".equals(action.getChangeType())) {
                    changeStep.setBearbeitungsstatusEnum(StepStatus.DONE);
                }
                try {
                    StepManager.saveStep(changeStep);
                } catch (DAOException e) {
                    log.error(e);
                }

                CloseStepHelper.closeStep(currentStep, Helper.getCurrentUser());
            }

        }

        // finally reload process table
        loadProcesses();
    }
}
