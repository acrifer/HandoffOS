package com.lifeos.note.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifeos.api.ai.dto.AiAsyncJobCommand;
import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.api.ai.dto.AiJobType;
import com.lifeos.api.ai.dto.HandoffSkillSourceDTO;
import com.lifeos.api.ai.mq.AiMqConstants;
import com.lifeos.note.domain.entity.AiWorkflowJob;
import com.lifeos.note.domain.entity.HandoffSkill;
import com.lifeos.note.domain.entity.Note;
import com.lifeos.note.mapper.HandoffSkillMapper;
import com.lifeos.note.mapper.NoteMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AiJobServiceImplTest {

    @Spy
    @InjectMocks
    private AiJobServiceImpl aiJobService;

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private HandoffSkillMapper handoffSkillMapper;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Test
    void submitNoteJobEnqueuesRocketMqCommand() {
        Note note = new Note();
        note.setId(203L);
        note.setUserId(9L);
        note.setTitle("Async note");
        note.setContent("Move summary generation to background jobs.");
        note.setTags("ai,workflow");
        when(noteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(note);
        doAnswer(invocation -> true).when(aiJobService).save(any(AiWorkflowJob.class));

        AiAsyncJobDTO job = aiJobService.submitNoteJob(9L, 203L, AiJobType.SUMMARY);

        assertThat(job.getId()).isNotNull();
        assertThat(job.getNoteId()).isEqualTo(203L);
        assertThat(job.getNoteTitle()).isEqualTo("Async note");
        assertThat(job.getJobType()).isEqualTo(AiJobType.SUMMARY);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).syncSend(eq(AiMqConstants.TOPIC), messageCaptor.capture(), anyLong());
        AiAsyncJobCommand command = JSON.parseObject(messageCaptor.getValue(), AiAsyncJobCommand.class);
        assertThat(command.getJobId()).isEqualTo(job.getId());
        assertThat(command.getUserId()).isEqualTo(9L);
        assertThat(command.getNoteId()).isEqualTo(203L);
        assertThat(command.getJobType()).isEqualTo(AiJobType.SUMMARY);
        assertThat(command.getTitle()).isEqualTo("Async note");
    }

    @Test
    void submitSkillDistillJobEnqueuesSkillCommand() {
        doAnswer(invocation -> true).when(aiJobService).save(any(AiWorkflowJob.class));
        HandoffSkillSourceDTO source = new HandoffSkillSourceDTO();
        source.setSourceType("FEISHU_DOC");
        source.setExternalId("doc-1");
        source.setTitle("交接文档");
        source.setContent("先确认发布风险，再推进上线。");
        HandoffSkill skill = new HandoffSkill();
        skill.setId(3001L);
        skill.setUserId(9L);
        skill.setName("支付后端交接助手");
        when(handoffSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(skill);

        AiAsyncJobDTO job = aiJobService.submitSkillDistillJob(9L, 3001L, "支付后端交接助手",
                "负责支付项目后端交接", List.of(source));

        assertThat(job.getSkillId()).isEqualTo(3001L);
        assertThat(job.getJobType()).isEqualTo(AiJobType.SKILL_DISTILL);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).syncSend(eq(AiMqConstants.TOPIC), messageCaptor.capture(), anyLong());
        AiAsyncJobCommand command = JSON.parseObject(messageCaptor.getValue(), AiAsyncJobCommand.class);
        assertThat(command.getSkillId()).isEqualTo(3001L);
        assertThat(command.getSkillName()).isEqualTo("支付后端交接助手");
        assertThat(command.getSkillSources()).hasSize(1);
    }
}
