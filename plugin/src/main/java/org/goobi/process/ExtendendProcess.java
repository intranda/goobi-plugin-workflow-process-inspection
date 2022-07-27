package org.goobi.process;

import org.goobi.beans.DatabaseObject;
import org.goobi.beans.Process;
import org.goobi.production.cli.helper.StringPair;

import lombok.Getter;
import lombok.Setter;

public class ExtendendProcess implements DatabaseObject {

    @Getter
    @Setter
    private String selectedAction;



    @Getter
    private Process process;

    public ExtendendProcess(Process p, String defaultValue) {
        process = p;
        selectedAction = defaultValue;
    }

    @Override
    public void lazyLoad() {
        // nothing
    }


    public String getThumbnailUrl() {
        return ""; //TODO
    }

    public String getMetadataValue (String metadataName) {
        for (StringPair sp : process.getMetadataList() ) {
            if (sp.getOne().equals(metadataName)) {
                return sp.getTwo();
            }
        }

        return "";
    }

}
