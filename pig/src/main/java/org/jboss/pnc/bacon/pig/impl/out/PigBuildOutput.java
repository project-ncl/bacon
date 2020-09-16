package org.jboss.pnc.bacon.pig.impl.out;

import lombok.Data;

@Data
public class PigBuildOutput {
    private String message;
    private PigRunOutput pigRunOutput;

    public PigBuildOutput(String message, PigRunOutput pigRunOutput) {
        this.message = message;
        this.pigRunOutput = pigRunOutput;
    }
}
