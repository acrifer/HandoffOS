package com.lifeos.note.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class HandoffSkillSourceSyncDTO {
    private List<String> documentRefs = new ArrayList<>();
    private String chatId;
    private Date startTime;
    private Date endTime;
    private Integer limit;
}
