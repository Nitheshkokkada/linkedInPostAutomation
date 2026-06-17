package com.linkedinagent.job;

import com.linkedinagent.agent.TopicResearchAgent;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.repository.TopicRepository;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.service.AgentLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class TopicResearchJob implements Job {

    private final TopicResearchAgent topicResearchAgent;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final AgentLogService agentLogService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long startMs = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(null, "TopicResearchJob", runId,
                "Daily topic research").getId();

        try {
            int usersProcessed = 0;

            for (User user : userRepository.findAll()) {
                if (topicRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(user.getId()).isEmpty()) {
                    continue;
                }
                topicResearchAgent.run(user.getId(), runId);
                usersProcessed++;
            }

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Users processed=" + usersProcessed, null, System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            log.error("TopicResearchJob failed", e);
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            throw new JobExecutionException(e);
        }
    }
}
