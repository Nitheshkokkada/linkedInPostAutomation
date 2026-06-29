package com.linkedinagent.job;

import com.linkedinagent.agent.SelfLearningAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class SelfLearningJob implements Job {

    private final SelfLearningAgent selfLearningAgent;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            selfLearningAgent.runForAllUsers();
        } catch (Exception e) {
            log.error("SelfLearningJob failed", e);
            throw new JobExecutionException(e);
        }
    }
}
