package com.lifeos.integration.dify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifyDocumentResponse {
    private String documentId;
    private String indexingStatus;
    private boolean demo;
}
