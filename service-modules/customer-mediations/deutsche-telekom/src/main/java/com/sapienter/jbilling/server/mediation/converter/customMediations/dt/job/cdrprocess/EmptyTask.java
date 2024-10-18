package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class EmptyTask implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        return RepeatStatus.FINISHED;
    }
}
