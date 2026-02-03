package org.thingsboard.server.common.data.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentAppConfig {

    private boolean multiSelect;
    private List<String> values;
}
