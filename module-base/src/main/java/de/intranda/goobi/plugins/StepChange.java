package de.intranda.goobi.plugins;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StepChange {

    // name of the step to manipulate
    private String stepName;
    // open, close, disable, nothing
    private String changeType;
    // display this text
    private String label;

}
