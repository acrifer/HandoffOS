package com.lifeos.integration.dify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifyDatasetResponse {
    private String datasetId;
    private String name;
    private boolean demo;
}
