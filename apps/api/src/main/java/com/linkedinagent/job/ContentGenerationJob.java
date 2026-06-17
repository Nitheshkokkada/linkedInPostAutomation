package com.linkedinagent.job;

import com.linkedinagent.agent.ContentCreationAgent;
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
public class ContentGenerationJob implements Job {

    private final ContentCreationAgent contentCreationAgent;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final AgentLogService agentLogService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long startMs = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(null, "ContentGenerationJob", runId,
                "Daily content generation").getId();

        try {
            int usersProcessed = 0;
            int postsCreated = 0;

            for (User user : userRepository.findAll()) {
                if (topicRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(user.getId()).isEmpty()) {
                    continue;
                }
                int created = contentCreationAgent.runForUnprocessedResearch(user.getId(), runId).size();
                if (created > 0) {
                    usersProcessed++;
                    postsCreated += created;
                }
            }

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Users processed=" + usersProcessed + ", posts created=" + postsCreated,
                    null, System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            log.error("ContentGenerationJob failed", e);
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            throw new JobExecutionException(e);
        }
    }
}
