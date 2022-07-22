package de.intranda.goobi.plugins;

import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IWorkflowPlugin;

import de.sub.goobi.config.ConfigPlugins;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class ProcessInspectionWorkflowPlugin implements IWorkflowPlugin, IPlugin {
    
    @Getter
    private String title = "intranda_workflow_processInspection";
        
    @Getter
    private String value;

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
        log.info("ProcessInspection workflow plugin started");
        value = ConfigPlugins.getPluginConfig(title).getString("value", "default value");
    }
}
