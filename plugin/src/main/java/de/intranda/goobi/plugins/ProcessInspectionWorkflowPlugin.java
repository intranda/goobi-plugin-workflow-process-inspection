package de.intranda.goobi.plugins;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.process.ExtendedProcessManager;
import org.goobi.process.ExtendedProcessPaginator;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IWorkflowPlugin;

import de.sub.goobi.config.ConfigPlugins;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class ProcessInspectionWorkflowPlugin implements IWorkflowPlugin, IPlugin {

    @Getter
    private String title = "intranda_workflow_processInspection";

    // name of the current open step
    private String processStepName;

    // name of the inspection step to open or skip
    private String inspectionStepName;

    // metadata to display
    @Getter
    private List<String> metadataList;

    @Getter
    @Setter
    private String sortField;


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
        inspectionStepName = config.getString("/inspectionStepName", "");
        metadataList = Arrays.asList(config.getStringArray("/metadata"));
    }

    /*
    - display thumbnail image, if possible (pdf only?)
    - generate fulltext
    - option to send process to a special inspection task for individual processes
    - option to skip special inspection task for individual processes
     */

    private void loadProcesses() {
        ExtendedProcessManager m = new ExtendedProcessManager();
        processPaginator = new ExtendedProcessPaginator("prozesse.titel", processStepName, m);
    }

    public ExtendedProcessPaginator getProcessPaginator() {
        if (processPaginator == null) {
            loadProcesses();
        }
        return processPaginator;
    }
}
