package com.lifeos.skill.dto;

import lombok.Data;

@Data
public class AskSkillRequest {
    private String question;
    private String conversationId;
}
