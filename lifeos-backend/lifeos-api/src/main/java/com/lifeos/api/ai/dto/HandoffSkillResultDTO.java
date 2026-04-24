package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class HandoffSkillResultDTO implements Serializable {
    private List<String> roleBoundaries = new ArrayList<>();
    private List<String> workPrinciples = new ArrayList<>();
    private List<String> decisionRules = new ArrayList<>();
    private List<String> workflowChecklists = new ArrayList<>();
    private List<String> communicationStyle = new ArrayList<>();
    private List<String> riskWarnings = new ArrayList<>();
    private List<String> handoffQuestions = new ArrayList<>();
    private List<String> citations = new ArrayList<>();
}
