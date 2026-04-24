package com.lifeos.ai.messaging;

import com.alibaba.fastjson2.JSON;
import com.lifeos.ai.service.AiSummaryService;
import com.lifeos.api.ai.client.NoteFeignClient;
import com.lifeos.api.ai.dto.AiAsyncJobCommand;
import com.lifeos.api.ai.dto.AiAsyncJobUpdateDTO;
import com.lifeos.api.ai.dto.AiJobStatus;
import com.lifeos.api.ai.dto.AiJobType;
import com.lifeos.api.ai.dto.HandoffSkillResultDTO;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.common.response.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobConsumerTest {

    @InjectMocks
    private AiJobConsumer consumer;

    @Mock
    private AiSummaryService aiSummaryService;

    @Mock
    private NoteFeignClient noteFeignClient;

    @Test
    void onMessageUpdatesJobStatusFromProcessingToSuccess() {
        AiAsyncJobCommand command = new AiAsyncJobCommand();
        command.setJobId(1001L);
        command.setUserId(9L);
        command.setNoteId(203L);
        command.setJobType(AiJobType.SUMMARY);
        command.setTitle("Async note");
        command.setContent("Background summarization");

        AiSummaryResult summaryResult = new AiSummaryResult();
        summaryResult.setStatus(AiJobStatus.SUCCESS);
        summaryResult.setSummary("这是生成好的摘要。");
        when(aiSummaryService.generateSummary(any())).thenReturn(summaryResult);
        when(noteFeignClient.updateJobStatus(eq(1001L), any(AiAsyncJobUpdateDTO.class))).thenReturn(Result.success());

        consumer.onMessage(JSON.toJSONString(command));

        ArgumentCaptor<AiAsyncJobUpdateDTO> updateCaptor = ArgumentCaptor.forClass(AiAsyncJobUpdateDTO.class);
        verify(noteFeignClient, org.mockito.Mockito.times(2)).updateJobStatus(eq(1001L), updateCaptor.capture());
        assertThat(updateCaptor.getAllValues().get(0).getStatus()).isEqualTo(AiJobStatus.PROCESSING);
        AiAsyncJobUpdateDTO successUpdate = updateCaptor.getAllValues().get(1);
        assertThat(successUpdate.getStatus()).isEqualTo(AiJobStatus.SUCCESS);
        assertThat(successUpdate.getResult().getSummary()).isEqualTo("这是生成好的摘要。");
    }

    @Test
    void onMessageHandlesSkillDistillJob() {
        AiAsyncJobCommand command = new AiAsyncJobCommand();
        command.setJobId(2002L);
        command.setUserId(9L);
        command.setSkillId(3001L);
        command.setJobType(AiJobType.SKILL_DISTILL);
        command.setSkillName("支付后端交接助手");

        HandoffSkillResultDTO result = new HandoffSkillResultDTO();
        result.getRoleBoundaries().add("只回答支付项目交接问题。");
        when(aiSummaryService.distillHandoffSkill(any())).thenReturn(result);
        when(noteFeignClient.updateJobStatus(eq(2002L), any(AiAsyncJobUpdateDTO.class))).thenReturn(Result.success());

        consumer.onMessage(JSON.toJSONString(command));

        ArgumentCaptor<AiAsyncJobUpdateDTO> updateCaptor = ArgumentCaptor.forClass(AiAsyncJobUpdateDTO.class);
        verify(noteFeignClient, org.mockito.Mockito.times(2)).updateJobStatus(eq(2002L), updateCaptor.capture());
        AiAsyncJobUpdateDTO successUpdate = updateCaptor.getAllValues().get(1);
        assertThat(successUpdate.getStatus()).isEqualTo(AiJobStatus.SUCCESS);
        assertThat(successUpdate.getResult().getHandoffSkill().getRoleBoundaries())
                .contains("只回答支付项目交接问题。");
    }
}
