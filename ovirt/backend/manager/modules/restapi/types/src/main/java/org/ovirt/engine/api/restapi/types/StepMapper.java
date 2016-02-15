package org.ovirt.engine.api.restapi.types;

import java.sql.Date;
import java.util.Calendar;

import org.ovirt.engine.api.model.ExternalSystemType;
import org.ovirt.engine.api.model.Job;
import org.ovirt.engine.api.model.Step;
import org.ovirt.engine.api.model.StepEnum;
import org.ovirt.engine.api.restapi.utils.GuidUtils;
import org.ovirt.engine.api.restapi.utils.TypeConversionHelper;

public class StepMapper {

    @Mapping(from = org.ovirt.engine.core.common.job.Step.class, to = Step.class)
    public static Step map(org.ovirt.engine.core.common.job.Step entity,
            Step step) {

        Step model = step != null ? step : new Step();
        model.setId(entity.getId().toString());
        if (entity.getParentStepId() != null) {
            Step parentStep = new Step();
            parentStep.setId(entity.getParentStepId().toString());
            model.setParentStep(parentStep);
        }
        Job job = new Job();
        job.setId(entity.getJobId().toString());
        model.setJob(job);
        StepEnum type = map(entity.getStepType());
        model.setType(type == null ? null : type.value());
        model.setDescription(entity.getDescription());
        model.setNumber(entity.getStepNumber());
        model.setStatus(JobMapper.map(entity.getStatus(), null));
        model.setStartTime(DateMapper.map(entity.getStartTime(), null));
        if (entity.getEndTime() != null) {
            model.setEndTime(TypeConversionHelper.toXMLGregorianCalendar(entity.getEndTime(), null));
        }
        model.setExternal(entity.isExternal());
        if (entity.getExternalSystem() != null && entity.getExternalSystem().getType() != null) {
            model.setExternalType(map(entity.getExternalSystem().getType()));
        }

        return model;
    }

    @Mapping(from = Step.class, to = org.ovirt.engine.core.common.job.Step.class)
    public static org.ovirt.engine.core.common.job.Step map(Step step,
            org.ovirt.engine.core.common.job.Step entity) {
        org.ovirt.engine.core.common.job.Step target =
                entity != null ? entity : new org.ovirt.engine.core.common.job.Step();
        target.setId(GuidUtils.asGuid(step.getId()));
        if (step.isSetParentStep()) {
            target.setParentStepId(GuidUtils.asGuid(step.getParentStep().getId()));
        }
        target.setJobId(GuidUtils.asGuid(step.getJob().getId()));
        if (step.isSetType()) {
            StepEnum type = StepEnum.fromValue(step.getType());
            if (type != null) {
                target.setStepType(map(type));
            }
        }
        if (step.isSetDescription()) {
            target.setDescription(step.getDescription());
        }
        if (step.isSetNumber()) {
            target.setStepNumber(step.getNumber());
        }
        if (step.isSetStatus()) {
            target.setStatus(JobMapper.map(step.getStatus(), null));
        }
        target.setStartTime(step.isSetStartTime() ? step.getStartTime().toGregorianCalendar().getTime()
                : new Date((Calendar.getInstance().getTimeInMillis())));
        target.setEndTime(step.isSetEndTime() ? step.getEndTime().toGregorianCalendar().getTime()
                : new Date((Calendar.getInstance().getTimeInMillis())));
        target.setExternal(step.isSetExternal() ? step.isExternal() : true);
        return target;
    }

    @Mapping(from = StepEnum.class,
            to = org.ovirt.engine.core.common.job.StepEnum.class)
    public static org.ovirt.engine.core.common.job.StepEnum map(StepEnum type) {
        if (StepEnum.VALIDATING.name().equals(type.name().toUpperCase())) {
            return org.ovirt.engine.core.common.job.StepEnum.VALIDATING;
        }
        if (StepEnum.EXECUTING.name().equals(type.name().toUpperCase())) {
            return org.ovirt.engine.core.common.job.StepEnum.EXECUTING;
        }
        if (StepEnum.FINALIZING.name().equals(type.name().toUpperCase())) {
            return org.ovirt.engine.core.common.job.StepEnum.FINALIZING;
        }
        if (StepEnum.REBALANCING_VOLUME.name().equals(type.name().toUpperCase())) {
            return org.ovirt.engine.core.common.job.StepEnum.REBALANCING_VOLUME;
        }
        if (StepEnum.REMOVING_BRICKS.name().equals(type.name().toUpperCase())) {
            return org.ovirt.engine.core.common.job.StepEnum.REMOVING_BRICKS;
        }
        return org.ovirt.engine.core.common.job.StepEnum.UNKNOWN;
    }

    @Mapping(from = org.ovirt.engine.core.common.job.StepEnum.class,
            to = StepEnum.class)
    public static StepEnum map(org.ovirt.engine.core.common.job.StepEnum type) {
        if (StepEnum.VALIDATING.name().equals(type.name())) {
            return StepEnum.VALIDATING;
        }
        if (StepEnum.EXECUTING.name().equals(type.name())) {
            return StepEnum.EXECUTING;
        }
        if (StepEnum.FINALIZING.name().equals(type.name())) {
            return StepEnum.FINALIZING;
        }
        if (StepEnum.REBALANCING_VOLUME.name().equals(type.name())) {
            return StepEnum.REBALANCING_VOLUME;
        }
        if (StepEnum.REMOVING_BRICKS.name().equals(type.name())) {
            return StepEnum.REMOVING_BRICKS;
        }
        return StepEnum.UNKNOWN;
    }

    @Mapping(from = org.ovirt.engine.core.common.job.ExternalSystemType.class,
            to = ExternalSystemType.class)
    public static String map(org.ovirt.engine.core.common.job.ExternalSystemType type) {
        switch (type) {
        case VDSM:
            return ExternalSystemType.VDSM.toString();
        case GLUSTER:
            return ExternalSystemType.GLUSTER.toString();
        default:
            return null;
        }
    }
}
